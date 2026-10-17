package com.epikaigle.lemondashboard

import org.junit.Assert.*
import org.junit.Test

class SnapshotCodecTest {
    @Test fun survivesRoundTripAndDoesNotContainCustomerPII() {
        val snapshot = Snapshot(
            listOf(Store("7", "Ma boutique")),
            listOf(Sale("order:1", "7", "Commande", "Produit", "paid", "EUR",
                1000, 100, 1090, 109, "2026-10-16T09:00:00Z", false)),
            listOf(Subscription("sub:1", "7", "Produit", "Mensuel", "active",
                "2026-11-16T00:00:00Z", "", false, 1090L)),
            "2026-10-16T20:00:00Z", 0.92, "2026-10-16", "2026-10-16T20:00:00Z"
        )
        val encoded = SnapshotCodec.encode(snapshot)
        assertFalse(encoded.contains("user_email"))
        val decoded = SnapshotCodec.decode(encoded)
        assertEquals(snapshot, decoded)
    }
    @Test fun oldCacheWithoutNewFieldsCanStillBeRead() {
        val snapshot = Snapshot(emptyList(), listOf(
            Sale("order:1", "1", "Commande", "App", "paid", "USD",
                1000, 0, 1000, 0, "2026-10-16T12:00:00Z", false)), emptyList(), "2026-10-16T13:00:00Z")
        val oldJson = org.json.JSONObject(SnapshotCodec.encode(snapshot))
        oldJson.remove("lastFullSyncAt")
        oldJson.getJSONArray("sales").getJSONObject(0).remove("refundedAt")
        val restored = SnapshotCodec.decode(oldJson.toString())
        assertEquals("", restored.lastFullSyncAt)
        assertEquals("", restored.sales.first().refundedAt)
    }

}
