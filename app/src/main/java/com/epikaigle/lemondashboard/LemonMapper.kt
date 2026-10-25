package com.epikaigle.lemondashboard

import org.json.JSONObject

/**
 * Pure financial mapping, independently unit tested with documented JSON:API fields.
 * Never copies user_name, user_email, card details, payment URLs or customer IDs.
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
            taxInclusive = a.taxInclusiveOrNull()
        )
    }

    fun renewalInvoices(invoices: List<JSONObject>, subscriptions: List<JSONObject>): List<Sale> {
        val productNames = subscriptions.associate { sub ->
            sub.getString("id") to sub.attributes().optString("product_name", "Abonnement")
        }
        return invoices.mapNotNull { record ->
            val a = record.attributes()
            val reason = a.getString("billing_reason")
            require(reason in setOf("initial", "renewal", "updated")) {
                "Motif de facture inconnu : vérification API nécessaire"
            }
            if (reason == "initial") return@mapNotNull null
            val subscriptionId = a.idValue("subscription_id")
            Sale(
                id = "invoice:${record.getString("id")}",
                storeId = a.idValue("store_id"),
                source = if (reason == "renewal") "Renouvellement" else "Ajustement",
                product = productNames[subscriptionId] ?: "Abonnement",
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
            taxInclusive = a.taxInclusiveOrNull()
            )
        }
    }
}
