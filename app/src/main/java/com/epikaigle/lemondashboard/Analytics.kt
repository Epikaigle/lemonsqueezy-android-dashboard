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

    fun selectedSales(snapshot: Snapshot, storeId: String, includeTest: Boolean): List<Sale> =
        snapshot.sales.filter { (storeId.isEmpty() || it.storeId == storeId) && (includeTest || !it.testMode) }

    fun selectedSubscriptions(snapshot: Snapshot, storeId: String, includeTest: Boolean): List<Subscription> =
        snapshot.subscriptions.filter { (storeId.isEmpty() || it.storeId == storeId) && (includeTest || !it.testMode) }

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
}

