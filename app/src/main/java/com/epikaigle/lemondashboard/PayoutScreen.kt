@file:Suppress("DEPRECATION")
package com.epikaigle.lemondashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PayoutScreen(
    snapshot: Snapshot,
    sales: List<Sale>,
    s: UiState,
    vm: DashboardViewModel,
    onOpenGlossary: () -> Unit = {}
) {
    val uri = LocalUriHandler.current
    val today = remember { LocalDate.now() }
    val payoutEst = remember(sales, today) { Analytics.payoutEstimates(sales, today) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Versements", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.clickable { onOpenGlossary() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Lemon, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cycle MoR (13j, 50\u00A0$)", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium, softWrap = false)
                }
            }
        }

        StoreChips(snapshot, s.selectedStore, vm::store)

        // Hero Card: Prochain virement estimé
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF1B2F2A), DarkSurface)))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROCHAIN VIREMENT ESTIMÉ",
                            color = MintSuccessLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Surface(
                            color = MintSuccessContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text(
                                text = daysUntilLabel(payoutEst.daysUntilNextPayout),
                                color = MintSuccessLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                softWrap = false,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = Analytics.moneyRange(payoutEst.pocketNextPayoutUsdCents, s.currency, snapshot.eurPerUsd),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Net en poche après TVA, frais MoR et virement (1\u00A0%)",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = payoutEst.nextPayoutDate.format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy", Locale.FRANCE))
                                .replaceFirstChar { it.uppercase() },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )

                        Surface(
                            color = if (payoutEst.reachesThreshold) MintSuccessContainer.copy(alpha = 0.4f) else AmberWarningContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.clickable { onOpenGlossary() }
                        ) {
                            Text(
                                text = if (payoutEst.reachesThreshold) "Seuil atteint (≥ 50\u00A0$)" else "En attente seuil 50\u00A0$",
                                color = if (payoutEst.reachesThreshold) MintSuccessLight else AmberWarningLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                softWrap = false,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Funds Breakdown: Dernier virement reçu vs En réserve
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val lastPayout = payoutEst.pastPayouts.lastOrNull()
            val paidValue = when {
                lastPayout == null -> Analytics.money(0L, s.currency, snapshot.eurPerUsd)
                s.currency.equals("EUR", ignoreCase = true) && lastPayout.verifiedEurCents != null ->
                    Analytics.localMoney(lastPayout.verifiedEurCents, "EUR")
                else -> Analytics.money(lastPayout.amountUsdCents, s.currency, snapshot.eurPerUsd)
            }
            val paidSubtitle = when {
                lastPayout == null -> "Aucun virement émis"
                lastPayout.verifiedEurCents != null -> "Reçu le 29 oct."
                else -> "Cycle du ${friendlyDate(lastPayout.date.toString())}"
            }
            StatCard(
                title = "Dernier virement reçu",
                value = paidValue,
                icon = Icons.Default.AccountBalance,
                iconTint = MintSuccessLight,
                subtitle = paidSubtitle,
                modifier = Modifier.weight(1f)
            )
            val holdingSubtitle = if (payoutEst.holdingCount == 0) {
                "Aucune vente récente"
            } else {
                "${pluralFr(payoutEst.holdingCount, "vente")} en sécurité"
            }
            StatCard(
                title = "En réserve (13j)",
                value = Analytics.money(payoutEst.pocketHoldingUsdCents, s.currency, snapshot.eurPerUsd),
                icon = Icons.Default.LockClock,
                iconTint = AmberWarningLight,
                subtitle = holdingSubtitle,
                modifier = Modifier.weight(1f)
            )
        }

        // Past Payouts History (if any)
        if (payoutEst.pastPayouts.isNotEmpty()) {
            DashboardCard {
                Text("Virements bancaires effectués", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    payoutEst.pastPayouts.sortedByDescending { it.date }.forEach { payout ->
                        val amountText = if (payout.verifiedEurCents != null && s.currency.equals("EUR", ignoreCase = true)) {
                            Analytics.localMoney(payout.verifiedEurCents, "EUR")
                        } else {
                            Analytics.money(payout.amountUsdCents, s.currency, snapshot.eurPerUsd)
                        }
                        val subtitleText = if (payout.verifiedEurCents != null) {
                            "Reçu le 29 oct. sur votre compte bancaire"
                        } else {
                            "Transféré sur compte bancaire"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Surface(
                                    color = MintSuccessContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MintSuccessLight, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = payout.date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRANCE)),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = subtitleText,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = amountText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintSuccessLight,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Official External Link Card
        DashboardCard(backgroundColor = DarkSurfaceVariant) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Lemon, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Relevés officiels Lemon Squeezy", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                }
                Button(
                    onClick = { uri.openUri("https://app.lemonsqueezy.com/settings/payout") },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Ouvrir ↗", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
