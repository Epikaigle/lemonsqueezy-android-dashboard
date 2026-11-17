@file:Suppress("DEPRECATION")
package com.epikaigle.lemondashboard

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
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
                .padding(14.dp),
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(Modifier.padding(12.dp)) {
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
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MetricCounterCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color = Lemon,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

fun pluralFr(count: Int, singular: String, plural: String = "${singular}s"): String =
    if (count <= 1) "$count\u00A0$singular" else "$count\u00A0$plural"

fun daysUntilLabel(days: Long): String = when {
    days <= 0L -> "Aujourd'hui"
    days == 1L -> "Demain"
    else -> "Dans $days\u00A0jours"
}

fun frenchStatusLabel(status: String): String = when (status) {
    "paid" -> "Payé"
    "active" -> "Actif"
    "inactive" -> "Non activée"
    "disabled" -> "Désactivée"
    "on_trial" -> "En essai"
    "cancelled" -> "Résilié"
    "refunded" -> "Remboursé"
    "partial_refund" -> "Remb. partiel"
    "expired" -> "Expiré"
    "past_due" -> "En retard"
    "unpaid" -> "Impayé"
    "paused" -> "En pause"
    "pending" -> "En attente"
    "failed" -> "Échoué"
    "void" -> "Annulé"
    "draft" -> "Brouillon"
    else -> status.replaceFirstChar { it.uppercase() }
}

@Composable
fun TrendBadge(rate: Double?, label: String? = null) {
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
    val rateText = when {
        isZero -> "Stable"
        else -> "${if (isPositive) "+" else ""}${"%.1f".format(Locale.FRANCE, rate)}\u00A0%"
    }
    val displayText = if (label != null) "$rateText $label" else rateText

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(13.dp))
            Text(
                text = displayText,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val label = frenchStatusLabel(status)
    val (tint, bg) = when (status) {
        "paid", "active" -> MintSuccessLight to MintSuccessContainer.copy(alpha = 0.4f)
        "on_trial", "partial_refund", "paused", "pending", "inactive" -> AmberWarningLight to AmberWarningContainer.copy(alpha = 0.4f)
        "cancelled", "refunded", "past_due", "unpaid", "disabled", "failed" -> CoralDangerLight to CoralDangerContainer.copy(alpha = 0.4f)
        "expired", "void", "draft" -> TextMuted to DarkSurfaceVariant
        else -> TextSecondary to DarkSurfaceVariant
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
            softWrap = false,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

private val frenchMonthsShort = listOf(
    "Janv.", "Févr.", "Mars", "Avr.", "Mai", "Juin",
    "Juil.", "Août", "Sept.", "Oct.", "Nov.", "Déc."
)

private val frenchMonthsFull = listOf(
    "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
)

private fun formatChartAxisLabel(raw: String, period: Period): String {
    if (raw.isBlank()) return ""
    return when (period) {
        Period.MONTH -> if (raw == "1") "1er" else raw
        Period.YEAR -> {
            val monthIndex = (raw.toIntOrNull() ?: 0) - 1
            frenchMonthsShort.getOrElse(monthIndex) { raw }
        }
        else -> raw
    }
}

private fun formatChartTooltipLabel(raw: String, period: Period): String = when (period) {
    Period.DAY -> "Créneau : ${raw.removeSuffix("h")}h00"
    Period.WEEK -> "Date : $raw"
    Period.MONTH -> "Le ${if (raw == "1") "1er" else raw} du mois"
    Period.YEAR -> {
        val monthIndex = (raw.toIntOrNull() ?: 0) - 1
        "Mois : ${frenchMonthsFull.getOrElse(monthIndex) { raw }}"
    }
    Period.ALL -> if (raw == "Sans date") "Remboursements sans date" else "Année $raw"
}

@Composable
fun InteractiveChartCard(
    buckets: List<Pair<String, Long>>,
    currency: String,
    rate: Double?,
    periodTitle: String,
    period: Period = Period.MONTH
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
            Text(
                text = "Évolution des revenus",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                            text = formatChartTooltipLabel(pair.first, period),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            text = Analytics.money(pair.second, currency, rate),
                            fontWeight = FontWeight.Bold,
                            color = if (pair.second >= 0) Lemon else CoralDangerLight,
                            fontSize = 13.sp,
                            softWrap = false,
                            maxLines = 1
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
                val firstLabel = formatChartAxisLabel(buckets.firstOrNull()?.first.orEmpty(), period)
                val midLabel = formatChartAxisLabel(buckets.getOrNull(buckets.size / 2)?.first.orEmpty(), period)
                val lastLabel = formatChartAxisLabel(buckets.lastOrNull()?.first.orEmpty(), period)

                Text(firstLabel, color = TextMuted, fontSize = 11.sp, softWrap = false)
                if (buckets.size > 2) {
                    Text(midLabel, color = TextMuted, fontSize = 11.sp, softWrap = false)
                }
                Text(lastLabel, color = TextMuted, fontSize = 11.sp, softWrap = false)
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
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
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
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("IDENTIFIANT DE TRANSACTION", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = sale.id,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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

            // Customer / Buyer Information Card
            CustomerInfoCard("Acheteur / Client", sale.userName, sale.userEmail)

            // Breakdown Table Card
            DashboardCard(backgroundColor = DarkSurfaceVariant) {
                Text("Détail financier", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                sale.subtotalUsdCents?.let {
                    DetailRow("Sous-total", Analytics.money(it, currency, snapshot.eurPerUsd))
                }
                sale.discountUsdCents?.takeIf { it > 0L }?.let {
                    DetailRow("Remise", "- " + Analytics.money(it, currency, snapshot.eurPerUsd))
                }
                DetailRow("Total facturé (TTC)", Analytics.money(sale.usdCents, currency, snapshot.eurPerUsd))
                if (sale.taxUsdCents != null) {
                    val label = sale.taxName.ifBlank { "TVA / taxes" }
                    val rate = sale.taxRate?.let { " (" + it + "\u00A0%)" } ?: ""
                    DetailRow(label + rate, Analytics.money(sale.taxUsdCents, currency, snapshot.eurPerUsd),
                        valueColor = TextSecondary)
                    sale.taxInclusive?.let { included ->
                        DetailRow("Taxe dans le prix", if (included) "Incluse" else "Ajoutée",
                            valueColor = TextMuted)
                    }
                } else {
                    Text("Détail de TVA indisponible dans le cache. Actualisez les données.",
                        fontSize = 11.sp, color = TextMuted)
                }
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
                    "Après remboursements",
                    Analytics.money(sale.netUsd, currency, snapshot.eurPerUsd),
                    isBold = true,
                    valueColor = if (sale.paid) MintSuccessLight else TextMuted
                )
                Analytics.estimatedPlatformFeeUsdCents(sale)?.let { fee ->
                    DetailRow("Frais MoR (base estimée)",
                        "- " + Analytics.money(fee, currency, snapshot.eurPerUsd),
                        valueColor = TextSecondary)
                }
                Analytics.estimatedAfterTaxesAndFeesUsdCents(sale)?.let { projected ->
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 8.dp))
                    DetailRow("Net estimé après TVA & frais",
                        "≈ " + Analytics.money(projected, currency, snapshot.eurPerUsd),
                        isBold = true, valueColor = MintSuccessLight)
                }
                if (sale.paid) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (sale.usdRefundCents > 0L)
                            "Revenu net non estimé : ajustements de remboursement indisponibles."
                        else "Estimation hors suppléments internationaux (+1,5\u00A0%), PayPal ou virement (1\u00A0%).",
                        fontSize = 11.sp, color = TextMuted
                    )
                }
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

            OutlinedButton(
                onClick = {
                    val shareText = buildString {
                        appendLine("🧾 Transaction Lemon Squeezy :")
                        appendLine("• Produit : ${sale.product}")
                        appendLine("• Référence : #${sale.id}")
                        if (sale.userName.isNotBlank() || sale.userEmail.isNotBlank()) {
                            appendLine("• Client : ${listOf(sale.userName, sale.userEmail).filter { it.isNotBlank() }.joinToString(" • ")}")
                        }
                        appendLine("• Date : ${friendlyDateTime(sale.createdAt)}")
                        appendLine("• Montant après remboursements, avant TVA et frais : ${Analytics.money(sale.netUsd, currency, snapshot.eurPerUsd)}")
                        appendLine("• Statut : ${frenchStatusLabel(sale.status)}")
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Commande Lemon Squeezy #${sale.id}")
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Partager la transaction"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Lemon.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Lemon, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Partager le récapitulatif", color = Lemon, fontWeight = FontWeight.Bold)
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
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = sub.product.ifBlank { "Abonnement" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (sub.variant.isNotBlank()) {
                        Text(text = sub.variant, fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("RÉFÉRENCE ABONNEMENT", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = sub.id,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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

            // Customer / Subscriber Information Card
            CustomerInfoCard("Abonné / Client", sub.userName, sub.userEmail)

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
                    val freq = if (interval.isBlank()) "" else "\u00A0/ ${if (sub.billingIntervalCount > 1) "${sub.billingIntervalCount}\u00A0" else ""}$interval"
                    DetailRow("Tarif catalogue", "${Analytics.localMoney(sub.catalogPriceCents, sub.catalogCurrency)}$freq", isBold = true)
                }
                if (sub.lastInvoiceUsdCents != null) {
                    DetailRow("Dernière facture payée", Analytics.money(sub.lastInvoiceUsdCents, currency, snapshot.eurPerUsd))
                }
            }

            if (sub.testMode) {
                Surface(
                    color = LemonContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Lemon.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = Lemon, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Abonnement effectué en MODE TEST", fontSize = 12.sp, color = Lemon, fontWeight = FontWeight.Medium)
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

@Composable
fun CustomerInfoCard(
    title: String,
    userName: String,
    userEmail: String
) {
    if (userName.isBlank() && userEmail.isBlank()) return
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    DashboardCard(backgroundColor = DarkSurfaceVariant) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Lemon, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        if (userName.isNotBlank()) {
            Text(
                text = userName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (userEmail.isNotBlank()) Spacer(Modifier.height(6.dp))
        }
        if (userEmail.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = userEmail,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(userEmail))
                        Toast.makeText(context, "Email copié !", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copier l'email", tint = Lemon, modifier = Modifier.size(15.dp))
                }
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
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f).padding(end = 10.dp)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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

@Composable
fun LicenseKeyCard(
    license: LicenseKey,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val effectiveStatus = if (license.disabled) "disabled" else license.status
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = license.productName.ifBlank { "Licence logicielle" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (license.expiresAt.isNotBlank()) "Expire le ${friendlyDate(license.expiresAt)}" else "Licence à vie (Perpétuelle)",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusBadge(effectiveStatus)
            }

            if (license.userName.isNotBlank() || license.userEmail.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Lemon, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = listOf(license.userName, license.userEmail).filter { it.isNotBlank() }.joinToString(" • "),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = license.keyShort.ifBlank { license.key.take(18) + "…" },
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Lemon,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(license.key))
                            Toast.makeText(context, "Clé copiée !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copier la clé", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activationText = if (license.activationLimit == 0) {
                    "${pluralFr(license.instancesCount, "poste activé", "postes activés")} • Illimité"
                } else {
                    "Activations : ${license.instancesCount}\u00A0/\u00A0${license.activationLimit}\u00A0PC"
                }
                Text(
                    text = activationText,
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (license.instancesCount > 0) {
                    Surface(
                        color = MintSuccessContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "En service",
                            fontSize = 10.sp,
                            color = MintSuccessLight,
                            fontWeight = FontWeight.Bold,
                            softWrap = false,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseKeyDetailSheet(
    license: LicenseKey,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val effectiveStatus = if (license.disabled) "disabled" else license.status

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
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = license.productName.ifBlank { "Licence logicielle" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Émise le ${friendlyDateTime(license.createdAt)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                StatusBadge(effectiveStatus)
            }

            DashboardCard(backgroundColor = DarkSurfaceVariant) {
                Text("CLÉ DE LICENCE COMPLÈTE", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = license.key,
                    fontSize = 13.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = Lemon,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(license.key))
                            Toast.makeText(context, "Clé de licence copiée !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copier", fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareText = buildString {
                                appendLine("🔑 Clé de licence : ${license.productName.ifBlank { "Logiciel" }}")
                                appendLine(license.key)
                                if (license.userName.isNotBlank() || license.userEmail.isNotBlank()) {
                                    appendLine("Titulaire : ${listOf(license.userName, license.userEmail).filter { it.isNotBlank() }.joinToString(" • ")}")
                                }
                                if (license.expiresAt.isNotBlank()) {
                                    appendLine("Date d'expiration : ${friendlyDate(license.expiresAt)}")
                                }
                                if (license.activationLimit > 0) {
                                    appendLine("Limite : ${pluralFr(license.activationLimit, "poste", "postes")}")
                                }
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Clé de licence ${license.productName}")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Partager la clé"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Lemon.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Lemon, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Partager", color = Lemon, fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                    }
                }
            }

            // Customer / License Holder Card
            CustomerInfoCard("Titulaire de la licence", license.userName, license.userEmail)

            DashboardCard(backgroundColor = DarkSurfaceVariant) {
                Text("Détails d'activation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                val limit = if (license.activationLimit == 0) "Illimitée" else pluralFr(license.activationLimit, "poste", "postes")
                DetailRow("Limite d'activation", limit)
                DetailRow(
                    "Postes activés",
                    pluralFr(license.instancesCount, "poste actif", "postes actifs"),
                    valueColor = if (license.instancesCount > 0) MintSuccessLight else TextSecondary
                )
                if (license.expiresAt.isNotBlank()) {
                    DetailRow("Date d'expiration", friendlyDate(license.expiresAt), valueColor = AmberWarningLight)
                } else {
                    DetailRow("Durée de validité", "Licence perpétuelle (À vie)", valueColor = MintSuccessLight)
                }
                if (license.orderId.isNotBlank()) {
                    DetailRow("Commande associée", "#${license.orderId}")
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
fun MerchantHealthCard(refundRate: Double, onClick: (() -> Unit)? = null) {
    val (statusText, statusColor, containerColor) = when {
        refundRate < 1.0 -> Triple("Excellente (< 1\u00A0%)", MintSuccessLight, MintSuccessContainer)
        refundRate <= 2.0 -> Triple("Modérée (1–2\u00A0%)", AmberWarningLight, AmberWarningContainer)
        else -> Triple("Attention (> 2\u00A0%)", CoralDangerLight, CoralDangerContainer)
    }

    DashboardCard(modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "SANTÉ DU COMPTE MARCHAND",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.FRANCE, "%.1f\u00A0%% de remboursement", refundRate),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = containerColor.copy(alpha = 0.6f),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    softWrap = false,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TopProductsCard(products: List<Analytics.ProductMetric>, currency: String, eurPerUsd: Double?) {
    DashboardCard {
        Text("Meilleures ventes", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        if (products.isEmpty()) {
            Text("Aucune vente enregistrée sur cette sélection.", fontSize = 12.sp, color = TextMuted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                products.forEachIndexed { index, p ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = DarkSurfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Lemon)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(pluralFr(p.salesCount, "vente"), fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                        }
                        Text(
                            text = Analytics.money(p.netUsdCents, currency, eurPerUsd),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                    if (index < products.lastIndex) {
                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingRenewalsCard(renewals: List<Analytics.UpcomingRenewal>, currency: String, eurPerUsd: Double?) {
    DashboardCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Renouvellements à venir (30j)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val projectedTotal = renewals.sumOf { it.estimatedUsdCents }
            Surface(
                color = PurpleAccent.copy(alpha = 0.2f),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = "+${Analytics.money(projectedTotal, currency, eurPerUsd)}",
                    color = Color(0xFFDDD6FE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    softWrap = false,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (renewals.isEmpty()) {
            Text("Aucun renouvellement programmé dans les 30 prochains jours.", fontSize = 12.sp, color = TextMuted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                renewals.take(5).forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = r.subscription.product.ifBlank { "Abonnement" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val buyer = r.subscription.userName.ifBlank { r.subscription.userEmail }
                            val line2 = if (buyer.isNotBlank()) {
                                "$buyer • ${daysUntilLabel(r.daysRemaining)}"
                            } else {
                                "${daysUntilLabel(r.daysRemaining)} • ${friendlyDate(r.renewalDate.toString())}"
                            }
                            Text(
                                text = line2,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = Analytics.money(r.estimatedUsdCents, currency, eurPerUsd),
                            fontSize = 13.sp,
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
}

@Composable
fun FeeCalculatorDialog(
    currency: String,
    eurPerUsd: Double?,
    onDismiss: () -> Unit
) {
    var priceInput by remember { mutableStateOf("29") }
    var isInternational by remember { mutableStateOf(false) }
    var isSubscription by remember { mutableStateOf(false) }

    val localCents = kotlin.math.round((priceInput.toDoubleOrNull() ?: 0.0) * 100.0).toLong()
    val fx = (eurPerUsd ?: (1.0 / 1.1511)).coerceAtLeast(0.01)
    val fixedLocalCents = if (currency.equals("EUR", ignoreCase = true)) {
        kotlin.math.round(50.0 * fx).toLong()
    } else {
        50L
    }
    val pct = 5.0 + (if (isInternational) 1.5 else 0.0) + (if (isSubscription) 0.5 else 0.0)
    val totalFeeCents = if (localCents <= 0L) 0L else {
        (kotlin.math.round(localCents * pct / 100.0).toLong() + fixedLocalCents).coerceAtMost(localCents)
    }
    val netLocalCents = (localCents - totalFeeCents).coerceAtLeast(0L)
    val effectivePercent = if (localCents > 0L) (totalFeeCents.toDouble() / localCents.toDouble()) * 100.0 else 0.0
    val symbol = if (currency.equals("EUR", ignoreCase = true)) "€" else "$"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = Lemon, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Simulateur de frais MoR", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { input ->
                        val clean = input.replace(',', '.')
                        if (clean.count { it == '.' } <= 1 && clean.all { it.isDigit() || it == '.' }) {
                            priceInput = clean
                        }
                    },
                    label = { Text("Prix de vente ($currency)", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Lemon,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Lemon
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Price Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("9", "19", "29", "49", "99", "149", "199").forEach { preset ->
                        FilterChip(
                            selected = priceInput == preset,
                            onClick = { priceInput = preset },
                            label = { Text("$preset\u00A0$symbol", fontSize = 11.sp, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Lemon,
                                selectedLabelColor = DarkBackground,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Carte internationale ou PayPal (+1,5\u00A0%)",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Switch(
                        checked = isInternational,
                        onCheckedChange = { isInternational = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = Lemon)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Abonnement récurrent (+0,5\u00A0%)",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Switch(
                        checked = isSubscription,
                        onCheckedChange = { isSubscription = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = Lemon)
                    )
                }

                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val marginPercent = if (localCents > 0L) (netLocalCents.toDouble() / localCents.toDouble()) * 100.0 else 0.0
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow("Prix brut HT", Analytics.localMoney(localCents, currency))
                        DetailRow(
                            "Frais MoR (${String.format(Locale.FRANCE, "%.1f", effectivePercent)}\u00A0%)",
                            "- ${Analytics.localMoney(totalFeeCents, currency)}",
                            valueColor = CoralDangerLight
                        )
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))
                        DetailRow(
                            "Bénéfice net estimé",
                            Analytics.localMoney(netLocalCents, currency),
                            isBold = true,
                            valueColor = MintSuccessLight
                        )
                        DetailRow(
                            "Marge nette conservée",
                            String.format(Locale.FRANCE, "%.1f\u00A0%%", marginPercent),
                            valueColor = MintSuccessLight
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground)
            ) {
                Text("Fermer", fontWeight = FontWeight.Bold)
            }
        }
    )
}

fun friendlyDate(iso: String): String {
    val day = Analytics.dateOf(iso) ?: return "indisponible"
    return day.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRANCE))
}

fun friendlyDateTime(iso: String): String = try {
    val dt = java.time.Instant.parse(iso).atZone(java.time.ZoneId.systemDefault())
    dt.format(DateTimeFormatter.ofPattern("dd MMM à HH:mm", Locale.FRANCE))
} catch (_: Exception) { "jamais" }

data class GlossaryTerm(
    val badge: String,
    val badgeColor: Color,
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossarySheet(onDismiss: () -> Unit) {
    val terms = remember {
        listOf(
            GlossaryTerm(
                badge = "MoR",
                badgeColor = Lemon,
                title = "Merchant of Record (Marchand de référence)",
                description = "Lemon Squeezy agit comme revendeur officiel et légal de vos produits. Ils encaissent vos clients mondiaux, calculent et reversent automatiquement la TVA et taxes locales, gèrent la conformité et les contestations bancaires, puis vous reversent votre revenu net après une rétention de sécurité."
            ),
            GlossaryTerm(
                badge = "13 jours",
                badgeColor = AmberWarningLight,
                title = "Période de rétention de sécurité",
                description = "Délai de réserve obligatoire appliqué par Lemon Squeezy après chaque vente avant que les fonds ne soient éligibles au versement. Cette retenue garantit la couverture des contestations bancaires et demandes de remboursement."
            ),
            GlossaryTerm(
                badge = "≥ 50 $",
                badgeColor = MintSuccessLight,
                title = "Seuil minimum de versement bancaire",
                description = "Lemon Squeezy déclenche un virement bancaire automatique uniquement si votre solde disponible atteint au moins 50 $ lors des clôtures de cycle (1er et 15 du mois). En dessous de ce seuil, les fonds restent en réserve et s'accumulent."
            ),
            GlossaryTerm(
                badge = "14 & 28",
                badgeColor = MintSuccessLight,
                title = "Dates des virements bancaires",
                description = "Lemon Squeezy émet les ordres de virement deux fois par mois : le 14 (pour les ventes validées au 1er) et le 28 (pour les ventes validées au 15). Les fonds arrivent sur votre compte sous 1 à 3 jours ouvrés."
            ),
            GlossaryTerm(
                badge = "MRR",
                badgeColor = PurpleAccent,
                title = "Monthly Recurring Revenue (Revenu Mensuel Récurrent)",
                description = "Revenu normalisé sur 30 jours généré par l'ensemble de vos abonnements actifs. Par exemple, un abonnement annuel vendu 120 € contribue à hauteur de 10 € / mois à votre MRR."
            ),
            GlossaryTerm(
                badge = "ARR",
                badgeColor = PurpleAccent,
                title = "Annual Recurring Revenue (Revenu Annuel Récurrent)",
                description = "Projection annuelle de vos abonnements récurrents actuels (MRR × 12 mois), en supposant que le parc d'abonnés reste stable sur une année."
            ),
            GlossaryTerm(
                badge = "Quick Ratio",
                badgeColor = MintSuccessLight,
                title = "Indicateur de santé de croissance SaaS",
                description = "Mesure le rapport entre les nouveaux revenus récurrents gagnés et les revenus perdus par résiliation (churn). Un score supérieur à 4,0 indique une excellente croissance saine ; entre 1,0 et 4,0 une croissance modérée ; inférieur à 1,0 une attrition nette."
            ),
            GlossaryTerm(
                badge = "Churn",
                badgeColor = CoralDangerLight,
                title = "Taux de Churn (Attrition / Résiliation)",
                description = "Pourcentage d'abonnés ayant résilié ou non renouvelé leur abonnement au cours de la période. Un taux faible (< 5 %) témoigne d'une excellente rétention."
            ),
            GlossaryTerm(
                badge = "AOV",
                badgeColor = Lemon,
                title = "Average Order Value (Panier Moyen)",
                description = "Montant moyen payé par commande sur la période sélectionnée (Chiffre d'affaires net divisé par le nombre de transactions)."
            ),
            GlossaryTerm(
                badge = "Pending",
                badgeColor = AmberWarningLight,
                title = "En attente / Non encaissé",
                description = "Désigne un paiement client en cours de confirmation bancaire, ou un montant en cours de rétention de sécurité (13 jours) avant d'être libéré pour votre prochain virement."
            ),
            GlossaryTerm(
                badge = "ARPU",
                badgeColor = Lemon,
                title = "Average Revenue Per User (Revenu / Abonné)",
                description = "Revenu mensuel récurrent moyen généré par chaque abonnement actif (MRR divisé par le nombre d'abonnés actifs)."
            ),
            GlossaryTerm(
                badge = "TVA",
                badgeColor = TextSecondary,
                title = "TVA & Taxes locales",
                description = "Taxe sur la valeur ajoutée perçue auprès du client final selon son pays. En tant que MoR, Lemon Squeezy collecte et reverse directement cette TVA aux autorités fiscales locales."
            )
        )
    }

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
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Lemon.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Lemon, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Glossaire & Définitions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Comprendre les sigles et règles financières de Lemon Squeezy", fontSize = 11.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(4.dp))

            terms.forEach { term ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = term.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = term.badgeColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, term.badgeColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = term.badge,
                                    color = term.badgeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = term.description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceContainerHigh)
            ) {
                Text("Compris", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
