package com.epikaigle.lemondashboard

import org.json.JSONArray
import org.json.JSONObject

data class Store(val id: String, val name: String, val currency: String = "USD")
data class Sale(
    val id: String,
    val storeId: String,
    val source: String,
    val product: String,
    val status: String,
    val currency: String,
    val originalCents: Long,
    val originalRefundCents: Long,
    val usdCents: Long,
    val usdRefundCents: Long,
    val createdAt: String,
    val testMode: Boolean,
    val subscriptionId: String = "",
    val refundedAt: String = ""
) {
    val paid: Boolean get() = status in setOf("paid", "refunded", "partial_refund")
    val netUsd: Long get() = if (paid) (usdCents - usdRefundCents).coerceAtLeast(0) else 0
}
data class Subscription(
    val id: String,
    val storeId: String,
    val product: String,
    val variant: String,
    val status: String,
    val renewsAt: String,
    val endsAt: String,
    val testMode: Boolean,
    val lastInvoiceUsdCents: Long? = null,
    val lastInvoiceDate: String = "",
    val catalogPriceCents: Long? = null,
    val catalogCurrency: String = "",
    val billingInterval: String = "",
    val billingIntervalCount: Int = 1
)

data class LicenseKey(
    val id: String,
    val storeId: String,
    val orderId: String,
    val orderItemId: String = "",
    val productId: String = "",
    val productName: String = "",
    val key: String,
    val keyShort: String = "",
    val status: String,
    val activationLimit: Int,
    val instancesCount: Int,
    val disabled: Boolean,
    val createdAt: String,
    val expiresAt: String = ""
)

data class Snapshot(
    val stores: List<Store>,
    val sales: List<Sale>,
    val subscriptions: List<Subscription>,
    val fetchedAt: String,
    val eurPerUsd: Double? = null,
    val fxDate: String = "",
    val lastFullSyncAt: String = "",
    val licenseKeys: List<LicenseKey> = emptyList()
)

object SnapshotCodec {
    fun encode(snapshot: Snapshot): String = JSONObject().apply {
        put("version", 1)
        put("fetchedAt", snapshot.fetchedAt)
        put("eurPerUsd", snapshot.eurPerUsd ?: JSONObject.NULL)
        put("fxDate", snapshot.fxDate)
        put("lastFullSyncAt", snapshot.lastFullSyncAt)
        put("stores", JSONArray().apply {
            snapshot.stores.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("currency", it.currency)) }
        })
        put("sales", JSONArray().apply {
            snapshot.sales.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("storeId", s.storeId); put("source", s.source)
                    put("product", s.product); put("status", s.status); put("currency", s.currency)
                    put("originalCents", s.originalCents); put("originalRefundCents", s.originalRefundCents)
                    put("usdCents", s.usdCents); put("usdRefundCents", s.usdRefundCents)
                    put("createdAt", s.createdAt); put("testMode", s.testMode)
                    put("subscriptionId", s.subscriptionId)
                    put("refundedAt", s.refundedAt)
                })
            }
        })
        put("subscriptions", JSONArray().apply {
            snapshot.subscriptions.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("storeId", s.storeId)
                    put("product", s.product); put("variant", s.variant)
                    put("status", s.status); put("renewsAt", s.renewsAt); put("endsAt", s.endsAt)
                    put("testMode", s.testMode)
                    put("lastInvoiceUsdCents", s.lastInvoiceUsdCents ?: JSONObject.NULL)
                    put("lastInvoiceDate", s.lastInvoiceDate)
                    put("catalogPriceCents", s.catalogPriceCents ?: JSONObject.NULL)
                    put("catalogCurrency", s.catalogCurrency)
                    put("billingInterval", s.billingInterval)
                    put("billingIntervalCount", s.billingIntervalCount)
                })
            }
        })
        put("licenseKeys", JSONArray().apply {
            snapshot.licenseKeys.forEach { k ->
                put(JSONObject().apply {
                    put("id", k.id); put("storeId", k.storeId); put("orderId", k.orderId)
                    put("orderItemId", k.orderItemId); put("productId", k.productId)
                    put("productName", k.productName); put("key", k.key)
                    put("keyShort", k.keyShort); put("status", k.status)
                    put("activationLimit", k.activationLimit); put("instancesCount", k.instancesCount)
                    put("disabled", k.disabled); put("createdAt", k.createdAt); put("expiresAt", k.expiresAt)
                })
            }
        })
    }.toString()

    fun decode(raw: String): Snapshot {
        val root = JSONObject(raw)
        require(root.optInt("version") == 1) { "Version de cache incompatible" }
        val stores = root.getJSONArray("stores").objects().map {
            Store(it.getString("id"), it.getString("name"), it.optString("currency", "USD"))
        }
        val sales = root.getJSONArray("sales").objects().map {
            Sale(
                id = it.getString("id"), storeId = it.getString("storeId"),
                source = it.getString("source"), product = it.getString("product"),
                status = it.getString("status"), currency = it.getString("currency"),
                originalCents = it.getLong("originalCents"),
                originalRefundCents = it.getLong("originalRefundCents"),
                usdCents = it.getLong("usdCents"), usdRefundCents = it.getLong("usdRefundCents"),
                createdAt = it.getString("createdAt"), testMode = it.getBoolean("testMode"),
                subscriptionId = it.optString("subscriptionId"),
                refundedAt = it.optString("refundedAt")
            )
        }
        val subscriptions = root.getJSONArray("subscriptions").objects().map {
            Subscription(
                id = it.getString("id"), storeId = it.getString("storeId"),
                product = it.getString("product"), variant = it.getString("variant"),
                status = it.getString("status"), renewsAt = it.getString("renewsAt"),
                endsAt = it.getString("endsAt"), testMode = it.getBoolean("testMode"),
                lastInvoiceUsdCents = if (it.isNull("lastInvoiceUsdCents")) null else it.getLong("lastInvoiceUsdCents"),
                lastInvoiceDate = it.optString("lastInvoiceDate"),
                catalogPriceCents = if (it.isNull("catalogPriceCents")) null else it.optLong("catalogPriceCents"),
                catalogCurrency = it.optString("catalogCurrency"),
                billingInterval = it.optString("billingInterval"),
                billingIntervalCount = it.optInt("billingIntervalCount", 1)
            )
        }
        val licenseKeys = root.optJSONArray("licenseKeys")?.objects()?.map {
            LicenseKey(
                id = it.getString("id"),
                storeId = it.getString("storeId"),
                orderId = it.optString("orderId"),
                orderItemId = it.optString("orderItemId"),
                productId = it.optString("productId"),
                productName = it.optString("productName"),
                key = it.getString("key"),
                keyShort = it.optString("keyShort"),
                status = it.getString("status"),
                activationLimit = it.optInt("activationLimit", 0),
                instancesCount = it.optInt("instancesCount", 0),
                disabled = it.optBoolean("disabled", false),
                createdAt = it.getString("createdAt"),
                expiresAt = it.optString("expiresAt")
            )
        } ?: emptyList()
        return Snapshot(stores, sales, subscriptions, root.getString("fetchedAt"),
            if (root.isNull("eurPerUsd")) null else root.getDouble("eurPerUsd"),
            root.optString("fxDate"), root.optString("lastFullSyncAt"),
            licenseKeys)
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
}
