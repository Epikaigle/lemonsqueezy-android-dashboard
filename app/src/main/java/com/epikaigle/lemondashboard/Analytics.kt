package com.epikaigle.lemondashboard

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Currency
import java.util.Locale

enum class Period(val title: String) {
    DAY("Jour"), WEEK("7 jours"), MONTH("Mois"), YEAR("Année"), ALL("Tout")
}

data class Metrics(
    val grossUsdCents: Long,
    val refundedUsdCents: Long,
    val netUsdCents: Long,
    val payments: Int,
    val activeSubscriptions: Int
)

object Analytics {
    fun dateOf(iso: String): LocalDate? = try {
        Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate()
    } catch (_: Exception) {
        try { LocalDate.parse(iso.take(10)) } catch (_: Exception) { null }
    }

    fun selectedSales(snapshot: Snapshot, storeId: String): List<Sale> =
        snapshot.sales.filter { (storeId.isEmpty() || it.storeId == storeId) && !it.testMode }

    fun selectedSubscriptions(snapshot: Snapshot, storeId: String): List<Subscription> =
        snapshot.subscriptions.filter { (storeId.isEmpty() || it.storeId == storeId) && !it.testMode }

    fun selectedLicenseKeys(snapshot: Snapshot, storeId: String): List<LicenseKey> =
        snapshot.licenseKeys.filter { storeId.isEmpty() || it.storeId == storeId }

    fun inPeriod(date: LocalDate?, period: Period, today: LocalDate = LocalDate.now()): Boolean {
        if (date == null || date.isAfter(today)) return false
        return when (period) {
            Period.DAY -> date == today
            Period.WEEK -> !date.isBefore(today.minusDays(6))
            Period.MONTH -> YearMonth.from(date) == YearMonth.from(today)
            Period.YEAR -> date.year == today.year
            Period.ALL -> true
        }
    }

    /** Full refunds have refunded_at; partial refunds often lack an event date.
     * Never invent a refund date or backdate a partial refund to the sale day.
     */
    fun undatedRefundUsdCents(sales: List<Sale>): Long =
        sales.asSequence().filter { it.paid && it.refundedAt.isBlank() }
            .sumOf { it.usdRefundCents.coerceIn(0L, it.usdCents.coerceAtLeast(0L)) }

    private fun refund(sale: Sale): Long =
        sale.usdRefundCents.coerceIn(0L, sale.usdCents.coerceAtLeast(0L))

    fun metrics(sales: List<Sale>, subscriptions: List<Subscription>, period: Period, today: LocalDate = LocalDate.now()): Metrics {
        val paid = sales.filter { it.paid && inPeriod(dateOf(it.createdAt), period, today) }
        val gross = paid.sumOf { it.usdCents }
        val refunds = sales.asSequence().filter { it.paid && refund(it) > 0 }
            .filter { period == Period.ALL || inPeriod(dateOf(it.refundedAt), period, today) }
            .sumOf { refund(it) }
        return Metrics(gross, refunds, gross - refunds, paid.size,
            subscriptions.count { it.status == "active" })
    }

    private fun between(date: LocalDate?, start: LocalDate, end: LocalDate): Boolean =
        date != null && !date.isBefore(start) && !date.isAfter(end)

    fun previousPeriodNet(sales: List<Sale>, period: Period, today: LocalDate = LocalDate.now()): Long? {
        val start: LocalDate
        val end: LocalDate
        when (period) {
            Period.DAY -> return null
            Period.WEEK -> { start = today.minusDays(13); end = today.minusDays(7) }
            Period.MONTH -> {
                start = today.minusMonths(1).withDayOfMonth(1)
                end = start.withDayOfMonth(today.dayOfMonth.coerceAtMost(start.lengthOfMonth()))
            }
            Period.YEAR -> {
                start = today.minusYears(1).withDayOfYear(1)
                end = today.minusYears(1)
            }
            Period.ALL -> return null
        }
        var total = 0L
        for (sale in sales) {
            if (!sale.paid) continue
            if (between(dateOf(sale.createdAt), start, end)) total += sale.usdCents
            if (between(dateOf(sale.refundedAt), start, end)) total -= refund(sale)
        }
        return total
    }

    /** One linear pass; a separate undated bucket reconciles lifetime partial refunds. */
    fun graphBuckets(sales: List<Sale>, period: Period, today: LocalDate = LocalDate.now()): List<Pair<String, Long>> {
        val hours = LongArray(24)
        val grouped = HashMap<String, Long>()
        var firstYear = today.year
        var undated = 0L
        fun record(iso: String, amount: Long) {
            if (amount == 0L) return
            val day = dateOf(iso) ?: return
            if (!inPeriod(day, period, today)) return
            if (period == Period.DAY) {
                val hour = runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).hour }.getOrNull() ?: return
                hours[hour] += amount
                return
            }
            val key = when (period) {
                Period.WEEK, Period.MONTH -> day.toString()
                Period.YEAR -> YearMonth.from(day).toString()
                Period.ALL -> day.year.toString()
                Period.DAY -> error("already handled")
            }
            grouped[key] = (grouped[key] ?: 0L) + amount
            if (day.year < firstYear) firstYear = day.year
        }
        sales.forEach { sale ->
            if (!sale.paid) return@forEach
            record(sale.createdAt, sale.usdCents)
            val amount = refund(sale)
            if (amount > 0) {
                if (sale.refundedAt.isNotBlank()) record(sale.refundedAt, -amount)
                else undated += amount
            }
        }
        if (period == Period.DAY) return (0..23).map { "${it}h" to hours[it] }
        val days = when (period) {
            Period.WEEK -> (6 downTo 0).map { today.minusDays(it.toLong()) }
            Period.MONTH -> (1..today.lengthOfMonth()).map { today.withDayOfMonth(it) }
            Period.YEAR -> (1..12).map { today.withMonth(it).withDayOfMonth(1) }
            Period.ALL -> (firstYear..today.year).map { LocalDate.of(it, 1, 1) }
            Period.DAY -> error("already handled")
        }
        val buckets = days.map { day ->
            val key = when (period) {
                Period.WEEK, Period.MONTH -> day.toString()
                Period.YEAR -> YearMonth.from(day).toString()
                Period.ALL -> day.year.toString()
                Period.DAY -> error("already handled")
            }
            val label = when (period) {
                Period.WEEK -> "${day.dayOfMonth}/${day.monthValue}"
                Period.MONTH -> day.dayOfMonth.toString()
                Period.YEAR -> day.monthValue.toString()
                Period.ALL -> day.year.toString()
                Period.DAY -> error("already handled")
            }
            label to (grouped[key] ?: 0L)
        }
        return if (period == Period.ALL && undated != 0L) buckets + ("Sans date" to -undated) else buckets
    }

    fun money(usdCents: Long, currency: String, eurPerUsd: Double?): String {
        val target = if (currency == "EUR" && eurPerUsd != null && eurPerUsd > 0) "EUR" else "USD"
        val amount = BigDecimal.valueOf(usdCents, 2)
            .multiply(if (target == "EUR") BigDecimal.valueOf(eurPerUsd!!) else BigDecimal.ONE)
            .setScale(2, RoundingMode.HALF_UP)
        return NumberFormat.getCurrencyInstance(Locale.FRANCE).apply {
            this.currency = Currency.getInstance(target)
            maximumFractionDigits = 2
        }.format(amount)
    }

    fun localMoney(cents: Long, currency: String): String = try {
        NumberFormat.getCurrencyInstance(Locale.FRANCE).apply {
            this.currency = Currency.getInstance(currency.uppercase(Locale.ROOT))
        }.format(BigDecimal.valueOf(cents, 2))
    } catch (_: Exception) {
        "${BigDecimal.valueOf(cents, 2)} ${currency.uppercase(Locale.ROOT)}"
    }

    data class PayoutEstimates(
        val readyUsdCents: Long,
        val holdingUsdCents: Long,
        val nextPayoutDate: LocalDate,
        val daysUntilNextPayout: Long
    )

    fun payoutEstimates(sales: List<Sale>, today: LocalDate = LocalDate.now()): PayoutEstimates {
        val nextDate = when {
            today.dayOfMonth <= 14 -> today.withDayOfMonth(14)
            today.dayOfMonth <= 28 -> today.withDayOfMonth(28)
            else -> today.plusMonths(1).withDayOfMonth(14)
        }
        val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, nextDate).coerceAtLeast(0)
        val cutoff = today.minusDays(13)

        var ready = 0L
        var holding = 0L
        for (sale in sales) {
            if (!sale.paid) continue
            val date = dateOf(sale.createdAt) ?: continue
            val net = sale.netUsd
            if (date.isAfter(cutoff)) {
                holding += net
            } else {
                ready += net
            }
        }
        return PayoutEstimates(
            readyUsdCents = ready,
            holdingUsdCents = holding,
            nextPayoutDate = nextDate,
            daysUntilNextPayout = daysUntil
        )
    }

    fun mrrEstimateUsdCents(subscriptions: List<Subscription>): Long {
        return subscriptions.asSequence()
            .filter { it.status in setOf("active", "on_trial") }
            .map { sub ->
                val amount = sub.catalogPriceCents ?: sub.lastInvoiceUsdCents ?: 0L
                when (sub.billingInterval) {
                    "year" -> amount / 12L
                    "month" -> amount / sub.billingIntervalCount.coerceAtLeast(1)
                    "week" -> (amount * 52L) / 12L
                    "day" -> amount * 30L
                    else -> amount
                }
            }
            .sum()
    }

    fun arrEstimateUsdCents(subscriptions: List<Subscription>): Long =
        mrrEstimateUsdCents(subscriptions) * 12L

    data class ProductMetric(
        val name: String,
        val salesCount: Int,
        val netUsdCents: Long,
        val grossUsdCents: Long
    )

    fun topProducts(sales: List<Sale>, limit: Int = 5): List<ProductMetric> {
        return sales.filter { it.paid }
            .groupBy { it.product.ifBlank { "Logiciel / Produit" } }
            .map { (name, items) ->
                ProductMetric(
                    name = name,
                    salesCount = items.size,
                    netUsdCents = items.sumOf { it.netUsd },
                    grossUsdCents = items.sumOf { it.usdCents }
                )
            }
            .sortedByDescending { it.netUsdCents }
            .take(limit)
    }

    fun churnRatePercent(subscriptions: List<Subscription>): Double {
        val active = subscriptions.count { it.status in setOf("active", "on_trial") }
        val cancelled = subscriptions.count { it.status in setOf("cancelled", "expired") }
        val total = active + cancelled
        return if (total == 0) 0.0 else (cancelled.toDouble() / total.toDouble()) * 100.0
    }

    fun quickRatio(subscriptions: List<Subscription>): Double {
        val active = subscriptions.count { it.status in setOf("active", "on_trial") }
        val cancelled = subscriptions.count { it.status in setOf("cancelled", "expired") }
        return if (cancelled == 0) (if (active > 0) active.toDouble() else 1.0) else (active.toDouble() / cancelled.toDouble())
    }

    fun refundRatePercent(sales: List<Sale>): Double {
        val paid = sales.filter { it.paid }
        if (paid.isEmpty()) return 0.0
        val gross = paid.sumOf { it.usdCents }
        val refund = paid.sumOf { it.usdRefundCents }
        return if (gross <= 0L) 0.0 else (refund.toDouble() / gross.toDouble()) * 100.0
    }

    fun averageOrderValueUsdCents(sales: List<Sale>): Long {
        val paid = sales.filter { it.paid }
        return if (paid.isEmpty()) 0L else paid.sumOf { it.netUsd } / paid.size
    }

    fun arpuUsdCents(mrrUsdCents: Long, activeSubsCount: Int): Long {
        return if (activeSubsCount <= 0) 0L else mrrUsdCents / activeSubsCount
    }

    fun arpuUsdCents(subscriptions: List<Subscription>): Long {
        val active = subscriptions.count { it.status in setOf("active", "on_trial") }
        return arpuUsdCents(mrrEstimateUsdCents(subscriptions), active)
    }

    fun billingIntervalBreakdown(subscriptions: List<Subscription>): Map<String, Int> {
        val active = subscriptions.filter { it.status in setOf("active", "on_trial") }
        val counts = mutableMapOf("Mensuel" to 0, "Annuel" to 0, "Autre" to 0)
        for (sub in active) {
            when (sub.billingInterval) {
                "month" -> counts["Mensuel"] = (counts["Mensuel"] ?: 0) + 1
                "year" -> counts["Annuel"] = (counts["Annuel"] ?: 0) + 1
                else -> counts["Autre"] = (counts["Autre"] ?: 0) + 1
            }
        }
        return counts
    }

    data class UpcomingRenewal(
        val subscription: Subscription,
        val renewalDate: LocalDate,
        val daysRemaining: Long,
        val estimatedUsdCents: Long
    )

    fun upcomingRenewals(
        subscriptions: List<Subscription>,
        daysAhead: Long = 30,
        today: LocalDate = LocalDate.now()
    ): List<UpcomingRenewal> {
        val limit = today.plusDays(daysAhead)
        return subscriptions.filter { it.status == "active" && it.renewsAt.isNotBlank() }
            .mapNotNull { sub ->
                val date = dateOf(sub.renewsAt) ?: return@mapNotNull null
                if (date.isBefore(today) || date.isAfter(limit)) return@mapNotNull null
                val days = java.time.temporal.ChronoUnit.DAYS.between(today, date).coerceAtLeast(0)
                val amount = sub.catalogPriceCents ?: sub.lastInvoiceUsdCents ?: 0L
                UpcomingRenewal(sub, date, days, amount)
            }
            .sortedBy { it.daysRemaining }
    }

    /**
     * Indicative platform fee only: JSON:API does not disclose actual fee amounts.
     * Geography, PayPal, affiliate, custom plan and payout fees are not inferred.
     */
    fun estimatedPlatformFeeUsdCents(sale: Sale): Long? {
        if (!sale.paid || sale.usdCents <= 0L) return null
        val percent = if (sale.source == "Renouvellement" || sale.source == "Ajustement") 0.055 else 0.05
        return (50L + kotlin.math.round(sale.usdCents * percent).toLong())
            .coerceAtMost(sale.usdCents)
    }

    // Refund-related tax adjustments are unavailable; don't fabricate payout net.
    fun estimatedAfterTaxesAndFeesUsdCents(sale: Sale): Long? {
        if (!sale.paid || sale.usdRefundCents > 0L) return null
        val tax = sale.taxUsdCents ?: return null
        val fee = estimatedPlatformFeeUsdCents(sale) ?: return null
        return (sale.usdCents - tax - fee).coerceAtLeast(0L)
    }

    data class FeeBreakdown(
        val grossCents: Long,
        val feeCents: Long,
        val netCents: Long,
        val feePercentage: Double
    )

    fun lemonSqueezyFees(amountCents: Long, isInternationalOrPayPal: Boolean = false): FeeBreakdown {
        if (amountCents <= 0L) return FeeBreakdown(0L, 0L, 0L, 0.0)
        val ratePercent = if (isInternationalOrPayPal) 6.5 else 5.0
        val percentageFee = (amountCents.toDouble() * ratePercent / 100.0).toLong()
        val fixedFee = 50L
        val totalFee = (percentageFee + fixedFee).coerceAtMost(amountCents)
        val net = (amountCents - totalFee).coerceAtLeast(0L)
        val effectivePercent = (totalFee.toDouble() / amountCents.toDouble()) * 100.0
        return FeeBreakdown(amountCents, totalFee, net, effectivePercent)
    }

    fun generateCsvExport(sales: List<Sale>, currency: String = "USD", eurPerUsd: Double? = null): String {
        val sb = StringBuilder()
        sb.append("Date,ID,Produit,Source,Statut,Total client USD,Remboursement USD,Apres remboursement USD,Devise,Mode Test,Sous-total USD,Remise USD,TVA taxes USD,Taux TVA,TVA incluse,Frais estimes USD,Revenu estime USD\n")
        sales.forEach { s ->
            val date = s.createdAt.take(19).replace('T', ' ')
            val productEscaped = "\"" + s.product.replace("\"", "\"\"") + "\""
            val source = s.source
            val status = s.status
            val gross = String.format(java.util.Locale.US, "%.2f", s.usdCents / 100.0)
            val refund = String.format(java.util.Locale.US, "%.2f", s.usdRefundCents / 100.0)
            val net = String.format(java.util.Locale.US, "%.2f", s.netUsd / 100.0)
            val curr = s.currency
            val test = if (s.testMode) "OUI" else "NON"
            fun amount(value: Long?): String = value?.let {
                String.format(java.util.Locale.US, "%.2f", it / 100.0)
            } ?: ""
            val rate = s.taxRate?.toString().orEmpty()
            val inclusive = s.taxInclusive?.toString().orEmpty()
            sb.append(listOf(date, s.id, productEscaped, source, status, gross, refund, net, curr, test,
                amount(s.subtotalUsdCents), amount(s.discountUsdCents), amount(s.taxUsdCents),
                rate, inclusive, amount(estimatedPlatformFeeUsdCents(s)),
                amount(estimatedAfterTaxesAndFeesUsdCents(s))
            ).joinToString(","))
            sb.append("\n")
        }
        return sb.toString()
    }
}

