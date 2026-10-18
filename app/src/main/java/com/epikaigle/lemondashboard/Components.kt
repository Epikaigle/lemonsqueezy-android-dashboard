@file:Suppress("DEPRECATION")
package com.epikaigle.lemondashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    backgroundColor: Color = DarkSurface,
    borderColor: Color = DarkBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            content = content
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color = Lemon,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TrendBadge(rate: Double?, label: String = "vs période précédente") {
    if (rate == null) return
    val isPositive = rate > 0
    val isZero = rate == 0.0
    val containerColor = when {
        isPositive -> MintSuccessContainer.copy(alpha = 0.4f)
        isZero -> DarkSurfaceVariant
        else -> CoralDangerContainer.copy(alpha = 0.4f)
    }
    val contentColor = when {
        isPositive -> MintSuccessLight
        isZero -> TextSecondary
        else -> CoralDangerLight
    }
    val icon = when {
        isPositive -> Icons.AutoMirrored.Filled.TrendingUp
        isZero -> Icons.AutoMirrored.Filled.TrendingFlat
        else -> Icons.AutoMirrored.Filled.TrendingDown
    }
    val text = when {
        isZero -> "Stable"
        else -> "${if (isPositive) "+" else ""}${"%.1f".format(Locale.FRANCE, rate)} %"
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
            Text(
                text = "$text $label",
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (label, tint, bg) = when (status) {
        "paid" -> Triple("Payé", MintSuccessLight, MintSuccessContainer.copy(alpha = 0.4f))
        "active" -> Triple("Actif", MintSuccessLight, MintSuccessContainer.copy(alpha = 0.4f))
        "on_trial" -> Triple("Essai", AmberWarningLight, AmberWarningContainer.copy(alpha = 0.4f))
        "cancelled" -> Triple("Annulé", CoralDangerLight, CoralDangerContainer.copy(alpha = 0.4f))
        "refunded" -> Triple("Remboursé", CoralDangerLight, CoralDangerContainer.copy(alpha = 0.4f))
        "partial_refund" -> Triple("Remb. partiel", AmberWarningLight, AmberWarningContainer.copy(alpha = 0.4f))
        "expired" -> Triple("Expiré", TextMuted, DarkSurfaceVariant)
        "past_due" -> Triple("En retard", CoralDangerLight, CoralDangerContainer.copy(alpha = 0.4f))
        "unpaid" -> Triple("Impayé", CoralDangerLight, CoralDangerContainer.copy(alpha = 0.4f))
        "paused" -> Triple("En pause", AmberWarningLight, AmberWarningContainer.copy(alpha = 0.4f))
        "pending" -> Triple("En attente", AmberWarningLight, AmberWarningContainer.copy(alpha = 0.4f))
        else -> Triple(status.replaceFirstChar { it.uppercase() }, TextSecondary, DarkSurfaceVariant)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun InteractiveChartCard(
    buckets: List<Pair<String, Long>>,
    currency: String,
    rate: Double?,
    periodTitle: String
) {
    var selectedIndex by remember(buckets) { mutableStateOf<Int?>(null) }
    val maxValue = (buckets.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(0L)
    val minValue = (buckets.minOfOrNull { it.second } ?: 0L).coerceAtMost(0L)
    val hasData = buckets.any { it.second != 0L }

    DashboardCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Évolution des revenus",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (hasData) "Touchez une barre pour inspecter le montant" else "Aucune transaction enregistrée",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            if (selectedIndex != null) {
                IconButton(
                    onClick = { selectedIndex = null },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Réinitialiser la sélection",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Selected Tooltip Bubble
        AnimatedVisibility(
            visible = selectedIndex != null && selectedIndex!! in buckets.indices,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val idx = selectedIndex
            if (idx != null && idx in buckets.indices) {
                val pair = buckets[idx]
                Surface(
                    color = DarkSurfaceContainerHigh,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Lemon.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Date : ${pair.first}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = Analytics.money(pair.second, currency, rate),
                            fontWeight = FontWeight.Bold,
                            color = if (pair.second >= 0) Lemon else CoralDangerLight,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        if (!hasData) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Pas de ventes sur cette période ($periodTitle)",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(buckets) {
                        detectTapGestures { offset ->
                            val count = buckets.size.coerceAtLeast(1)
                            val cellWidth = size.width / count
                            val clickedIndex = (offset.x / cellWidth).toInt().coerceIn(0, buckets.lastIndex)
                            selectedIndex = if (selectedIndex == clickedIndex) null else clickedIndex
                        }
                    }
                    .semantics {
                        contentDescription = "Graphique de revenus : ${buckets.size} périodes, de ${Analytics.money(minValue, currency, rate)} à ${Analytics.money(maxValue, currency, rate)}"
                    }
            ) {
                val count = buckets.size.coerceAtLeast(1)
                val cell = size.width / count
                val range = (maxValue.toDouble() - minValue.toDouble()).coerceAtLeast(1.0)
                val zeroY = (maxValue.toDouble() / range * size.height).toFloat()

                // Baseline 0
                drawLine(
                    color = DarkBorderHighlight,
                    start = Offset(0f, zeroY),
                    end = Offset(size.width, zeroY),
                    strokeWidth = 1.dp.toPx()
                )

                buckets.forEachIndexed { index, pair ->
                    val isSelected = selectedIndex == index
                    val delta = (pair.second.toDouble() / range * size.height).toFloat()
                    val top = if (delta >= 0f) zeroY - delta else zeroY
                    val barHeight = kotlin.math.abs(delta).coerceAtLeast(if (pair.second != 0L) 3.dp.toPx() else 0f)
                    val boundedTop = top.coerceIn(0f, size.height)

                    val barColor = when {
                        isSelected -> Lemon
                        pair.second < 0 -> CoralDanger
                        index == buckets.lastIndex -> MintSuccessLight
                        else -> MintSuccess.copy(alpha = 0.75f)
                    }

                    if (barHeight > 0f) {
                        val barWidth = cell * 0.62f
                        val barLeft = index * cell + cell * 0.19f

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(barLeft, boundedTop),
                            size = Size(barWidth, barHeight.coerceAtMost((size.height - boundedTop).coerceAtLeast(0f))),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )

                        // Highlight outline on selected bar
                        if (isSelected) {
                            drawRoundRect(
                                color = TextPrimary,
                                topLeft = Offset(barLeft - 1.dp.toPx(), (boundedTop - 2.dp.toPx()).coerceAtLeast(0f)),
                                size = Size(barWidth + 2.dp.toPx(), barHeight + 4.dp.toPx()),
                                cornerRadius = CornerRadius(5.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // X-Axis bounds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(buckets.firstOrNull()?.first.orEmpty(), color = TextMuted, fontSize = 11.sp)
                if (buckets.size > 2) {
                    Text(buckets.getOrNull(buckets.size / 2)?.first.orEmpty(), color = TextMuted, fontSize = 11.sp)
                }
                Text(buckets.lastOrNull()?.first.orEmpty(), color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    sale: Sale,
    snapshot: Snapshot,
    currency: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val originalNet = (sale.originalCents - sale.originalRefundCents).coerceAtLeast(0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = sale.product,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${sale.source} • ${friendlyDateTime(sale.createdAt)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                StatusBadge(if (sale.usdRefundCents > 0L && sale.paid) "partial_refund" else sale.status)
            }

            // Reference Pill with Copy Button
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("IDENTIFIANT DE TRANSACTION", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                        Text(sale.id, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(sale.id))
                            Toast.makeText(context, "Identifiant copié !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copier l'identifiant", tint = Lemon, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Breakdown Table Card
            DashboardCard(backgroundColor = DarkSurfaceVariant) {
                Text("Détail financier", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                DetailRow("Montant brut", Analytics.money(sale.usdCents, currency, snapshot.eurPerUsd))
                if (sale.usdRefundCents > 0L) {
                    DetailRow(
                        "Remboursement",
                        "- ${Analytics.money(sale.usdRefundCents, currency, snapshot.eurPerUsd)}",
                        valueColor = CoralDangerLight
                    )
                    if (sale.refundedAt.isNotBlank()) {
                        DetailRow("Date de remboursement", friendlyDateTime(sale.refundedAt), valueColor = TextMuted)
                    }
                }
                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 8.dp))
                DetailRow(
                    "Total net",
                    Analytics.money(sale.netUsd, currency, snapshot.eurPerUsd),
                    isBold = true,
                    valueColor = if (sale.paid) MintSuccessLight else TextMuted
                )
                DetailRow(
                    "Montant d'origine",
                    Analytics.localMoney(originalNet, sale.currency),
                    valueColor = TextSecondary
                )
            }

            if (sale.testMode) {
                Surface(
                    color = LemonContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Lemon.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = Lemon, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Transaction effectuée en MODE TEST", fontSize = 12.sp, color = Lemon, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceContainerHigh)
            ) {
                Text("Fermer", color = TextPrimary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailSheet(
    sub: Subscription,
    snapshot: Snapshot,
    currency: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = sub.product.ifBlank { "Abonnement" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2
                    )
                    if (sub.variant.isNotBlank()) {
                        Text(text = sub.variant, fontSize = 13.sp, color = TextSecondary)
                    }
                }
                StatusBadge(sub.status)
            }

            // Reference Pill
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("RÉFÉRENCE ABONNEMENT", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                        Text(sub.id, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(sub.id))
                            Toast.makeText(context, "Référence copiée !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copier", tint = Lemon, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Details Table
            DashboardCard(backgroundColor = DarkSurfaceVariant) {
                Text("Échéances et tarification", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                if (sub.renewsAt.isNotBlank() && sub.status !in setOf("cancelled", "expired")) {
                    DetailRow("Prochain renouvellement", friendlyDate(sub.renewsAt), valueColor = MintSuccessLight)
                }
                if (sub.endsAt.isNotBlank()) {
                    DetailRow(
                        if (sub.status == "cancelled") "Fin d'accès" else "Échéance",
                        friendlyDate(sub.endsAt),
                        valueColor = if (sub.status == "cancelled") CoralDangerLight else TextSecondary
                    )
                }
                if (sub.catalogPriceCents != null) {
                    val interval = when (sub.billingInterval) {
                        "month" -> "mois"; "year" -> "an"; "week" -> "semaine"; "day" -> "jour"
                        else -> sub.billingInterval
                    }
                    val freq = if (interval.isBlank()) "" else " / ${if (sub.billingIntervalCount > 1) "${sub.billingIntervalCount} " else ""}$interval"
                    DetailRow("Tarif catalogue", "${Analytics.localMoney(sub.catalogPriceCents, sub.catalogCurrency)}$freq", isBold = true)
                }
                if (sub.lastInvoiceUsdCents != null) {
                    DetailRow("Dernière facture payée", Analytics.money(sub.lastInvoiceUsdCents, currency, snapshot.eurPerUsd))
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceContainerHigh)
            ) {
                Text("Fermer", color = TextPrimary)
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
        if (action != null) action()
    }
}

@Composable
fun ExplanationBanner(value: String) {
    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Lemon, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(value, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(message, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (!actionText.isNullOrBlank() && onAction != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Lemon.copy(alpha = 0.5f))
            ) {
                Text(actionText, color = Lemon)
            }
        }
    }
}

fun friendlyDate(iso: String): String {
    val day = Analytics.dateOf(iso) ?: return "indisponible"
    return day.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRANCE))
}

fun friendlyDateTime(iso: String): String = try {
    val dt = java.time.Instant.parse(iso).atZone(java.time.ZoneId.systemDefault())
    dt.format(DateTimeFormatter.ofPattern("dd MMM à HH:mm", Locale.FRANCE))
} catch (_: Exception) { "jamais" }
