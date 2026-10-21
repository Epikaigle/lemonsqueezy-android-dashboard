@file:Suppress("DEPRECATION")
package com.epikaigle.lemondashboard

import android.Manifest
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


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
    val undated = remember(sales) { Analytics.undatedRefundUsdCents(sales) }
    var selectedSaleForDetail by remember { mutableStateOf<Sale?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Store & Period Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StoreChips(snapshot, s.selectedStore, vm::store)
                PeriodChips(s.period, vm::period)
            }
        }

        // Hero Card: Net Revenue
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
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
                        .padding(22.dp)
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

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = Analytics.money(m.netUsdCents, s.currency, snapshot.eurPerUsd),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = if (s.currency == "EUR")
                                "Conversion indicative en EUR au taux de change disponible"
                            else "Total des paiements retenus en USD",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // KPI 3-Row Grid
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Paiements",
                    value = m.payments.toString(),
                    icon = Icons.Default.Payments,
                    iconTint = MintSuccessLight,
                    subtitle = "Commandes validées",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Abos actifs",
                    value = m.activeSubscriptions.toString(),
                    icon = Icons.Default.Autorenew,
                    iconTint = Lemon,
                    subtitle = "Clients récurrents",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Panier moyen (AOV)",
                    value = Analytics.money(aov, s.currency, snapshot.eurPerUsd),
                    icon = Icons.Default.ShoppingCart,
                    iconTint = MintSuccessLight,
                    subtitle = "Net moyen / vente",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Revenu / Abo (ARPU)",
                    value = Analytics.money(arpu, s.currency, snapshot.eurPerUsd),
                    icon = Icons.Default.AccountCircle,
                    iconTint = Lemon,
                    subtitle = "Moyenne mensuelle",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            MerchantHealthCard(refundRate)
        }

        if (undated > 0L) {
            item {
                ExplanationBanner(
                    "Remboursements partiels sans date : " +
                        Analytics.money(undated, s.currency, snapshot.eurPerUsd) +
                        ". " +
                        if (s.period == Period.ALL) "Inclus dans le total historique (catégorie « Sans date »)."
                        else "Non attribués à cette période ; choisir « Tout » pour les inclure."
                )
            }
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
                Spacer(Modifier.height(10.dp))
                if (sales.isEmpty()) {
                    DashboardCard {
                        Text("Pas encore de ventes pour cette boutique.", color = TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    sales.take(5).forEach { sale ->
                        SaleItemCard(
                            sale = sale,
                            snapshot = snapshot,
                            currency = s.currency,
                            onClick = { selectedSaleForDetail = sale }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        item {
            ExplanationBanner(
                "Solde de période : ventes datées moins remboursements entièrement datés. " +
                    "Les remboursements partiels sans date ne sont pas imputés arbitrairement. Ce ne sont ni des bénéfices comptables ni des virements bancaires confirmés."
            )
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

