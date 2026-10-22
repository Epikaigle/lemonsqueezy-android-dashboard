@file:Suppress("DEPRECATION")
package com.epikaigle.lemondashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PayoutScreen(snapshot: Snapshot?, currency: String) {
    val uri = LocalUriHandler.current
    val today = remember { LocalDate.now() }
    val sales = snapshot?.sales.orEmpty()
    val payoutEst = remember(sales) { Analytics.payoutEstimates(sales, today) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Versements", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        // Next Payout Countdown Card
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
                            letterSpacing = 1.sp
                        )
                        Surface(
                            color = MintSuccessContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text(
                                text = "Dans ${payoutEst.daysUntilNextPayout} jour(s)",
                                color = MintSuccessLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = payoutEst.nextPayoutDate.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRANCE))
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        // Funds Breakdown: Ready vs In Holding
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Ventes prêtes",
                value = Analytics.money(payoutEst.readyUsdCents, currency, snapshot?.eurPerUsd),
                icon = Icons.Default.CheckCircle,
                iconTint = MintSuccessLight,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "En rétention (13j)",
                value = Analytics.money(payoutEst.holdingUsdCents, currency, snapshot?.eurPerUsd),
                icon = Icons.Default.LockClock,
                iconTint = AmberWarningLight,
                modifier = Modifier.weight(1f)
            )
        }

        // Payout Lifecycle Steps Timeline
        DashboardCard {
            Text("Cycle des versements", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(10.dp))

            TimelineStep(
                step = 1,
                title = "Paiement client",
                description = "Vente confirmée et créditée sur le solde.",
                isDone = true
            )
            TimelineStep(
                step = 2,
                title = "Rétention de 13 jours",
                description = "Période de garantie anti-litiges et contestations.",
                isDone = true
            )
            TimelineStep(
                step = 3,
                title = "Clôture de cycle (1er & 15)",
                description = "Consolidation des ventes prêtes (minimum 50 $).",
                isDone = true
            )
            TimelineStep(
                step = 4,
                title = "Virement bancaire (14 & 28)",
                description = "Ordre de virement vers votre compte bancaire.",
                isDone = false
            )
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
                    Text("Portail Lemon Squeezy", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                }
                Button(
                    onClick = { uri.openUri("https://app.lemonsqueezy.com/") },
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

@Composable
fun TimelineStep(step: Int, title: String, description: String, isDone: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isDone) MintSuccessContainer else DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDone) MintSuccessLight else TextMuted
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
            Text(text = description, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
