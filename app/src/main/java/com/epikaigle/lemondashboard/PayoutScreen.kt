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
fun PayoutScreen(snapshot: Snapshot?, currency: String) {
    val uri = LocalUriHandler.current
    val today = remember { LocalDate.now() }
    val sales = snapshot?.sales.orEmpty()
    val payoutEst = remember(sales) { Analytics.payoutEstimates(sales, today) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Versements", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Text("Calendrier officiel Lemon Squeezy, délais de rétention et estimations.", color = TextSecondary, fontSize = 13.sp)

        // Next Payout Countdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF1B2F2A), DarkSurface)))
                    .padding(20.dp)
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
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = payoutEst.nextPayoutDate.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRANCE))
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Lemon Squeezy déclenche les virements deux fois par mois (les 14 et 28).",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Funds Breakdown: Ready vs In Holding
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Ventes prêtes",
                value = Analytics.money(payoutEst.readyUsdCents, currency, snapshot?.eurPerUsd),
                icon = Icons.Default.CheckCircle,
                iconTint = MintSuccessLight,
                subtitle = "> 13 jours d'ancienneté",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "En rétention",
                value = Analytics.money(payoutEst.holdingUsdCents, currency, snapshot?.eurPerUsd),
                icon = Icons.Default.LockClock,
                iconTint = AmberWarningLight,
                subtitle = "Période de sécurité 13j",
                modifier = Modifier.weight(1f)
            )
        }

        // Payout Lifecycle Steps Timeline
        DashboardCard {
            Text("Cycle de vie d'un versement Lemon Squeezy", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(14.dp))

            TimelineStep(
                step = 1,
                title = "Paiement client",
                description = "La vente est confirmée et créditée sur votre compte marchand.",
                isDone = true
            )
            TimelineStep(
                step = 2,
                title = "Période de retenue de 13 jours",
                description = "Chaque commande est sécurisée pendant 13 jours pour couvrir les contestations et remboursements éventuels.",
                isDone = true
            )
            TimelineStep(
                step = 3,
                title = "Calcul du cycle (1er & 15 du mois)",
                description = "Lemon Squeezy consolide toutes les ventes ayant dépassé les 13 jours si le solde atteint 50 $ minimum.",
                isDone = true
            )
            TimelineStep(
                step = 4,
                title = "Ordre de virement (14 & 28 du mois)",
                description = "Le virement bancaire est envoyé vers votre compte (délai bancaire de 1 à 5 jours ouvrés).",
                isDone = false
            )
        }

        // Official External Link Card
        DashboardCard(backgroundColor = DarkSurfaceVariant) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Lemon, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Versements officiels", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Consultez l'historique bancaire exact sur le portail Lemon Squeezy.", color = TextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { uri.openUri("https://app.lemonsqueezy.com/") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground)
            ) {
                Text("Ouvrir le portail Lemon Squeezy ↗", fontWeight = FontWeight.Bold)
            }
        }

        ExplanationBanner("Les montants affichés sont une estimation indicative calculée à partir des commandes brutes et des remboursements. Les frais Lemon Squeezy, taxes retenues à la source et réserves de risques peuvent modifier le montant final viré.")
    }
}

@Composable
fun TimelineStep(step: Int, title: String, description: String, isDone: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isDone) MintSuccessContainer else DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDone) MintSuccessLight else TextMuted
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(text = description, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

