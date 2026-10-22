@file:Suppress("DEPRECATION")
package com.epikaigle.lemondashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun OverviewScreen(
    snapshot: Snapshot,
    sales: List<Sale>,
    subs: List<Subscription>,
    s: UiState,
    vm: DashboardViewModel
) {
    val now = remember { LocalDate.now() }
    val m = remember(sales, subs, s.period) { Analytics.metrics(sales, subs, s.period, now) }
    val history = remember(sales, s.period) { Analytics.graphBuckets(sales, s.period, now) }
    val prevNet = remember(sales, s.period) { Analytics.previousPeriodNet(sales, s.period, now) }
    val refundRate = remember(sales) { Analytics.refundRatePercent(sales) }
    val aov = remember(sales) { Analytics.averageOrderValueUsdCents(sales) }
    val mrr = remember(subs) { Analytics.mrrEstimateUsdCents(subs) }
    val activeSubsCount = remember(subs) { subs.count { it.status in setOf("active", "on_trial") } }
    val arpu = remember(mrr, activeSubsCount) { Analytics.arpuUsdCents(mrr, activeSubsCount) }
    val productLeaderboard = remember(sales, s.period) {
        val inPeriodSales = sales.filter { Analytics.inPeriod(Analytics.dateOf(it.createdAt), s.period, now) }
        Analytics.topProducts(inPeriodSales, 5)
    }
    var selectedSaleForDetail by remember { mutableStateOf<Sale?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Store & Period Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StoreChips(snapshot, s.selectedStore, vm::store)
                PeriodChips(s.period, vm::period)
            }
        }

        // Hero Card: Net Revenue
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F3229), Color(0xFF142426), DarkSurface)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CHIFFRE D'AFFAIRES NET",
                                color = MintSuccessLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            if (prevNet != null) {
                                val diff = m.netUsdCents - prevNet
                                val rate = if (prevNet != 0L) (diff * 100.0) / prevNet else null
                                TrendBadge(rate)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = Analytics.money(m.netUsdCents, s.currency, snapshot.eurPerUsd),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // KPI 3-Row Grid
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = "Paiements",
                    value = m.payments.toString(),
                    icon = Icons.Default.Payments,
                    iconTint = MintSuccessLight,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Abos actifs",
                    value = m.activeSubscriptions.toString(),
                    icon = Icons.Default.Autorenew,
                    iconTint = Lemon,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = "Ventes brutes",
                    value = Analytics.money(m.grossUsdCents, s.currency, snapshot.eurPerUsd),
                    icon = Icons.Default.TrendingUp,
                    iconTint = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Remboursements",
                    value = Analytics.money(m.refundedUsdCents, s.currency, snapshot.eurPerUsd),
                    icon = Icons.Default.AssignmentReturn,
                    iconTint = CoralDangerLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = "Panier moyen",
                    value = Analytics.money(aov, s.currency, snapshot.eurPerUsd),
                    icon = Icons.Default.ShoppingCart,
                    iconTint = MintSuccessLight,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Revenu / Abo",
                    value = Analytics.money(arpu, s.currency, snapshot.eurPerUsd),
                    icon = Icons.Default.AccountCircle,
                    iconTint = Lemon,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            MerchantHealthCard(refundRate)
        }

        // Interactive Revenue Chart
        item {
            InteractiveChartCard(
                buckets = history,
                currency = s.currency,
                rate = snapshot.eurPerUsd,
                periodTitle = s.period.title
            )
        }

        // Top Software Products Leaderboard
        if (productLeaderboard.isNotEmpty()) {
            item {
                TopProductsCard(productLeaderboard, s.currency, snapshot.eurPerUsd)
            }
        }

        // Recent Transactions Preview
        item {
            Column {
                SectionHeader(
                    title = "Dernières transactions",
                    subtitle = "${sales.size} au total",
                    action = {
                        TextButton(onClick = { vm.tab(1) }) {
                            Text("Voir tout", color = Lemon, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Lemon, modifier = Modifier.size(16.dp))
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                if (sales.isEmpty()) {
                    DashboardCard {
                        Text("Pas encore de ventes.", color = TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    sales.take(5).forEach { sale ->
                        SaleItemCard(
                            sale = sale,
                            snapshot = snapshot,
                            currency = s.currency,
                            onClick = { selectedSaleForDetail = sale }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }

    selectedSaleForDetail?.let { sale ->
        TransactionDetailSheet(
            sale = sale,
            snapshot = snapshot,
            currency = s.currency,
            onDismiss = { selectedSaleForDetail = null }
        )
    }
}
