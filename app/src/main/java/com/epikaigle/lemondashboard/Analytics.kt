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
    WEEK("7 jours"), MONTH("Mois"), YEAR("Année"), ALL("Tout")
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

    fun graphBuckets(sales: List<Sale>, period: Period, today: LocalDate = LocalDate.now()): List<Pair<String, Long>> {
        val paid = sales.filter { it.paid && inPeriod(dateOf(it.createdAt), period, today) }
        val periods = when (period) {
            Period.WEEK -> (6 downTo 0).map { today.minusDays(it.toLong()) }
            Period.MONTH -> (1..today.lengthOfMonth()).map { today.withDayOfMonth(it) }
            Period.YEAR -> (1..12).map { today.withMonth(it).withDayOfMonth(1) }
            Period.ALL -> {
                val years = paid.mapNotNull { dateOf(it.createdAt)?.year }
                val first = (years.minOrNull() ?: today.year).coerceAtMost(today.year)
                (first..today.year).map { LocalDate.of(it, 1, 1) }
            }
        }
        return periods.map { day ->
            val value = paid.filter { sale ->
                val d = dateOf(sale.createdAt)
                when (period) {
                    Period.WEEK, Period.MONTH -> d == day
                    Period.YEAR -> d?.year == day.year && d.month == day.month
                    Period.ALL -> d?.year == day.year
                }
            }.sumOf { it.netUsd }
            val label = when (period) {
                Period.WEEK -> "${day.dayOfMonth}/${day.monthValue}"
                Period.MONTH -> day.dayOfMonth.toString()
                Period.YEAR -> day.monthValue.toString()
                Period.ALL -> day.year.toString()
            }
            label to value
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
