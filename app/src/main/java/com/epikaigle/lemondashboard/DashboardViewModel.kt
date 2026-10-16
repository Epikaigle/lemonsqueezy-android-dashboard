package com.epikaigle.lemondashboard

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val connected: Boolean = false,
    val loading: Boolean = false,
    val progress: String = "",
    val error: String? = null,
    val snapshot: Snapshot? = null,
    val selectedStore: String = "",
    val includeTest: Boolean = false,
    val currency: String = "USD",
    val period: Period = Period.MONTH,
    val tab: Int = 0
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val secure = SecureStore(application)
    private val api = LemonApi()
    var state by mutableStateOf(UiState(
        connected = secure.readToken() != null,
        snapshot = secure.readSnapshot()?.let { runCatching { SnapshotCodec.decode(it) }.getOrNull() },
        selectedStore = secure.storeId(),
        includeTest = secure.showTests(),
        currency = secure.currency()
    ))
        private set

    init { if (state.connected) refresh() }

    fun connect(raw: String) {
        val token = raw.trim()
        if (token.isEmpty()) { state = state.copy(error = "Saisis une clé API."); return }
        if (state.loading) return
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, progress = "Vérification de la clé…")
            try {
                api.validate(token)
                val snapshot = api.fetch(token) { updateProgress(it) }
                val previous = state.snapshot
                val complete = snapshot.withFallbackFx(previous)
                withContext(Dispatchers.IO) {
                    secure.saveToken(token)
                    secure.saveSnapshot(SnapshotCodec.encode(complete))
                }
                secure.saveStoreId("")
                state = state.copy(connected = true, loading = false, progress = "",
                    snapshot = complete, selectedStore = "", tab = 0)
            } catch (e: Exception) {
                state = state.copy(loading = false, progress = "",
                    error = e.message ?: "Connexion impossible. Vérifie ton réseau et ta clé API.")
            }
        }
    }

    fun refresh() {
        if (state.loading) return
        val token = secure.readToken() ?: run {
            state = state.copy(connected = false, error = "Clé introuvable, reconnecte-toi.")
            return
        }
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, progress = "Synchronisation…")
            try {
                api.validate(token)
                val snapshot = api.fetch(token) { updateProgress(it) }.withFallbackFx(state.snapshot)
                withContext(Dispatchers.IO) { secure.saveSnapshot(SnapshotCodec.encode(snapshot)) }
                state = state.copy(loading = false, progress = "", snapshot = snapshot)
            } catch (e: Exception) {
                state = state.copy(loading = false, progress = "",
                    error = e.message ?: "Synchronisation impossible. Cache conservé.")
            }
        }
    }

    private suspend fun updateProgress(value: String) {
        withContext(Dispatchers.Main) { state = state.copy(progress = value) }
    }

    private fun Snapshot.withFallbackFx(previous: Snapshot?): Snapshot =
        if (eurPerUsd != null) this else copy(eurPerUsd = previous?.eurPerUsd, fxDate = previous?.fxDate ?: "")

    fun store(id: String) { secure.saveStoreId(id); state = state.copy(selectedStore = id) }
    fun tests(show: Boolean) { secure.saveShowTests(show); state = state.copy(includeTest = show) }
    fun currency(value: String) { secure.saveCurrency(value); state = state.copy(currency = value) }
    fun period(value: Period) { state = state.copy(period = value) }
    fun tab(value: Int) { state = state.copy(tab = value) }
    fun clearError() { state = state.copy(error = null) }
    fun disconnect() {
        secure.clear()
        state = UiState()
    }
}
