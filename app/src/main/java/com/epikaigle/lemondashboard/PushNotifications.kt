package com.epikaigle.lemondashboard

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PushConfig(val origin: String, val secret: String)

object PushNotifications {
    const val CHANNEL_ID = "lemon_sales"
    private val network = OkHttpClient.Builder()
        .followRedirects(false).followSslRedirects(false)
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(18, TimeUnit.SECONDS).build()

    fun available(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()
    fun permissionGranted(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID, "Ventes Lemon Squeezy", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Ventes, renouvellements et remboursements" }
        manager.createNotificationChannel(channel)
    }

    private fun endpoint(origin: String): String {
        val url = origin.trim().trimEnd('/').toHttpUrl()
        require(url.scheme == "https" && url.username.isEmpty() && url.password.isEmpty() &&
            url.query == null && url.fragment == null && url.encodedPath == "/") {
            "Utilise seulement l'adresse HTTPS du Worker, sans chemin."
        }
        return url.newBuilder().addPathSegments("device/register").build().toString()
    }

    private suspend fun getToken(): String = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful && !task.result.isNullOrEmpty()) continuation.resume(task.result!!)
            else continuation.resumeWithException(
                task.exception ?: IOException("Jeton Firebase indisponible"))
        }
    }

    suspend fun register(context: Context, origin: String, secret: String) {
        require(available(context)) {
            "Firebase non configuré dans cette APK. Ajoute le fichier google-services.json à la compilation."
        }
        require(permissionGranted(context)) { "Autorise les notifications Android." }
        require(secret.trim().length >= 20) { "Le secret d'appairage doit contenir au moins 20 caractères." }
        endpoint(origin)
        val token = getToken()
        registerToken(origin, secret.trim(), token)
        withContext(Dispatchers.IO) {
            SecureStore(context).savePushConfig(PushConfig(origin.trim().trimEnd('/'), secret.trim()))
        }
    }

    suspend fun registerToken(origin: String, secret: String, token: String) {
        require(token.isNotBlank())
        request(origin, secret, "POST", JSONObject().put("token", token))
    }

    suspend fun unregister(origin: String, secret: String) {
        request(origin, secret, "DELETE", null)
    }

    suspend fun status(config: PushConfig): Boolean {
        return request(config.origin, config.secret, "GET", null).optBoolean("registered")
    }

    private suspend fun request(origin: String, secret: String, method: String, json: JSONObject?): JSONObject =
        withContext(Dispatchers.IO) {
            val url = endpoint(origin)
            val builder = Request.Builder().url(url).header("Authorization", "Bearer " + secret)
                .header("Accept", "application/json")
            val req = when (method) {
                "POST" -> builder.post((json?.toString() ?: "{}").toRequestBody(
                    "application/json; charset=utf-8".toMediaType()))
                "DELETE" -> builder.delete()
                else -> builder.get()
            }.build()
            network.newCall(req).execute().use { response ->
                if (!response.isSuccessful) throw IOException(
                    when (response.code) {
                        401 -> "Secret d'appairage refusé."
                        503 -> "Le relais de notifications n'est pas encore configuré."
                        else -> "Relais indisponible (HTTP " + response.code + ")."
                    }
                )
                JSONObject(response.body?.string() ?: "{}")
            }
        }

    fun refreshToken(context: Context, token: String) {
        val config = SecureStore(context).readPushConfig() ?: return
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { registerToken(config.origin, config.secret, token) }
        }
    }

    fun showForegroundNotification(context: Context, title: String, message: String) {
        if (!permissionGranted(context) || SecureStore(context).readPushConfig() == null) return
        createChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title.take(100))
            .setContentText(message.take(220))
            .setStyle(Notification.BigTextStyle().bigText(message.take(220)))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify((System.currentTimeMillis() and 0x7fffffff).toInt(), notification)
    }
}

class LemonPushService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushNotifications.refreshToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val alert = message.notification ?: return
        PushNotifications.showForegroundNotification(
            applicationContext, alert.title ?: "Lemon Dashboard", alert.body.orEmpty()
        )
    }
}
