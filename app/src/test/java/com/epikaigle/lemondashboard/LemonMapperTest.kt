package com.epikaigle.lemondashboard

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LemonMapperTest {
    private fun row(id: String, extra: Map<String, Any> = emptyMap()): JSONObject {
        val a = JSONObject()
            .put("store_id", 1).put("subscription_id", 12)
            .put("status", "paid").put("currency", "USD")
            .put("total", 1299).put("refunded_amount", 0)
            .put("total_usd", 1299).put("refunded_amount_usd", 0)
            .put("created_at", "2026-10-17T00:00:00Z")
            .put("test_mode", false).put("user_email", "private@invalid.example")
        extra.forEach { (key, value) -> a.put(key, value) }
        return JSONObject().put("id", id).put("attributes", a)
    }

    @Test fun mapsTaxAndDiscountWithoutInventingFees() {
        val record = row("financial", mapOf(
            "subtotal" to 2000, "subtotal_usd" to 2000,
            "discount_total" to 100, "discount_total_usd" to 100,
            "tax" to 380, "tax_usd" to 380,
            "tax_name" to "VAT", "tax_rate" to "20.00",
            "tax_inclusive" to false
        ))
        val sale = LemonMapper.orders(listOf(record)).single()
        assertEquals(380L, sale.taxUsdCents)
        assertEquals(100L, sale.discountUsdCents)
        assertEquals("VAT", sale.taxName)
        assertEquals(20.0, sale.taxRate!!, 0.001)
        assertEquals(false, sale.taxInclusive)
        assertNull(LemonMapper.orders(listOf(row("old"))).single().taxUsdCents)
    }

    @Test fun initialInvoiceIsNotAddedTwice() {
        val order = row("1", mapOf("first_order_item" to JSONObject().put("product_name", "App")))
        val initial = row("2", mapOf("billing_reason" to "initial"))
        val renewal = row("3", mapOf("billing_reason" to "renewal"))
        val updated = row("4", mapOf("billing_reason" to "updated"))
        val sales = LemonMapper.orders(listOf(order)) +
            LemonMapper.renewalInvoices(listOf(initial, renewal, updated), emptyList())
        assertEquals(3, sales.size)
        assertEquals(1299L * 3, sales.sumOf { it.netUsd })
        assertEquals(1, sales.count { it.id.startsWith("order:") })
        assertEquals(2, sales.count { it.id.startsWith("invoice:") })
        assertEquals("App", sales.first().product)
    }

    @Test fun refundedAndPendingPaymentsStayAccurate() {
        val refunded = row("5", mapOf("billing_reason" to "renewal",
            "status" to "partial_refund", "refunded_amount_usd" to 499, "refunded_amount" to 499))
        val pending = row("6", mapOf("billing_reason" to "renewal", "status" to "pending"))
        val sales = LemonMapper.renewalInvoices(listOf(refunded, pending), emptyList())
        assertEquals(800L, sales[0].netUsd)
        assertEquals(0L, sales[1].netUsd)
    }

    @Test fun rejectsUnknownInvoiceReasonRatherThanMiscount() {
        val unknown = row("7", mapOf("billing_reason" to "other"))
        assertThrows(IllegalArgumentException::class.java) {
            LemonMapper.renewalInvoices(listOf(unknown), emptyList())
        }
    }

    @Test fun neverCopiesPersonallyIdentifyingFields() {
        val parsed = LemonMapper.orders(listOf(row("8")))
        val cache = SnapshotCodec.encode(Snapshot(emptyList(), parsed, emptyList(), "2026-10-17T00:00:00Z"))
        assertFalse(cache.contains("private@invalid.example"))
        assertFalse(cache.contains("user_email"))
    }

    @Test fun missingRequiredFinancialFieldFailsInsteadOfSilentlyRecordingZero() {
        val bad = row("9")
        bad.getJSONObject("attributes").remove("total_usd")
        assertThrows(org.json.JSONException::class.java) {
            LemonMapper.orders(listOf(bad))
        }
    }
    @Test fun mapsFullRefundDateButNotUnknownPartialDate() {
        val full = row("10", mapOf("status" to "refunded",
            "refunded_amount" to 1299, "refunded_amount_usd" to 1299,
            "refunded_at" to "2026-10-17T12:00:00Z"))
        val partial = row("11", mapOf("status" to "partial_refund",
            "refunded_amount" to 100, "refunded_amount_usd" to 100,
            "refunded_at" to JSONObject.NULL))
        val mapped = LemonMapper.orders(listOf(full, partial))
        assertEquals("2026-10-17T12:00:00Z", mapped[0].refundedAt)
        assertEquals("", mapped[1].refundedAt)
    }

}
