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
            sale("b", 700, 700)), emptyList(), Period.ALL, date)
        assertEquals(2700L, m.grossUsdCents)
        assertEquals(1200L, m.refundedUsdCents)
        assertEquals(1500L, m.netUsdCents)
    }

    @Test fun excludesTestByDefault() {
        val data = Snapshot(emptyList(), listOf(sale("real", 100), sale("test", 500, test = true)),
            emptyList(), "2026-10-16T00:00:00Z")
        assertEquals(1, Analytics.selectedSales(data, "").size)
    }

    @Test fun estimatedFeesDoNotMisrepresentRefundsOrMissingTaxes() {
        val standard = sale("standard", 2000).copy(taxUsdCents = 400)
        assertEquals(150L, Analytics.estimatedPlatformFeeUsdCents(standard))
        assertEquals(1450L, Analytics.estimatedAfterTaxesAndFeesUsdCents(standard))
        assertNull(Analytics.estimatedAfterTaxesAndFeesUsdCents(standard.copy(taxUsdCents = null)))
        assertNull(Analytics.estimatedAfterTaxesAndFeesUsdCents(standard.copy(usdRefundCents = 200)))
        assertNull(Analytics.estimatedPlatformFeeUsdCents(standard.copy(status = "pending")))
        assertEquals(160L, Analytics.estimatedPlatformFeeUsdCents(standard.copy(source = "Renouvellement")))
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

    @Test fun dayPeriodAndHourlyBuckets() {
        val rows = listOf(
            sale("morning", 450, at = "2026-10-16T09:00:00Z"),
            sale("evening", 550, at = "2026-10-16T16:00:00Z"),
            sale("old", 200, at = "2026-10-15T16:00:00Z")
        )
        val hours = Analytics.graphBuckets(rows, Period.DAY, date)
        assertEquals(24, hours.size)
        assertEquals(1000L, hours.sumOf { it.second })
        assertEquals(1000L, Analytics.metrics(rows, emptyList(), Period.DAY, date).netUsdCents)
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
    @Test fun fullRefundBelongsToActualRefundPeriod() {
        val refunded = sale("full", 1800L, refund = 1800L, status = "refunded",
            at = "2026-09-10T09:00:00Z").copy(refundedAt = "2026-10-12T11:00:00Z")
        val october = Analytics.metrics(listOf(refunded), emptyList(), Period.MONTH, date)
        assertEquals(0L, october.grossUsdCents)
        assertEquals(1800L, october.refundedUsdCents)
        assertEquals(-1800L, october.netUsdCents)
        assertEquals(-1800L, Analytics.graphBuckets(listOf(refunded), Period.MONTH, date)
            .first { it.first == "12" }.second)
    }

    @Test fun partialRefundWithUnknownDateIsNotBackdated() {
        val partial = sale("partial", 2000L, refund = 500L, status = "partial_refund",
            at = "2026-09-10T09:00:00Z")
        assertEquals(0L, Analytics.metrics(listOf(partial), emptyList(), Period.MONTH, date).netUsdCents)
        assertEquals(1500L, Analytics.metrics(listOf(partial), emptyList(), Period.ALL, date).netUsdCents)
        assertEquals(500L, Analytics.undatedRefundUsdCents(listOf(partial)))
        assertEquals(-500L, Analytics.graphBuckets(listOf(partial), Period.ALL, date).last().second)
        assertEquals("Sans date", Analytics.graphBuckets(listOf(partial), Period.ALL, date).last().first)
    }

    @Test fun priorPeriodUsesRefundDateInsteadOfSaleDate() {
        val refunded = sale("ref", 1000, refund = 1000, status = "refunded",
            at = "2026-08-01T11:00:00Z").copy(refundedAt = "2026-09-05T09:00:00Z")
        assertEquals(-1000L, Analytics.previousPeriodNet(listOf(refunded), Period.MONTH, date))
    }

    @Test fun payoutEstimatesCalculatesHoldingAndReady() {
        // date is 2026-10-16. Next payout should be 2026-10-28 (in 12 days).
        // 13-day holding cutoff is 2026-10-03.
        val oldSale = sale("old", 5000, at = "2026-10-01T10:00:00Z") // ready
        val recentSale = sale("recent", 3000, at = "2026-10-10T10:00:00Z") // holding
        val est = Analytics.payoutEstimates(listOf(oldSale, recentSale), date)
        assertEquals(5000L, est.readyUsdCents)
        assertEquals(3000L, est.holdingUsdCents)
        assertEquals(LocalDate.of(2026, 10, 28), est.nextPayoutDate)
        assertEquals(12L, est.daysUntilNextPayout)
    }

    @Test fun payoutEstimatesCalculatesPastAndProjectedPayouts() {
        // date is 2026-10-16.
        // Past cycle: Sept 20 sale (> 15th -> Oct 14 payout). Matured Oct 3, paid on Oct 14.
        val pastSale = sale("past", 6500, at = "2026-09-20T10:00:00Z")
        // Upcoming cycle: Oct 5 sale (<= 15th -> Oct 28 payout). Matures Oct 18 (< Oct 28).
        val currentSale = sale("curr", 5500, at = "2026-10-05T10:00:00Z")
        // Recent sale in holding (matures after next payout date):
        val futureHolding = sale("late", 1500, at = "2026-10-16T08:00:00Z")

        val est = Analytics.payoutEstimates(listOf(pastSale, currentSale, futureHolding), date)

        assertEquals(6500L, est.alreadyPaidUsdCents)
        assertEquals(1, est.pastPayouts.size)
        assertEquals(LocalDate.of(2026, 10, 14), est.pastPayouts[0].date)
        assertEquals(6500L, est.pastPayouts[0].amountUsdCents)

        assertEquals(5500L, est.projectedNextPayoutUsdCents)
        assertTrue(est.reachesThreshold)
        // On Oct 16, both Oct 5 (held 11 days) and Oct 16 are within the 13-day holding period:
        assertEquals(7000L, est.holdingUsdCents)
    }

    @Test fun realPocketNetCalculatesExactDeductions() {
        // Order 30€ (3345 USD cents), VAT 5.81€ (581 USD cents)
        val s = sale("lifetime", 3345, at = "2026-10-14T19:14:00Z").copy(taxUsdCents = 581L)
        val pocketNet = Analytics.realPocketNetUsdCents(s)
        // 3345 - 581 - (50 + round(3345 * 0.065) = 267) = 2497 * 0.99 = 2472
        assertEquals(2472L, pocketNet)

        val range = Analytics.moneyRange(1000L, "EUR", 1.0 / 1.1511)
        assertTrue(range.contains("–") || range.contains("-"))
    }

    @Test fun mrrCalculationNormalizesBillingIntervals() {
        val monthly = Subscription("1", "1", "Monthly", "", "active", "", "", false,
            catalogPriceCents = 2900L, billingInterval = "month", billingIntervalCount = 1)
        val yearly = Subscription("2", "1", "Yearly", "", "active", "", "", false,
            catalogPriceCents = 12000L, billingInterval = "year", billingIntervalCount = 1)
        val cancelled = Subscription("3", "1", "Cancelled", "", "cancelled", "", "", false,
            catalogPriceCents = 5000L, billingInterval = "month")
        val mrr = Analytics.mrrEstimateUsdCents(listOf(monthly, yearly, cancelled))
        // 2900 + 12000/12 = 2900 + 1000 = 3900
        assertEquals(3900L, mrr)
        assertEquals(3900L * 12L, Analytics.arrEstimateUsdCents(listOf(monthly, yearly, cancelled)))
    }

    @Test fun calculatesTopProductsRankedByRevenue() {
        val sales = listOf(
            sale("1", 2000, status = "paid").copy(product = "Plugin Pro"),
            sale("2", 3000, status = "paid").copy(product = "Desktop App"),
            sale("3", 5000, status = "paid").copy(product = "Plugin Pro"),
            sale("4", 1000, status = "refunded", refund = 1000).copy(product = "Desktop App"),
            sale("5", 500, status = "pending").copy(product = "Mobile App")
        )
        val top = Analytics.topProducts(sales)
        assertEquals(2, top.size)
        assertEquals("Plugin Pro", top[0].name)
        assertEquals(7000L, top[0].grossUsdCents)
        assertEquals(2, top[0].salesCount)

        assertEquals("Desktop App", top[1].name)
        assertEquals(4000L, top[1].grossUsdCents)
        assertEquals(3000L, top[1].netUsdCents)
        assertEquals(2, top[1].salesCount)
    }

    @Test fun calculatesSaaSChurnRateAndQuickRatio() {
        val subs = listOf(
            Subscription("1", "1", "SaaS", "", "active", "", "", false, 2900L),
            Subscription("2", "1", "SaaS", "", "active", "", "", false, 2900L),
            Subscription("3", "1", "SaaS", "", "active", "", "", false, 2900L),
            Subscription("4", "1", "SaaS", "", "cancelled", "", "", false, 2900L)
        )
        val churn = Analytics.churnRatePercent(subs)
        assertEquals(25.0, churn, 0.01)

        val qr = Analytics.quickRatio(subs)
        assertNotNull(qr)
        assertEquals(3.0, qr!!, 0.01)
    }

    @Test fun calculatesRefundRateAndMerchantHealth() {
        val sales = listOf(
            sale("1", 9000, status = "paid"),
            sale("2", 1000, status = "refunded", refund = 1000)
        )
        val refundRate = Analytics.refundRatePercent(sales)
        // 1000 refunded / 10000 gross = 10.0%
        assertEquals(10.0, refundRate, 0.01)
    }

    @Test fun calculatesAovAndArpu() {
        val sales = listOf(
            sale("1", 3000, status = "paid"),
            sale("2", 7000, status = "paid")
        )
        val aov = Analytics.averageOrderValueUsdCents(sales)
        assertEquals(5000L, aov)

        val subs = listOf(
            Subscription("1", "1", "SaaS", "", "active", "", "", false, 2000L, billingInterval = "month"),
            Subscription("2", "1", "SaaS", "", "active", "", "", false, 4000L, billingInterval = "month")
        )
        val arpu = Analytics.arpuUsdCents(subs)
        assertEquals(3000L, arpu)
    }

    @Test fun calculatesLemonSqueezyFeesAccurately() {
        // $100 sale = 10000 cents
        // Standard: 5% of $100 ($5.00) + $0.50 = $5.50 (550 cents). Net = $94.50 (9450 cents)
        val stdFee = Analytics.lemonSqueezyFees(10000L, isInternationalOrPayPal = false)
        assertEquals(550L, stdFee.feeCents)
        assertEquals(9450L, stdFee.netCents)
        assertEquals(5.5, stdFee.feePercentage, 0.01)

        // International (+1.5%): 6.5% of $100 ($6.50) + $0.50 = $7.00 (700 cents). Net = $93.00 (9300 cents)
        val intFee = Analytics.lemonSqueezyFees(10000L, isInternationalOrPayPal = true)
        assertEquals(700L, intFee.feeCents)
        assertEquals(9300L, intFee.netCents)
        assertEquals(7.0, intFee.feePercentage, 0.01)
    }

    @Test fun generatesValidCsvExport() {
        val sales = listOf(
            sale("ord_123", 4900, status = "paid").copy(product = "Licence Desktop \"Pro\"")
        )
        val csv = Analytics.generateCsvExport(sales)
        assertTrue(csv.contains("Date,ID,Produit,Source,Statut,Total client USD"))
        assertTrue(csv.contains("TVA taxes USD,Taux TVA,TVA incluse,Frais estimes USD"))
        assertTrue(csv.contains("ord_123"))
        assertTrue(csv.contains("\"Licence Desktop \"\"Pro\"\"\""))
        assertTrue(csv.contains("49.00"))
    }
}

