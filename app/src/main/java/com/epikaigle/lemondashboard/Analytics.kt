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

    fun metrics(sales: List<Sale>, subscriptions: List<Subscription>, period: Period, today: LocalDate = LocalDate.now()): Metrics {
        val paid = sales.filter { it.paid && inPeriod(dateOf(it.createdAt), period, today) }
        val gross = paid.sumOf { it.usdCents }
        val refund = paid.sumOf { it.usdRefundCents.coerceAtMost(it.usdCents) }
        return Metrics(
            grossUsdCents = gross,
            refundedUsdCents = refund,
            netUsdCents = gross - refund,
            payments = paid.size,
            activeSubscriptions = subscriptions.count { it.status == "active" }
        )
    }

    /**
     * Compare elapsed portions of periods, never a partial current month against a
     * full prior month. Null means there is no sensible comparison ("Tout").
     */
    fun previousPeriodNet(sales: List<Sale>, period: Period, today: LocalDate = LocalDate.now()): Long? {
        val start: LocalDate
        val end: LocalDate
        when (period) {
            Period.DAY -> return null
            Period.WEEK -> {
                start = today.minusDays(13)
                end = today.minusDays(7)
            }
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
        return sales.asSequence().filter { it.paid }
            .filter { sale -> dateOf(sale.createdAt)?.let { !it.isBefore(start) && !it.isAfter(end) } == true }
            .sumOf { it.netUsd }
    }

    /**
     * Linear-time aggregation for long histories: 100k+ transactions must not require
     * a separate full scan for each graph bucket.
     */
    fun graphBuckets(sales: List<Sale>, period: Period, today: LocalDate = LocalDate.now()): List<Pair<String, Long>> {
        if (period == Period.DAY) {
            val hours = LongArray(24)
            sales.forEach { sale ->
                if (!sale.paid) return@forEach
                val instant = runCatching { Instant.parse(sale.createdAt) }.getOrNull() ?: return@forEach
                val local = instant.atZone(ZoneId.systemDefault())
                if (local.toLocalDate() == today) hours[local.hour] += sale.netUsd
            }
            return (0..23).map { "${it}h" to hours[it] }
        }
        val grouped = HashMap<String, Long>()
        var firstYear = today.year
        sales.forEach { sale ->
            if (!sale.paid) return@forEach
            val date = dateOf(sale.createdAt) ?: return@forEach
            if (!inPeriod(date, period, today)) return@forEach
            val key = when (period) {
                Period.DAY -> error("already handled")
                Period.WEEK, Period.MONTH -> date.toString()
                Period.YEAR -> YearMonth.from(date).toString()
                Period.ALL -> date.year.toString()
            }
            grouped[key] = (grouped[key] ?: 0L) + sale.netUsd
            if (date.year < firstYear) firstYear = date.year
        }
        val buckets = when (period) {
            Period.DAY -> error("already handled")
            Period.WEEK -> (6 downTo 0).map { today.minusDays(it.toLong()) }
            Period.MONTH -> (1..today.lengthOfMonth()).map { today.withDayOfMonth(it) }
            Period.YEAR -> (1..12).map { today.withMonth(it).withDayOfMonth(1) }
            Period.ALL -> (firstYear..today.year).map { LocalDate.of(it, 1, 1) }
        }
        return buckets.map { day ->
            val key = when (period) {
                Period.DAY -> error("already handled")
                Period.WEEK, Period.MONTH -> day.toString()
                Period.YEAR -> YearMonth.from(day).toString()
                Period.ALL -> day.year.toString()
            }
            val label = when (period) {
                Period.DAY -> error("already handled")
                Period.WEEK -> "${day.dayOfMonth}/${day.monthValue}"
                Period.MONTH -> day.dayOfMonth.toString()
                Period.YEAR -> day.monthValue.toString()
                Period.ALL -> day.year.toString()
            }
            label to (grouped[key] ?: 0L)
        }
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
}
