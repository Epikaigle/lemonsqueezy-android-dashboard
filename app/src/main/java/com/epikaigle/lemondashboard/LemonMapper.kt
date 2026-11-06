package com.epikaigle.lemondashboard

import org.json.JSONObject

/**
 * Financial and customer mapping, independently unit tested with documented JSON:API fields.
 * Extracts user_name and user_email for customer identification on orders and licenses.
 * Never stores card details or sensitive payment credentials.
 * Unknown billing reasons fail closed to prevent silent over-counting.
 */
internal object LemonMapper {
    private fun JSONObject.attributes(): JSONObject = getJSONObject("attributes")
    private fun JSONObject.idValue(key: String): String =
        if (isNull(key)) "" else opt(key)?.toString() ?: ""

    private fun JSONObject.optionalCents(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null

    private fun JSONObject.taxInclusiveOrNull(): Boolean? =
        if (has("tax_inclusive") && !isNull("tax_inclusive")) getBoolean("tax_inclusive") else null

    fun orders(rows: List<JSONObject>): List<Sale> = rows.map { record ->
        val a = record.attributes()
        Sale(
            id = "order:${record.getString("id")}",
            storeId = a.idValue("store_id"),
            source = "Commande",
            product = a.optJSONObject("first_order_item")
                ?.optString("product_name")?.takeIf { it.isNotBlank() } ?: "Commande",
            status = a.getString("status"),
            currency = a.getString("currency"),
            originalCents = a.getLong("total"),
            originalRefundCents = a.getLong("refunded_amount"),
            usdCents = a.getLong("total_usd"),
            usdRefundCents = a.getLong("refunded_amount_usd"),
            createdAt = a.getString("created_at"),
            testMode = a.getBoolean("test_mode"),
            refundedAt = if (a.isNull("refunded_at")) "" else a.optString("refunded_at"),
            subtotalCents = a.optionalCents("subtotal"),
            discountCents = a.optionalCents("discount_total"),
            taxCents = a.optionalCents("tax"),
            subtotalUsdCents = a.optionalCents("subtotal_usd"),
            discountUsdCents = a.optionalCents("discount_total_usd"),
            taxUsdCents = a.optionalCents("tax_usd"),
            taxName = a.optString("tax_name", ""),
            taxRate = a.optString("tax_rate", "").toDoubleOrNull(),
            taxInclusive = a.taxInclusiveOrNull(),
            userName = a.optString("user_name", ""),
            userEmail = a.optString("user_email", "")
        )
    }

    fun renewalInvoices(invoices: List<JSONObject>, subscriptions: List<JSONObject>): List<Sale> {
        val subMeta = subscriptions.associate { sub ->
            val attr = sub.attributes()
            sub.getString("id") to Triple(
                attr.optString("product_name", "Abonnement"),
                attr.optString("user_name", ""),
                attr.optString("user_email", "")
            )
        }
        return invoices.mapNotNull { record ->
            val a = record.attributes()
            val reason = a.getString("billing_reason")
            require(reason in setOf("initial", "renewal", "updated")) {
                "Motif de facture inconnu : vérification API nécessaire"
            }
            if (reason == "initial") return@mapNotNull null
            val subscriptionId = a.idValue("subscription_id")
            val meta = subMeta[subscriptionId]
            val userName = a.optString("user_name", "").ifBlank { meta?.second.orEmpty() }
            val userEmail = a.optString("user_email", "").ifBlank { meta?.third.orEmpty() }
            Sale(
                id = "invoice:${record.getString("id")}",
                storeId = a.idValue("store_id"),
                source = if (reason == "renewal") "Renouvellement" else "Ajustement",
                product = meta?.first ?: "Abonnement",
                status = a.getString("status"),
                currency = a.getString("currency"),
                originalCents = a.getLong("total"),
                originalRefundCents = a.getLong("refunded_amount"),
                usdCents = a.getLong("total_usd"),
                usdRefundCents = a.getLong("refunded_amount_usd"),
                createdAt = a.getString("created_at"),
                testMode = a.getBoolean("test_mode"),
                subscriptionId = subscriptionId,
                refundedAt = if (a.isNull("refunded_at")) "" else a.optString("refunded_at"),
                subtotalCents = a.optionalCents("subtotal"),
                discountCents = a.optionalCents("discount_total"),
                taxCents = a.optionalCents("tax"),
                subtotalUsdCents = a.optionalCents("subtotal_usd"),
                discountUsdCents = a.optionalCents("discount_total_usd"),
                taxUsdCents = a.optionalCents("tax_usd"),
                taxName = a.optString("tax_name", ""),
                taxRate = a.optString("tax_rate", "").toDoubleOrNull(),
                taxInclusive = a.taxInclusiveOrNull(),
                userName = userName,
                userEmail = userEmail
            )
        }
    }
}
