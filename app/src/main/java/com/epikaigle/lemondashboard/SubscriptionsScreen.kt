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
fun SubscriptionsScreen(
    snapshot: Snapshot,
    subscriptions: List<Subscription>,
    s: UiState,
    vm: DashboardViewModel
) {
    val focusManager = LocalFocusManager.current
    var activeSubTab by remember { mutableIntStateOf(0) }
    var subQuery by remember { mutableStateOf("") }
    var subFilter by remember { mutableIntStateOf(0) }
    var selectedSubForDetail by remember { mutableStateOf<Subscription?>(null) }

    var licQuery by remember { mutableStateOf("") }
    var licFilter by remember { mutableIntStateOf(0) }
    var selectedLicForDetail by remember { mutableStateOf<LicenseKey?>(null) }

    val licenseKeys = remember(snapshot.licenseKeys, s.selectedStore) {
        Analytics.selectedLicenseKeys(snapshot, s.selectedStore)
    }

    val counts = remember(subscriptions) { subscriptions.groupingBy { it.status }.eachCount() }
    val mrr = remember(subscriptions) { Analytics.mrrEstimateUsdCents(subscriptions) }
    val arr = remember(subscriptions) { Analytics.arrEstimateUsdCents(subscriptions) }
    val churnRate = remember(subscriptions) { Analytics.churnRatePercent(subscriptions) }
    val quickRatio = remember(subscriptions) { Analytics.quickRatio(subscriptions) }
    val intervals = remember(subscriptions) { Analytics.billingIntervalBreakdown(subscriptions) }
    val renewals = remember(subscriptions) { Analytics.upcomingRenewals(subscriptions, 30) }

    val filteredSubs = remember(subscriptions, subQuery, subFilter) {
        subscriptions.filter { sub ->
            (subQuery.isBlank() || sub.product.contains(subQuery.trim(), ignoreCase = true) ||
                sub.variant.contains(subQuery.trim(), ignoreCase = true) ||
                sub.id.contains(subQuery.trim(), ignoreCase = true)) &&
                when (subFilter) {
                    1 -> sub.status == "active"
                    2 -> sub.status == "on_trial"
                    3 -> sub.status !in setOf("active", "on_trial")
                    else -> true
                }
        }
    }

    val filteredLicenses = remember(licenseKeys, licQuery, licFilter) {
        licenseKeys.filter { lic ->
            (licQuery.isBlank() || lic.productName.contains(licQuery.trim(), ignoreCase = true) ||
                lic.key.contains(licQuery.trim(), ignoreCase = true) ||
                lic.keyShort.contains(licQuery.trim(), ignoreCase = true) ||
                lic.orderId.contains(licQuery.trim(), ignoreCase = true)) &&
                when (licFilter) {
                    1 -> lic.status == "active" && !lic.disabled
                    2 -> lic.status == "expired"
                    3 -> lic.status == "disabled" || lic.disabled
                    else -> true
                }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("SaaS & Licences", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Revenus récurrents (MRR), renouvellements, churn et gestion des clés de licence.", color = TextSecondary, fontSize = 13.sp)
        }

        item { StoreChips(snapshot, s.selectedStore, vm::store) }

        // Sub-Tab Switcher: Abonnements vs Licences
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceVariant)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeSubTab == 0) Lemon else Color.Transparent)
                        .clickable { activeSubTab = 0 }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = if (activeSubTab == 0) DarkBackground else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Abonnements (${subscriptions.size})",
                            color = if (activeSubTab == 0) DarkBackground else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeSubTab == 1) Lemon else Color.Transparent)
                        .clickable { activeSubTab = 1 }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = if (activeSubTab == 1) DarkBackground else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Licences (${licenseKeys.size})",
                            color = if (activeSubTab == 1) DarkBackground else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (activeSubTab == 0) {
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

            // SaaS Health Indicators: Churn Rate & Quick Ratio
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Taux de Churn",
                        value = String.format(Locale.US, "%.1f%%", churnRate),
                        icon = Icons.Default.TrendingDown,
                        iconTint = if (churnRate > 5.0) CoralDangerLight else MintSuccessLight,
                        subtitle = "Attrition abonnés",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Quick Ratio",
                        value = String.format(Locale.US, "%.1f", quickRatio),
                        icon = Icons.Default.Speed,
                        iconTint = if (quickRatio >= 4.0) MintSuccessLight else if (quickRatio >= 1.0) AmberWarningLight else CoralDangerLight,
                        subtitle = if (quickRatio >= 4.0) "Excellente rétention" else "Expansion/Départs",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Billing Interval Breakdown Pill
            item {
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
                        Text("Formules récurrentes", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${intervals["Mensuel"] ?: 0} mensuel(s) • ${intervals["Annuel"] ?: 0} annuel(s)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lemon
                        )
                    }
                }
            }

            // Upcoming Renewals Calendar
            if (renewals.isNotEmpty()) {
                item {
                    UpcomingRenewalsCard(renewals, s.currency, snapshot.eurPerUsd)
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

            // Subscriptions Search Bar
            item {
                OutlinedTextField(
                    value = subQuery,
                    onValueChange = { subQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Rechercher un abonnement...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = {
                        if (subQuery.isNotEmpty()) {
                            IconButton(onClick = { subQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer", tint = TextSecondary)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
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
                            selected = subFilter == i,
                            onClick = { subFilter = i },
                            label = { Text(label, fontWeight = if (subFilter == i) FontWeight.Bold else FontWeight.Normal) },
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
                                selected = subFilter == i
                            )
                        )
                    }
                }
            }

            item {
                Text("${filteredSubs.size} abonnement(s) affiché(s)", color = TextSecondary, fontSize = 12.sp)
            }

            if (filteredSubs.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Autorenew,
                        title = if (subscriptions.isEmpty()) "Aucun abonnement enregistré" else "Aucun abonnement correspondant",
                        message = if (subscriptions.isEmpty()) "Les abonnements apparaîtront automatiquement dès qu'un client souscrit."
                        else "Modifiez la recherche ou les filtres.",
                        actionText = if (subQuery.isNotBlank() || subFilter != 0) "Effacer les filtres" else null,
                        onAction = { subQuery = ""; subFilter = 0 }
                    )
                }
            } else {
                items(filteredSubs, key = { it.id }) { sub ->
                    SubscriptionItemCard(
                        sub = sub,
                        snapshot = snapshot,
                        currency = s.currency,
                        onClick = { selectedSubForDetail = sub }
                    )
                }
            }
        } else {
            // TAB 1: LICENCES LOGICIELLES
            val activeLicCount = licenseKeys.count { it.status == "active" && !it.disabled }
            val expiredLicCount = licenseKeys.count { it.status == "expired" }
            val disabledLicCount = licenseKeys.count { it.status == "disabled" || it.disabled }
            val activeInstancesCount = licenseKeys.sumOf { it.instancesCount }

            // KPI row for licenses
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Licences actives",
                        value = activeLicCount.toString(),
                        icon = Icons.Default.VpnKey,
                        iconTint = MintSuccessLight,
                        subtitle = "Sur ${licenseKeys.size} au total",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Postes activés",
                        value = activeInstancesCount.toString(),
                        icon = Icons.Default.Devices,
                        iconTint = Lemon,
                        subtitle = "Machines en service",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search Bar for License Keys
            item {
                OutlinedTextField(
                    value = licQuery,
                    onValueChange = { licQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Rechercher une clé, produit ou commande...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = {
                        if (licQuery.isNotEmpty()) {
                            IconButton(onClick = { licQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer", tint = TextSecondary)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
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

            // License Status Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Toutes (${licenseKeys.size})",
                        "Actives ($activeLicCount)",
                        "Expirées ($expiredLicCount)",
                        "Désactivées ($disabledLicCount)"
                    ).forEachIndexed { i, label ->
                        FilterChip(
                            selected = licFilter == i,
                            onClick = { licFilter = i },
                            label = { Text(label, fontWeight = if (licFilter == i) FontWeight.Bold else FontWeight.Normal) },
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
                                selected = licFilter == i
                            )
                        )
                    }
                }
            }

            item {
                Text("${filteredLicenses.size} licence(s) affichée(s)", color = TextSecondary, fontSize = 12.sp)
            }

            if (filteredLicenses.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.VpnKey,
                        title = if (licenseKeys.isEmpty()) "Aucune licence logicielle générée" else "Aucune licence correspondante",
                        message = if (licenseKeys.isEmpty()) "Les clés de licence apparaîtront automatiquement lors de la vente de logiciels ou produits configurés avec le module licensing Lemon Squeezy."
                        else "Modifiez les filtres ou le texte recherché.",
                        actionText = if (licQuery.isNotBlank() || licFilter != 0) "Effacer les filtres" else null,
                        onAction = { licQuery = ""; licFilter = 0 }
                    )
                }
            } else {
                items(filteredLicenses, key = { it.id }) { lic ->
                    LicenseKeyCard(
                        license = lic,
                        onClick = { selectedLicForDetail = lic }
                    )
                }
            }
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

    selectedLicForDetail?.let { lic ->
        LicenseKeyDetailSheet(
            license = lic,
            onDismiss = { selectedLicForDetail = null }
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

