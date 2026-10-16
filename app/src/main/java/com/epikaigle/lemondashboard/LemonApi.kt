package com.epikaigle.lemondashboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

class LemonApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    class ApiFailure(message: String) : IOException(message)

    private fun safeUrl(url: String, lemonOnly: Boolean = true) = url.toHttpUrl().also {
        require(it.scheme == "https" &&
            it.host == (if (lemonOnly) "api.lemonsqueezy.com" else "api.frankfurter.dev") &&
            (!lemonOnly || it.encodedPath.startsWith("/v1/"))) {
            "URL de réponse non autorisée"
        }
    }

    private suspend fun request(url: String, token: String?, lemonOnly: Boolean = true): JSONObject =
        withContext(Dispatchers.IO) {
            val safe = safeUrl(url, lemonOnly)
            val builder = Request.Builder().url(safe).get()
                .header("Accept", if (lemonOnly) "application/vnd.api+json" else "application/json")
                .header("User-Agent", "LemonDashboard-Android/1.0")
            if (lemonOnly) {
                builder.header("Content-Type", "application/vnd.api+json")
                builder.header("Authorization", "Bearer ${token ?: throw ApiFailure("Clé API manquante")}")
            }
            val req = builder.build()
            repeat(3) { attempt ->
                client.newCall(req).execute().use { response ->
                    when (response.code) {
                        401, 403 -> throw ApiFailure("Clé API invalide, expirée ou accès refusé (HTTP ${response.code}).")
                        429 -> {
                            if (attempt == 2) throw ApiFailure("Limite de requêtes Lemon Squeezy atteinte. Réessaie plus tard.")
                            val seconds = response.header("Retry-After")?.toLongOrNull()?.coerceIn(1L, 30L)
                                ?: (2L * (attempt + 1))
                            delay(seconds * 1000)
                        }
                        in 500..599 -> {
                            if (attempt == 2) throw ApiFailure("Service distant temporairement indisponible (HTTP ${response.code}).")
                            delay(1_000L * (attempt + 1))
                        }
                        else -> {
                            if (!response.isSuccessful) throw ApiFailure("Erreur API (HTTP ${response.code}).")
                            val body = response.body?.string() ?: throw ApiFailure("Réponse API vide.")
                            return@withContext try { JSONObject(body) }
                            catch (_: Exception) { throw ApiFailure("Réponse JSON invalide.") }
                        }
                    }
                }
            }
            throw ApiFailure("Connexion impossible.")
        }

    suspend fun validate(token: String) {
        val json = request("https://api.lemonsqueezy.com/v1/users/me", token)
        if (json.optJSONObject("data") == null) throw ApiFailure("Réponse de validation inattendue.")
    }

    private suspend fun all(endpoint: String, token: String): List<JSONObject> {
        var next: String? = "https://api.lemonsqueezy.com/v1/$endpoint?page%5Bsize%5D=100"
        val seen = mutableSetOf<String>()
        val result = mutableListOf<JSONObject>()
        var page = 0
        while (next != null) {
            if (!seen.add(next) || ++page > 3000) {
                throw ApiFailure("Pagination anormale ou historique trop volumineux. Aucune donnée partielle enregistrée.")
            }
            val json = request(next, token)
            val data = json.optJSONArray("data") ?: throw ApiFailure("Liste API manquante : $endpoint.")
            for (i in 0 until data.length()) result.add(data.getJSONObject(i))
            val links = json.optJSONObject("links")
            next = if (links == null || links.isNull("next")) null
                else links.optString("next").also { require(it.startsWith("https://")) { "Pagination invalide" } }
            // ~170 requêtes / minute, en dessous de la limite habituelle de 300/min.
            if (next != null) delay(350)
        }
        return result
    }

    private fun JSONObject.str(name: String): String = if (isNull(name)) "" else optString(name, "")
    private fun JSONObject.idValue(name: String): String = if (isNull(name)) "" else opt(name)?.toString() ?: ""
    private fun attr(json: JSONObject): JSONObject =
        json.optJSONObject("attributes") ?: throw ApiFailure("Attributs manquants.")

    suspend fun fetch(token: String, progress: suspend (String) -> Unit): Snapshot = withContext(Dispatchers.IO) {
        progress("Récupération des boutiques…")
        val stores = all("stores", token).map { s ->
            val a = attr(s)
            Store(s.getString("id"), a.str("name").ifBlank { "Boutique ${s.getString("id")}" }, a.str("currency").ifBlank { "USD" })
        }

        progress("Récupération des abonnements…")
        val rawSubs = all("subscriptions", token)
        progress("Récupération des commandes…")
        val orders = all("orders", token)
        progress("Récupération des factures récurrentes…")
        val invoices = all("subscription-invoices", token)
        progress("Récupération des tarifs…")
        val pricesById = if (rawSubs.isEmpty()) emptyMap() else all("prices", token).associateBy { it.optString("id") }

        val invoiceData = invoices.map { raw -> raw to attr(raw) }
        val mostRecentPaidInvoice = invoiceData
            .filter { (_, a) -> a.str("status") in setOf("paid", "partial_refund") }
            .groupBy { (_, a) -> a.idValue("subscription_id") }
            .mapValues { (_, values) -> values.maxByOrNull { (_, a) -> a.str("created_at") }?.second }

        val subscriptions = rawSubs.map { raw ->
            val a = attr(raw)
            val last = mostRecentPaidInvoice[raw.optString("id")]
            val item = a.optJSONObject("first_subscription_item")
            val price = item?.idValue("price_id")?.let { pricesById[it] }?.let { attr(it) }
            val units = item?.optLong("quantity", 1L) ?: 1L
            val fixedUnitCents = if (price != null && price.str("scheme") == "standard" && price.isNull("usage_aggregation") && !price.isNull("unit_price")) price.optLong("unit_price") else null
            val storeCurrency = stores.firstOrNull { it.id == a.idValue("store_id") }?.currency ?: "USD"
            Subscription(
                id = raw.getString("id"),
                storeId = a.idValue("store_id"),
                product = a.str("product_name"),
                variant = a.str("variant_name"),
                status = a.str("status"),
                renewsAt = a.str("renews_at"),
                endsAt = a.str("ends_at"),
                testMode = a.optBoolean("test_mode"),
                lastInvoiceUsdCents = last?.optLong("total_usd"),
                lastInvoiceDate = last?.str("created_at") ?: "",
                catalogPriceCents = fixedUnitCents?.times(units),
                catalogCurrency = storeCurrency,
                billingInterval = price?.str("renewal_interval_unit") ?: "",
                billingIntervalCount = price?.optInt("renewal_interval_quantity", 1) ?: 1
            )
        }

        val sales = LemonMapper.orders(orders) + LemonMapper.renewalInvoices(invoices, rawSubs)
        progress("Récupération du taux de change indicatif…")
        val fx = try {
            val response = request("https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR", null, false)
            val rate = response.optJSONObject("rates")?.optDouble("EUR")?.takeIf { it.isFinite() && it > 0 }
            rate to response.optString("date")
        } catch (_: Exception) { null to "" }

        Snapshot(
            stores = stores,
            sales = sales.distinctBy { it.id }.sortedByDescending { it.createdAt },
            subscriptions = subscriptions.sortedByDescending { it.renewsAt },
            fetchedAt = Instant.now().toString(),
            eurPerUsd = fx.first,
            fxDate = fx.second
        )
    }
}
