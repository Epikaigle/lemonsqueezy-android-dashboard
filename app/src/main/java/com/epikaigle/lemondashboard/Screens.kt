@file:Suppress("DEPRECATION")
package com.epikaigle.lemondashboard

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LoginScreen(loading: Boolean, onConnect: (String) -> Unit) {
    var token by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(16.dp))

        // Glowing Lemon Icon
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Lemon, LemonDark))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(44.dp))
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Vos ventes.\nEn un coup d'œil.",
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Consultez les commandes, les abonnements et les revenus Lemon Squeezy depuis votre téléphone.",
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(32.dp))

        // Input Card
        DashboardCard(backgroundColor = DarkSurfaceVariant) {
            Text(
                text = "CLÉ API LEMON SQUEEZY",
                color = MintSuccessLight,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Collez votre clé ici", color = TextMuted) },
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                leadingIcon = {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (token.isEmpty()) {
                            TextButton(onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    token = clip.trim()
                                    Toast.makeText(context, "Clé collée !", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Presse-papier vide", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Coller", color = Lemon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (visible) "Masquer la clé" else "Afficher la clé",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Lemon,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Lemon
                )
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onConnect(token) },
                enabled = token.isNotBlank() && !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Lemon,
                    contentColor = DarkBackground,
                    disabledContainerColor = DarkSurfaceContainerHigh,
                    disabledContentColor = TextMuted
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkBackground, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Connexion en cours…", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Connecter ma boutique", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Security Explanation Badge
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
                Icon(Icons.Default.Shield, contentDescription = null, tint = MintSuccessLight, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Sécurité & Confidentialité maximale",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Votre clé est chiffrée sur le téléphone (Android Keystore). L’accès aux ventes est direct et aucune donnée client n'est collectée.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = { uri.openUri("https://app.lemonsqueezy.com/settings/api") },
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Lemon, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Créer une clé API sur Lemon Squeezy ↗", color = Lemon, fontWeight = FontWeight.SemiBold)
        }
    }
}

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
    val topProducts = remember(sales, s.period) {
        sales.filter { it.paid && Analytics.inPeriod(Analytics.dateOf(it.createdAt), s.period, now) }
            .groupBy { it.product }
            .mapValues { (_, transactions) -> transactions.sumOf { it.netUsd } }
            .toList().sortedByDescending { it.second }.take(4)
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

        // KPI 2x2 Grid
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

        // Top Products Leaderboard
        if (topProducts.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(title = "Meilleurs produits", subtitle = "Classés par revenu net sur la période")
                    Spacer(Modifier.height(12.dp))
                    topProducts.forEachIndexed { i, (product, net) ->
                        val medalColor = when (i) {
                            0 -> Lemon
                            1 -> Color(0xFFE2E8F0)
                            2 -> Color(0xFFF59E0B)
                            else -> TextMuted
                        }
                        DashboardCard(
                            modifier = Modifier.padding(bottom = 8.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(medalColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${i + 1}",
                                        color = medalColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = product,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = Analytics.money(net, s.currency, snapshot.eurPerUsd),
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
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

@Composable
fun SalesScreen(snapshot: Snapshot, sales: List<Sale>, s: UiState, vm: DashboardViewModel) {
    val today = remember { LocalDate.now() }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var selectedSaleForDetail by remember { mutableStateOf<Sale?>(null) }

    val inPeriod = remember(sales, s.period) {
        sales.filter { Analytics.inPeriod(Analytics.dateOf(it.createdAt), s.period, today) }
    }
    val rows = remember(inPeriod, query, filter) {
        inPeriod.filter { sale ->
            (query.isBlank() || sale.product.contains(query.trim(), ignoreCase = true) ||
                sale.id.contains(query.trim(), ignoreCase = true) ||
                sale.source.contains(query.trim(), ignoreCase = true)) &&
                when (filter) {
                    1 -> sale.paid && sale.usdRefundCents == 0L
                    2 -> sale.usdRefundCents > 0L
                    3 -> !sale.paid
                    else -> true
                }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Transactions", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Parcourez et inspectez les commandes, factures et remboursements.", color = TextSecondary, fontSize = 13.sp)
        }

        item { StoreChips(snapshot, s.selectedStore, vm::store) }
        item { PeriodChips(s.period, vm::period) }

        // Search Bar
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Rechercher un produit, référence ou ID...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer", tint = TextSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Lemon,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Lemon,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                )
            )
        }

        // Filter Chips with Counters
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val paidCount = inPeriod.count { it.paid && it.usdRefundCents == 0L }
                val refundCount = inPeriod.count { it.usdRefundCents > 0L }
                val unpaidCount = inPeriod.count { !it.paid }

                val filterOptions = listOf(
                    "Tous (${inPeriod.size})",
                    "Payés ($paidCount)",
                    "Remboursés ($refundCount)",
                    "Non encaissés ($unpaidCount)"
                )

                filterOptions.forEachIndexed { i, label ->
                    FilterChip(
                        selected = filter == i,
                        onClick = { filter = i },
                        label = { Text(label, fontWeight = if (filter == i) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Lemon,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = DarkBorder,
                            selectedBorderColor = Lemon,
                            enabled = true,
                            selected = filter == i
                        )
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${rows.size} transaction(s) trouvée(s)", color = TextSecondary, fontSize = 12.sp)
                Text("${inPeriod.size} sur la période", color = TextMuted, fontSize = 12.sp)
            }
        }

        if (rows.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.ReceiptLong,
                    title = if (inPeriod.isEmpty()) "Aucune transaction sur cette période" else "Aucun résultat trouvé",
                    message = if (inPeriod.isEmpty()) "Sélectionnez une période plus large pour voir l'historique."
                    else "Modifiez les termes de recherche ou réinitialisez les filtres.",
                    actionText = if (query.isNotBlank() || filter != 0) "Effacer les filtres" else null,
                    onAction = { query = ""; filter = 0 }
                )
            }
        } else {
            items(rows, key = { it.id }) { sale ->
                SaleItemCard(
                    sale = sale,
                    snapshot = snapshot,
                    currency = s.currency,
                    onClick = { selectedSaleForDetail = sale }
                )
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

@Composable
fun SaleItemCard(
    sale: Sale,
    snapshot: Snapshot,
    currency: String,
    onClick: () -> Unit
) {
    val originalNet = (sale.originalCents - sale.originalRefundCents).coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (sale.source == "Commande") MintSuccessContainer.copy(alpha = 0.5f) else LemonContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (sale.source == "Commande") Icons.Default.ShoppingBag else Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = if (sale.source == "Commande") MintSuccessLight else Lemon
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = sale.product,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${sale.source} • ${friendlyDate(sale.createdAt)}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                if (sale.testMode) {
                    Text("MODE TEST", fontSize = 10.sp, color = Lemon, fontWeight = FontWeight.Bold)
                }
                if (!sale.paid) {
                    Text(sale.status, fontSize = 11.sp, color = CoralDangerLight, fontWeight = FontWeight.Medium)
                } else if (sale.usdRefundCents > 0L) {
                    Text(
                        text = if (sale.refundedAt.isNotBlank()) "Remboursé le ${friendlyDate(sale.refundedAt)}"
                        else "Remboursement partiel",
                        fontSize = 11.sp,
                        color = CoralDangerLight
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Analytics.money(sale.netUsd, currency, snapshot.eurPerUsd),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (sale.paid) TextPrimary else TextMuted
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (sale.paid) Analytics.localMoney(originalNet, sale.currency) else "Non encaissé",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun SubscriptionsScreen(
    snapshot: Snapshot,
    subscriptions: List<Subscription>,
    s: UiState,
    vm: DashboardViewModel
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var selectedSubForDetail by remember { mutableStateOf<Subscription?>(null) }

    val counts = remember(subscriptions) { subscriptions.groupingBy { it.status }.eachCount() }
    val mrr = remember(subscriptions) { Analytics.mrrEstimateUsdCents(subscriptions) }
    val arr = remember(subscriptions) { Analytics.arrEstimateUsdCents(subscriptions) }

    val rows = remember(subscriptions, query, filter) {
        subscriptions.filter { sub ->
            (query.isBlank() || sub.product.contains(query.trim(), ignoreCase = true) ||
                sub.variant.contains(query.trim(), ignoreCase = true) ||
                sub.id.contains(query.trim(), ignoreCase = true)) &&
                when (filter) {
                    1 -> sub.status == "active"
                    2 -> sub.status == "on_trial"
                    3 -> sub.status !in setOf("active", "on_trial")
                    else -> true
                }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Abonnements", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Suivi de vos revenus récurrents (MRR), renouvellements et statuts.", color = TextSecondary, fontSize = 13.sp)
        }

        item { StoreChips(snapshot, s.selectedStore, vm::store) }

        // SaaS MRR & ARR Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(PurpleAccentContainer, Color(0xFF1E1738), DarkSurface)))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REVENU RÉCURRENT MENSUEL (MRR)",
                                color = Color(0xFFC4B5FD),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                color = PurpleAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(
                                    "ARR : ${Analytics.money(arr, s.currency, snapshot.eurPerUsd)}",
                                    color = Color(0xFFDDD6FE),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = Analytics.money(mrr, s.currency, snapshot.eurPerUsd),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Estimé sur la base des ${counts["active"] ?: 0} abonnement(s) actif(s)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Subscriptions Count Row
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = "Actifs",
                    value = (counts["active"] ?: 0).toString(),
                    icon = Icons.Default.CheckCircle,
                    iconTint = MintSuccessLight,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "En essai",
                    value = (counts["on_trial"] ?: 0).toString(),
                    icon = Icons.Default.HourglassTop,
                    iconTint = AmberWarningLight,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Résiliés",
                    value = (subscriptions.size - (counts["active"] ?: 0) - (counts["on_trial"] ?: 0)).toString(),
                    icon = Icons.Default.Cancel,
                    iconTint = CoralDangerLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Rechercher un abonnement...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer", tint = TextSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Lemon,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Lemon,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                )
            )
        }

        // Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Tous (${subscriptions.size})",
                    "Actifs (${counts["active"] ?: 0})",
                    "Essais (${counts["on_trial"] ?: 0})",
                    "Autres"
                ).forEachIndexed { i, label ->
                    FilterChip(
                        selected = filter == i,
                        onClick = { filter = i },
                        label = { Text(label, fontWeight = if (filter == i) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Lemon,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = DarkBorder,
                            selectedBorderColor = Lemon,
                            enabled = true,
                            selected = filter == i
                        )
                    )
                }
            }
        }

        item {
            Text("${rows.size} abonnement(s) affiché(s)", color = TextSecondary, fontSize = 12.sp)
        }

        if (rows.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.Autorenew,
                    title = if (subscriptions.isEmpty()) "Aucun abonnement enregistré" else "Aucun abonnement correspondant",
                    message = if (subscriptions.isEmpty()) "Les abonnements apparaîtront automatiquement dès qu'un client souscrit."
                    else "Modifiez la recherche ou les filtres.",
                    actionText = if (query.isNotBlank() || filter != 0) "Effacer les filtres" else null,
                    onAction = { query = ""; filter = 0 }
                )
            }
        } else {
            items(rows, key = { it.id }) { sub ->
                SubscriptionItemCard(
                    sub = sub,
                    snapshot = snapshot,
                    currency = s.currency,
                    onClick = { selectedSubForDetail = sub }
                )
            }
        }

        item {
            ExplanationBanner("Le tarif affiché est un prix catalogue. La dernière facture payée ne prédit pas le prochain débit : remises, taxes et prorata peuvent changer.")
        }
    }

    selectedSubForDetail?.let { sub ->
        SubscriptionDetailSheet(
            sub = sub,
            snapshot = snapshot,
            currency = s.currency,
            onDismiss = { selectedSubForDetail = null }
        )
    }
}

@Composable
fun SubscriptionItemCard(
    sub: Subscription,
    snapshot: Snapshot,
    currency: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = sub.product.ifBlank { "Abonnement" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                StatusBadge(sub.status)
            }

            if (sub.variant.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(sub.variant, color = TextSecondary, fontSize = 12.sp)
            }

            Spacer(Modifier.height(10.dp))

            if (sub.renewsAt.isNotBlank() && sub.status !in setOf("cancelled", "expired")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Prochain renouvellement : ${friendlyDate(sub.renewsAt)}", color = TextSecondary, fontSize = 12.sp)
                }
            }
            if (sub.endsAt.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (sub.status == "cancelled") "Annulé • accès jusqu'au ${friendlyDate(sub.endsAt)}"
                        else "Fin d'accès : ${friendlyDate(sub.endsAt)}",
                        color = if (sub.status == "cancelled") CoralDangerLight else TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sub.catalogPriceCents != null) {
                    val interval = when (sub.billingInterval) {
                        "month" -> "mois"; "year" -> "an"; "week" -> "semaine"; "day" -> "jour"
                        else -> sub.billingInterval
                    }
                    val frequency = if (interval.isBlank()) "" else " / ${if (sub.billingIntervalCount > 1) "${sub.billingIntervalCount} " else ""}$interval"
                    Text(
                        "${Analytics.localMoney(sub.catalogPriceCents, sub.catalogCurrency)}$frequency",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lemon
                    )
                }
                if (sub.lastInvoiceUsdCents != null) {
                    Text(
                        "Dernière facture : ${Analytics.money(sub.lastInvoiceUsdCents, currency, snapshot.eurPerUsd)}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (sub.testMode) {
                Spacer(Modifier.height(4.dp))
                Text("MODE TEST", color = Lemon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

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

@Composable
fun SettingsScreen(snapshot: Snapshot, s: UiState, vm: DashboardViewModel) {
    var confirm by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Réglages", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Text("Préférences d'affichage, synchronisation et sécurité.", color = TextSecondary, fontSize = 13.sp)

        // Store Selector
        DashboardCard {
            Text("Boutiques actives", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            StoreChips(snapshot, s.selectedStore, vm::store)
        }

        // Currency Preference
        DashboardCard {
            Text("Devise d'affichage", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = s.currency == "USD",
                    onClick = { vm.currency("USD") },
                    label = { Text("USD ($)", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Lemon,
                        selectedLabelColor = DarkBackground,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = s.currency == "EUR",
                    onClick = { vm.currency("EUR") },
                    label = { Text("EUR (€)", fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Lemon,
                        selectedLabelColor = DarkBackground,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextSecondary
                    )
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (s.currency == "EUR") {
                    if (snapshot.eurPerUsd == null) "Taux EUR indisponible : les valeurs restent en USD."
                    else "Conversion indicative au taux officiel BCE du ${snapshot.fxDate} (1 USD = ${snapshot.eurPerUsd} EUR)."
                } else "Montants convertis en USD par Lemon Squeezy à la transaction.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Test Mode Toggle
        DashboardCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Mode test", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text("Afficher les commandes générées en mode bac à sable.", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = s.includeTest,
                    onCheckedChange = vm::tests,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = Lemon
                    )
                )
            }
        }

        // Push Notifications
        PushSettingsCard(vm, s)

        // Synchronization
        DashboardCard {
            Text("Synchronisation & Cache", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Mise à jour rapide automatique des 30 derniers jours. Rapprochement complet de tout l'historique conseillé toutes les 24h.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(8.dp))
            DetailRow("Dernière synchronisation", friendlyDateTime(snapshot.fetchedAt))
            DetailRow("Dernier rapprochement complet", friendlyDateTime(snapshot.lastFullSyncAt))
            DetailRow("Commandes en cache", "${snapshot.sales.size} transactions")
            DetailRow("Abonnements en cache", "${snapshot.subscriptions.size} abonnements")

            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = vm::refreshFull,
                enabled = !s.loading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Lemon.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = Lemon)
                Spacer(Modifier.width(8.dp))
                Text("Rapprocher tout l'historique", color = Lemon, fontWeight = FontWeight.Bold)
            }
        }

        // Security & Danger Zone
        DashboardCard {
            Text("Sécurité & Données locales", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Cache chiffré AES-256-GCM. Capture d'écran et sauvegarde cloud désactivées.", fontSize = 12.sp, color = TextSecondary)
            Text("Clé API stockée uniquement sur cet appareil.", fontSize = 12.sp, color = TextSecondary)

            TextButton(
                onClick = { uri.openUri("https://docs.lemonsqueezy.com/api") },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text("Documentation officielle Lemon Squeezy ↗", color = Lemon)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { confirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralDangerContainer)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = CoralDangerLight)
                Spacer(Modifier.width(9.dp))
                Text("Effacer la clé et toutes les données", color = CoralDangerLight, fontWeight = FontWeight.Bold)
            }
        }

        ExplanationBanner("Application personnelle et indépendante, non affiliée à Lemon Squeezy. Fonctionnement en lecture seule : aucune modification de commande ou d'abonnement n'est possible.")
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Effacer toutes les données ?", fontWeight = FontWeight.Bold) },
            text = { Text("La clé API chiffrée et le cache local seront définitivement supprimés de ce téléphone. Les données sur votre compte Lemon Squeezy restent intactes.") },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            confirmButton = {
                TextButton(onClick = { confirm = false; vm.disconnect() }) {
                    Text("Supprimer", color = CoralDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun PushSettingsCard(vm: DashboardViewModel, s: UiState) {
    val context = LocalContext.current
    var origin by remember { mutableStateOf(s.pushOrigin) }
    var pairingSecret by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.enablePush(origin, pairingSecret)
    }

    DashboardCard {
        Text("Notifications instantanées", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            "Soyez alerté des nouvelles ventes et renouvellements en temps réel.",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(10.dp))

        if (!PushNotifications.available(context)) {
            Surface(
                color = CoralDangerContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CoralDangerLight, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Firebase n'est pas configuré dans ce build. La release finale doit inclure votre google-services.json.",
                        color = CoralDangerLight,
                        fontSize = 12.sp
                    )
                }
            }
        } else if (s.pushEnabled) {
            Surface(
                color = MintSuccessContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Alertes instantanées activées", color = MintSuccessLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("Relais : ${s.pushOrigin}", color = TextSecondary, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = vm::disablePush,
                enabled = !s.pushBusy,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CoralDanger.copy(alpha = 0.5f))
            ) {
                Text("Désactiver les alertes", color = CoralDangerLight)
            }
        } else {
            OutlinedTextField(
                value = origin,
                onValueChange = { origin = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Adresse HTTPS du relais") },
                placeholder = { Text("https://mon-worker.workers.dev") },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pairingSecret,
                onValueChange = { pairingSecret = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Secret d'appairage") },
                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { reveal = !reveal }) {
                        Icon(
                            if (reveal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (reveal) "Masquer le secret" else "Afficher le secret"
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    if (PushNotifications.permissionGranted(context)) vm.enablePush(origin, pairingSecret)
                    else permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                enabled = !s.pushBusy && origin.isNotBlank() && pairingSecret.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground)
            ) {
                Text(if (s.pushBusy) "Connexion…" else "Activer les notifications", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Le secret est chiffré sur cet appareil. Aucune clé API Lemon Squeezy n'est transmise au relais.",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        if (s.pushMessage.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = s.pushMessage,
                color = if (s.pushEnabled) MintSuccessLight else CoralDangerLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StoreChips(snapshot: Snapshot, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected.isEmpty(),
            onClick = { onSelect("") },
            label = { Text("Toutes les boutiques", fontWeight = if (selected.isEmpty()) FontWeight.Bold else FontWeight.Normal) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Lemon,
                selectedLabelColor = DarkBackground,
                containerColor = DarkSurfaceVariant,
                labelColor = TextSecondary
            ),
            border = FilterChipDefaults.filterChipBorder(
                borderColor = DarkBorder,
                selectedBorderColor = Lemon,
                enabled = true,
                selected = selected.isEmpty()
            )
        )
        snapshot.stores.forEach { store ->
            val isSelected = selected == store.id
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(store.id) },
                label = { Text(store.name, maxLines = 1, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Lemon,
                    selectedLabelColor = DarkBackground,
                    containerColor = DarkSurfaceVariant,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = DarkBorder,
                    selectedBorderColor = Lemon,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

@Composable
fun PeriodChips(selected: Period, onSelect: (Period) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Period.entries.forEach { period ->
            val isSelected = selected == period
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(period) },
                label = { Text(period.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Lemon,
                    selectedLabelColor = DarkBackground,
                    containerColor = DarkSurfaceVariant,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = DarkBorder,
                    selectedBorderColor = Lemon,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}
