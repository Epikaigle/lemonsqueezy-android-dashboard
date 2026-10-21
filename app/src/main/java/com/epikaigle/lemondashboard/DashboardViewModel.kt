@file:Suppress("DEPRECATION")
package com.epikaigle.lemondashboard

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

sealed interface UpdateStatus {
    object Idle : UpdateStatus
    object Checking : UpdateStatus
    data class UpToDate(val latestVersion: String, val checkedAt: Long = System.currentTimeMillis()) : UpdateStatus
    data class Available(val info: AppUpdateInfo) : UpdateStatus
    data class Downloading(val info: AppUpdateInfo, val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateStatus
    data class ReadyToInstall(val info: AppUpdateInfo, val apkFile: java.io.File) : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}

data class UiState(
    val connected: Boolean = false,
    val initializing: Boolean = false,
    val loading: Boolean = false,
    val progress: String = "",
    val error: String? = null,
    val snapshot: Snapshot? = null,
    val selectedStore: String = "",
    val includeTest: Boolean = false,
    val currency: String = "USD",
    val period: Period = Period.MONTH,
    val tab: Int = 0,
    val pushEnabled: Boolean = false,
    val pushOrigin: String = "",
    val pushBusy: Boolean = false,
    val pushMessage: String = "",
    val updateStatus: UpdateStatus = UpdateStatus.Idle,
    val showUpdateDialog: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val secure = SecureStore(application)
    private val api = LemonApi()
    private val storageLock = Any()
    @Volatile private var generation = 0L
    private var operation: Job? = null

    var state by mutableStateOf(UiState(
        connected = secure.readToken() != null,
        initializing = secure.readToken() != null,
        selectedStore = secure.storeId(),
        includeTest = secure.showTests(),
        currency = secure.currency(),
        pushEnabled = secure.readPushConfig() != null,
        pushOrigin = secure.readPushConfig()?.origin.orEmpty()
    ))
        private set

    init {
        viewModelScope.launch {
            checkForUpdate(isManual = false)
        }
        if (state.connected) {
            val expected = generation
            operation = viewModelScope.launch {
                val cached = withContext(Dispatchers.IO) {
                    synchronized(storageLock) {
                        secure.readSnapshot()?.let { runCatching { SnapshotCodec.decode(it) }.getOrNull() }
                    }
                }
                if (expected != generation) return@launch
                state = state.copy(snapshot = cached, initializing = false)
                startRefresh(forceFull = false, isSilent = true)
            }
        }
    }

    fun connect(raw: String) {
        val token = raw.trim()
        if (token.isEmpty()) {
            state = state.copy(error = "Saisis une clé API.")
            return
        }
        if (state.loading) return
        val expected = generation
        state = state.copy(loading = true, error = null, progress = "Vérification de la clé…")
        operation = viewModelScope.launch {
            try {
                api.validate(token)
                // Retain a validated token even if downloading a large history fails.
                // The next launch can resume without requiring the user to paste it again.
                withContext(Dispatchers.IO) {
                    synchronized(storageLock) {
                        coroutineContext.ensureActive()
                        if (expected != generation) throw CancellationException("Session terminée")
                        secure.saveToken(token)
                        secure.saveStoreId("")
                    }
                }
                if (expected != generation) return@launch
                state = state.copy(connected = true)
                val snapshot = api.fetch(token, { updateProgress(it, expected) })
                    .withFallbackFx(state.snapshot)
                // Prevent an in-flight login from resurrecting data after disconnect.
                withContext(Dispatchers.IO) {
                    synchronized(storageLock) {
                        coroutineContext.ensureActive()
                        if (expected != generation) throw CancellationException("Session terminée")
                        secure.saveSnapshot(SnapshotCodec.encode(snapshot))
                    }
                }
                if (expected != generation) return@launch
                state = state.copy(connected = true, loading = false, progress = "",
                    snapshot = snapshot, selectedStore = "", tab = 0)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (expected == generation) {
                    state = state.copy(loading = false, progress = "",
                        error = e.message ?: "Connexion impossible. Vérifie ton réseau et ta clé API.")
                }
            }
        }
    }

    fun refresh() = startRefresh(forceFull = false, isSilent = false)
    fun refreshFull() = startRefresh(forceFull = true, isSilent = false)

    private fun startRefresh(forceFull: Boolean, isSilent: Boolean = false) {
        if (state.loading) return
        val token = secure.readToken() ?: run {
            state = state.copy(connected = false, error = "Clé introuvable, reconnecte-toi.")
            return
        }
        val expected = generation
        state = state.copy(loading = true, error = if (isSilent) state.error else null, progress = "Synchronisation…")
        operation = viewModelScope.launch {
            try {
                val previous = state.snapshot
                val fresh = api.fetch(token, { updateProgress(it, expected) },
                    previous = previous, forceFull = forceFull).withFallbackFx(previous)
                withContext(Dispatchers.IO) {
                    synchronized(storageLock) {
                        coroutineContext.ensureActive()
                        if (expected != generation) throw CancellationException("Session terminée")
                        secure.saveSnapshot(SnapshotCodec.encode(fresh))
                    }
                }
                if (expected != generation) return@launch
                state = state.copy(loading = false, progress = "", snapshot = fresh)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (expected == generation) {
                    val errorMessage = if (isSilent && state.snapshot != null) null
                        else e.message ?: "Synchronisation impossible. Cache conservé."
                    state = state.copy(loading = false, progress = "", error = errorMessage)
                }
            }
        }
    }

    private suspend fun updateProgress(value: String, expected: Long) {
        withContext(Dispatchers.Main) {
            if (expected == generation) state = state.copy(progress = value)
        }
    }

    private fun Snapshot.withFallbackFx(previous: Snapshot?): Snapshot =
        if (eurPerUsd != null) this else copy(eurPerUsd = previous?.eurPerUsd, fxDate = previous?.fxDate ?: "")

    fun store(id: String) { secure.saveStoreId(id); state = state.copy(selectedStore = id) }
    fun tests(show: Boolean) { secure.saveShowTests(show); state = state.copy(includeTest = show) }
    fun currency(value: String) { secure.saveCurrency(value); state = state.copy(currency = value) }
    fun period(value: Period) { state = state.copy(period = value) }
    fun tab(value: Int) { state = state.copy(tab = value) }
    fun clearError() { state = state.copy(error = null) }

    fun enablePush(origin: String, secret: String) {
        if (state.pushBusy) return
        val expected = generation
        state = state.copy(pushBusy = true, pushMessage = "")
        viewModelScope.launch {
            try {
                val config = PushNotifications.register(getApplication(), origin, secret)
                if (expected != generation) {
                    runCatching { PushNotifications.unregister(config.origin, config.secret) }
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    synchronized(storageLock) {
                        coroutineContext.ensureActive()
                        if (expected != generation) throw CancellationException("Session terminée")
                        secure.savePushConfig(config)
                    }
                }
                if (expected != generation) return@launch
                state = state.copy(pushBusy = false, pushEnabled = true,
                    pushOrigin = config.origin, pushMessage = "Alertes activées.")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (expected == generation) state = state.copy(pushBusy = false, pushMessage =
                    error.message ?: "Activation des alertes impossible.")
            }
        }
    }

    fun disablePush() {
        if (state.pushBusy) return
        val config = secure.readPushConfig()
        if (config == null) {
            state = state.copy(pushEnabled = false, pushOrigin = "", pushMessage = "")
            return
        }
        val expected = generation
        state = state.copy(pushBusy = true, pushMessage = "")
        viewModelScope.launch {
            try {
                PushNotifications.unregister(config.origin, config.secret)
                if (expected != generation) return@launch
                withContext(Dispatchers.IO) {
                    synchronized(storageLock) {
                        if (expected == generation) secure.clearPushConfig()
                    }
                }
                if (expected != generation) return@launch
                state = state.copy(pushBusy = false, pushEnabled = false,
                    pushOrigin = "", pushMessage = "Alertes désactivées.")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (expected == generation) state = state.copy(pushBusy = false, pushMessage =
                    "Désactivation distante impossible : " +
                        (error.message ?: "vérifie la connexion."))
            }
        }
    }

    fun disconnect() {
        generation++ // invalidate any pending work before removing data
        operation?.cancel()
        operation = null
        val push = secure.readPushConfig()
        // Best-effort remote unregistration and FCM token revocation.
        synchronized(storageLock) { secure.clear() }
        state = UiState()
        if (push != null) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { PushNotifications.unregister(push.origin, push.secret) }
                if (PushNotifications.available(getApplication())) {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken()
                }
            }
        }
    }

    fun checkForUpdate(isManual: Boolean = false) {
        if (state.updateStatus is UpdateStatus.Checking || state.updateStatus is UpdateStatus.Downloading) return
        if (isManual) {
            state = state.copy(updateStatus = UpdateStatus.Checking)
        }
        viewModelScope.launch {
            try {
                val info = AppUpdateManager.checkForUpdate()
                if (info != null) {
                    if (info.isUpdateAvailable) {
                        state = state.copy(
                            updateStatus = UpdateStatus.Available(info),
                            showUpdateDialog = true
                        )
                    } else {
                        state = state.copy(
                            updateStatus = UpdateStatus.UpToDate(info.latestVersion)
                        )
                    }
                } else if (isManual) {
                    state = state.copy(
                        updateStatus = UpdateStatus.UpToDate(BuildConfig.VERSION_NAME)
                    )
                }
            } catch (e: Exception) {
                if (isManual) {
                    state = state.copy(
                        updateStatus = UpdateStatus.Error(e.message ?: "Impossible de vérifier les mises à jour.")
                    )
                }
            }
        }
    }

    fun startDownloadUpdate(context: android.content.Context, info: AppUpdateInfo) {
        if (state.updateStatus is UpdateStatus.Downloading) return
        state = state.copy(
            updateStatus = UpdateStatus.Downloading(info, 0f, 0L, info.apkSize)
        )
        viewModelScope.launch {
            try {
                val file = AppUpdateManager.downloadApk(
                    context = context.applicationContext,
                    apkUrl = info.apkUrl,
                    targetVersion = info.latestVersion
                ) { bytesRead, totalBytes ->
                    val progress = if (totalBytes > 0) (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
                    state = state.copy(
                        updateStatus = UpdateStatus.Downloading(info, progress, bytesRead, totalBytes)
                    )
                }
                state = state.copy(
                    updateStatus = UpdateStatus.ReadyToInstall(info, file)
                )
            } catch (e: Exception) {
                state = state.copy(
                    updateStatus = UpdateStatus.Error("Erreur téléchargement : ${e.message ?: "Erreur réseau"}")
                )
            }
        }
    }

    fun dismissUpdateDialog() {
        state = state.copy(showUpdateDialog = false)
    }

    fun showUpdateDialog() {
        state = state.copy(showUpdateDialog = true)
    }

    override fun onCleared() {
        operation?.cancel()
        super.onCleared()
    }
}
