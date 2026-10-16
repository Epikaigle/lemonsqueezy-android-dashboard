package com.epikaigle.lemondashboard

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AnalyticsTest {
    private val date = LocalDate.of(2026, 10, 16)
    private fun sale(
        id: String, amount: Long, refund: Long = 0, status: String = "paid",
        at: String = "2026-10-16T11:00:00Z", test: Boolean = false
    ) = Sale(id, "1", "Commande", "Produit", status, "USD",
        amount, refund, amount, refund, at, test)

    @Test fun excludesPendingAndOutsidePeriod() {
        val sales = listOf(
            sale("a", 1500), sale("b", 800, status = "pending"),
            sale("c", 600, at = "2025-01-10T11:00:00Z")
        )
        val m = Analytics.metrics(sales, emptyList(), Period.MONTH, date)
        assertEquals(1500L, m.netUsdCents)
        assertEquals(1, m.payments)
    }

    @Test fun handlesRefundsWithoutNegativeNet() {
        val m = Analytics.metrics(listOf(sale("a", 2000, 500),
            sale("b", 700, 700)), emptyList(), Period.MONTH, date)
        assertEquals(2700L, m.grossUsdCents)
        assertEquals(1200L, m.refundedUsdCents)
        assertEquals(1500L, m.netUsdCents)
    }

    @Test fun excludesTestByDefault() {
        val data = Snapshot(emptyList(), listOf(sale("real", 100), sale("test", 500, test = true)),
            emptyList(), "2026-10-16T00:00:00Z")
        assertEquals(1, Analytics.selectedSales(data, "", false).size)
        assertEquals(2, Analytics.selectedSales(data, "", true).size)
    }

    @Test fun allPeriodHasYearlyBuckets() {
        val buckets = Analytics.graphBuckets(
            listOf(sale("old", 100, at = "2025-10-16T12:00:00Z"), sale("now", 200)),
            Period.ALL, date
        )
        assertEquals(listOf("2025", "2026"), buckets.map { it.first })
        assertEquals(listOf(100L, 200L), buckets.map { it.second })
    }

    @Test fun comparesElapsedMonthNotEntirePreviousMonth() {
        val records = listOf(
            sale("previous-early", 1000, at = "2026-09-05T11:00:00Z"),
            sale("previous-late", 4000, at = "2026-09-27T11:00:00Z"),
            sale("current", 2500, at = "2026-10-10T11:00:00Z")
        )
        assertEquals(1000L, Analytics.previousPeriodNet(records, Period.MONTH, date))
    }

    @Test fun comparesPreviousSevenDays() {
        val records = listOf(
            sale("previous", 700, at = "2026-10-06T11:00:00Z"),
            sale("this-week", 1700, at = "2026-10-13T11:00:00Z")
        )
        assertEquals(700L, Analytics.previousPeriodNet(records, Period.WEEK, date))
    }

    @Test fun allPeriodHasNoComparison() {
        assertNull(Analytics.previousPeriodNet(listOf(sale("a", 100)), Period.ALL, date))
    }

    @Test fun largeHistoryHasCorrectGraphTotals() {
        val sales = List(100_000) { i ->
            sale("sale:$i", 10, at = "2026-10-16T11:00:00Z")
        }
        val graph = Analytics.graphBuckets(sales, Period.MONTH, date)
        assertEquals(1_000_000L, graph.sumOf { it.second })
        assertEquals(1_000_000L, graph.first { it.first == "16" }.second)
    }

    @Test fun activeCountDoesNotIncludeCancelledGrace() {
        val sub = { id: String, status: String ->
            Subscription(id, "1", "Produit", "", status, "", "", false)
        }
        val m = Analytics.metrics(emptyList(),
            listOf(sub("1", "active"), sub("2", "cancelled"), sub("3", "on_trial")),
            Period.ALL, date)
        assertEquals(1, m.activeSubscriptions)
    }
}
