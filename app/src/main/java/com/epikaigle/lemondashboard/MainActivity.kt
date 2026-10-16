package com.epikaigle.lemondashboard

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Lemon = Color(0xFFF6CD61)
private val Leaf = Color(0xFF8AE1C2)
private val Ink = Color(0xFF0D1516)
private val Panel = Color(0xFF1A2627)
private val Muted = Color(0xFF9FB2B1)
private val Danger = Color(0xFFFFADAD)
private val themeColors = darkColorScheme(
    primary = Lemon, onPrimary = Ink, secondary = Leaf,
    background = Ink, onBackground = Color(0xFFE8F4F1),
    surface = Panel, onSurface = Color(0xFFE8F4F1),
    surfaceVariant = Color(0xFF273635), onSurfaceVariant = Muted,
    error = Danger
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            MaterialTheme(colorScheme = themeColors) {
                val vm: DashboardViewModel = viewModel()
                LemonApp(vm)
            }
        }
    }
}

@Composable
private fun LemonApp(vm: DashboardViewModel) {
    val s = vm.state
    Scaffold(
        containerColor = Ink,
        topBar = {
            if (s.connected) Header(
                busy = s.loading,
                lastSync = s.snapshot?.fetchedAt.orEmpty(),
                onRefresh = vm::refresh
            )
        },
        bottomBar = {
            if (s.connected) {
                NavigationBar(containerColor = Panel) {
                    val tabs = listOf(
                        Triple("Accueil", Icons.Default.Dashboard, 0),
                        Triple("Ventes", Icons.Default.ReceiptLong, 1),
                        Triple("Abos", Icons.Default.Autorenew, 2),
                        Triple("Versements", Icons.Default.AccountBalanceWallet, 3),
                        Triple("Réglages", Icons.Default.Settings, 4)
                    )
                    tabs.forEach { (name, icon, page) ->
                        NavigationBarItem(
                            selected = s.tab == page, onClick = { vm.tab(page) },
                            icon = { Icon(icon, contentDescription = name) },
                            label = { Text(name, maxLines = 1, fontSize = 10.sp) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (s.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Lemon)
                if (s.progress.isNotBlank()) {
                    Text(s.progress, style = MaterialTheme.typography.labelSmall,
                        color = Muted, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
                }
            }
            if (s.error != null) {
                Surface(color = Color(0xFF482B2B), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.error, modifier = Modifier.weight(1f), color = Color.White,
                            style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = vm::clearError) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }
                }
            }
            if (!s.connected) {
                LoginScreen(loading = s.loading, onConnect = vm::connect)
            } else {
                val snapshot = s.snapshot
                if (snapshot == null) {
                    EmptyArea("Aucune donnée synchronisée", "Vérifie la connexion Internet et actualise le tableau de bord.",
                        vm::refresh)
                } else {
                    val filteredSales = remember(snapshot, s.selectedStore, s.includeTest) {
                        Analytics.selectedSales(snapshot, s.selectedStore, s.includeTest)
                    }
                    val filteredSubs = remember(snapshot, s.selectedStore, s.includeTest) {
                        Analytics.selectedSubscriptions(snapshot, s.selectedStore, s.includeTest)
                    }
                    when (s.tab) {
                        0 -> OverviewScreen(snapshot, filteredSales, filteredSubs, s, vm)
                        1 -> SalesScreen(snapshot, filteredSales, s, vm)
                        2 -> SubscriptionsScreen(snapshot, filteredSubs, s, vm)
                        3 -> PayoutScreen()
                        else -> SettingsScreen(snapshot, s, vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(busy: Boolean, lastSync: String, onRefresh: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 13.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Lemon),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Bolt, contentDescription = null, tint = Ink) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Lemon Dashboard", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text("Mis à jour : ${friendlyDate(lastSync)}", color = Muted, fontSize = 11.sp)
        }
        IconButton(onClick = onRefresh, enabled = !busy) {
            Icon(Icons.Default.Refresh, contentDescription = "Actualiser", tint = if (busy) Muted else Lemon)
        }
    }
}

@Composable
private fun LoginScreen(loading: Boolean, onConnect: (String) -> Unit) {
    var token by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(Lemon),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = Ink, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Vos ventes.\nEn un coup d'œil.", fontSize = 34.sp, lineHeight = 40.sp,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Consultez les commandes, les abonnements et les revenus Lemon Squeezy depuis votre téléphone.",
            color = Muted, lineHeight = 23.sp)
        Spacer(Modifier.height(34.dp))
        Text("CLÉ API LEMON SQUEEZY", color = Leaf, fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = token, onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Collez votre clé ici") },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (visible) "Masquer la clé" else "Afficher la clé")
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onConnect(token) }, enabled = token.isNotBlank() && !loading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(if (loading) "Connexion en cours…" else "Connecter ma boutique")
        }
        Spacer(Modifier.height(18.dp))
        Text("Votre clé est chiffrée sur le téléphone. Aucun serveur intermédiaire ni publicité.",
            color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        TextButton(onClick = { uri.openUri("https://app.lemonsqueezy.com/settings/api") }) {
            Text("Créer une clé API ↗")
        }
    }
}

@Composable
private fun OverviewScreen(snapshot: Snapshot, sales: List<Sale>, subs: List<Subscription>,
                           s: UiState, vm: DashboardViewModel) {
    val now = remember { LocalDate.now() }
    val m = remember(sales, subs, s.period) { Analytics.metrics(sales, subs, s.period, now) }
    val history = remember(sales, s.period) { Analytics.graphBuckets(sales, s.period, now) }
    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Vue d'ensemble", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                StoreChips(snapshot, s.selectedStore, vm::store)
                Spacer(Modifier.height(12.dp))
                PeriodChips(s.period, vm::period)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF25524A), Panel)))
                .padding(22.dp)) {
                Text("REVENUS APRÈS REMBOURSEMENTS", color = Color(0xFFD7EDE2),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text(Analytics.money(m.netUsdCents, s.currency, snapshot.eurPerUsd),
                    fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text(if (s.currency == "EUR")
                    "Estimation EUR au dernier taux disponible, pas un versement"
                else "Total des paiements retenus en USD",
                    color = Color(0xFFBCD7CE), fontSize = 12.sp)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetric("Paiements", m.payments.toString(), Modifier.weight(1f))
                SmallMetric("Abos actifs", m.activeSubscriptions.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetric("Brut", Analytics.money(m.grossUsdCents, s.currency, snapshot.eurPerUsd),
                    Modifier.weight(1f))
                SmallMetric("Remboursé", Analytics.money(m.refundedUsdCents, s.currency, snapshot.eurPerUsd),
                    Modifier.weight(1f))
            }
        }
        item { ChartCard(history, s.currency, snapshot.eurPerUsd) }
        item {
            Column {
                SectionTitle("Dernières transactions", "${sales.size} enregistrements")
                Spacer(Modifier.height(10.dp))
                if (sales.isEmpty()) {
                    EmptyCard("Pas encore de ventes pour cette boutique.")
                } else {
                    sales.take(5).forEach { sale ->
                        SaleItem(sale, snapshot, s.currency)
                        Spacer(Modifier.height(8.dp))
                    }
                    TextButton(onClick = { vm.tab(1) }) {
                        Text("Voir toutes les transactions  →")
                    }
                }
            }
        }
        item { Explanation("Paiements des commandes et factures récurrentes non initiales. Les montants sont des ventes, pas des bénéfices ou virements bancaires.") }
    }
}

@Composable
private fun StoreChips(snapshot: Snapshot, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected.isEmpty(), onClick = { onSelect("") }, label = { Text("Toutes") })
        snapshot.stores.forEach { store ->
            FilterChip(selected = selected == store.id, onClick = { onSelect(store.id) },
                label = { Text(store.name, maxLines = 1) })
        }
    }
}

@Composable
private fun PeriodChips(selected: Period, onSelect: (Period) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Period.entries.forEach { period ->
            FilterChip(selected = selected == period, onClick = { onSelect(period) },
                label = { Text(period.title) })
        }
    }
}

@Composable
private fun SmallMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ChartCard(buckets: List<Pair<String, Long>>, currency: String, rate: Double?) {
    val maxValue = (buckets.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    Card(shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(18.dp)) {
            Text("Évolution du revenu net", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            Text("Par période • paiements moins remboursements", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(22.dp))
            Canvas(Modifier.fillMaxWidth().height(140.dp).semantics {
                contentDescription = "Graphique de revenus : ${buckets.size} périodes. Maximum ${Analytics.money(maxValue, currency, rate)}"
            }) {
                val count = buckets.size.coerceAtLeast(1)
                val cell = size.width / count
                buckets.forEachIndexed { index, pair ->
                    val fraction = (pair.second.toFloat() / maxValue).coerceIn(0f, 1f)
                    val barHeight = (fraction * (size.height - 10f)).coerceAtLeast(if (pair.second > 0) 3f else 1f)
                    drawRoundRect(
                        color = if (index == buckets.lastIndex) Lemon else Leaf.copy(alpha = 0.75f),
                        topLeft = Offset(index * cell + cell * 0.19f, size.height - barHeight),
                        size = Size(cell * 0.62f, barHeight),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(buckets.firstOrNull()?.first ?: "", color = Muted, fontSize = 11.sp)
                Text(buckets.getOrNull(buckets.size / 2)?.first ?: "", color = Muted, fontSize = 11.sp)
                Text(buckets.lastOrNull()?.first ?: "", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SalesScreen(snapshot: Snapshot, sales: List<Sale>, s: UiState, vm: DashboardViewModel) {
    val today = remember { LocalDate.now() }
    val rows = remember(sales, s.period) {
        sales.filter { Analytics.inPeriod(Analytics.dateOf(it.createdAt), s.period, today) }
    }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Transactions", fontSize = 27.sp, fontWeight = FontWeight.Bold) }
        item { StoreChips(snapshot, s.selectedStore, vm::store) }
        item { PeriodChips(s.period, vm::period) }
        item { Text("${rows.size} transactions sur la période", color = Muted, fontSize = 13.sp) }
        if (rows.isEmpty()) {
            item { EmptyCard("Aucune transaction pour cette période.") }
        } else {
            items(rows, key = { it.id }) { sale -> SaleItem(sale, snapshot, s.currency) }
        }
    }
}

@Composable
private fun SaleItem(sale: Sale, snapshot: Snapshot, currency: String) {
    val originalNet = (sale.originalCents - sale.originalRefundCents).coerceAtLeast(0)
    Card(colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF2A4140)), contentAlignment = Alignment.Center) {
                Icon(if (sale.source == "Commande") Icons.Default.ShoppingBag else Icons.Default.Autorenew,
                    contentDescription = null, tint = Leaf)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sale.product, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text("${sale.source} • ${friendlyDate(sale.createdAt)}", fontSize = 11.sp, color = Muted)
                if (sale.testMode) Text("MODE TEST", fontSize = 10.sp, color = Lemon)
                if (!sale.paid) Text(sale.status, fontSize = 11.sp, color = Danger)
            }
            Spacer(Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(Analytics.money(sale.netUsd, currency, snapshot.eurPerUsd),
                    fontWeight = FontWeight.Bold)
                Text(Analytics.localMoney(originalNet, sale.currency), color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SubscriptionsScreen(snapshot: Snapshot, subscriptions: List<Subscription>,
                                s: UiState, vm: DashboardViewModel) {
    val counts = subscriptions.groupingBy { it.status }.eachCount()
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Abonnements", fontWeight = FontWeight.Bold, fontSize = 27.sp) }
        item { StoreChips(snapshot, s.selectedStore, vm::store) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetric("Actifs", (counts["active"] ?: 0).toString(), Modifier.weight(1f))
                SmallMetric("En essai", (counts["on_trial"] ?: 0).toString(), Modifier.weight(1f))
                SmallMetric("Autres", (subscriptions.size - (counts["active"] ?: 0) - (counts["on_trial"] ?: 0)).toString(),
                    Modifier.weight(1f))
            }
        }
        item { Explanation("Le montant montré est le dernier paiement connu, pas une promesse de prochain débit ni un MRR contractuel.") }
        if (subscriptions.isEmpty()) {
            item { EmptyCard("Aucun abonnement pour cette boutique.") }
        } else {
            items(subscriptions, key = { it.id }) { sub -> SubscriptionItem(sub, snapshot, s.currency) }
        }
    }
}

@Composable
private fun SubscriptionItem(sub: Subscription, snapshot: Snapshot, currency: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sub.product.ifBlank { "Abonnement" }, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), maxLines = 2)
                StatusChip(sub.status)
            }
            if (sub.variant.isNotBlank()) {
                Text(sub.variant, color = Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text("Prochain renouvellement : ${friendlyDate(sub.renewsAt)}", color = Muted, fontSize = 12.sp)
            if (sub.endsAt.isNotBlank()) {
                Text("Fin d'accès : ${friendlyDate(sub.endsAt)}", color = Muted, fontSize = 12.sp)
            }
            if (sub.lastInvoiceUsdCents != null) {
                Text("Dernière facture payée : ${Analytics.money(sub.lastInvoiceUsdCents, currency, snapshot.eurPerUsd)}",
                    fontSize = 13.sp)
            } else {
                Text("Dernière facture : non disponible", color = Muted, fontSize = 12.sp)
            }
            if (sub.testMode) Text("MODE TEST", color = Lemon, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val label = when (status) {
        "active" -> "Actif"
        "on_trial" -> "Essai"
        "cancelled" -> "Annulé"
        "expired" -> "Expiré"
        "past_due" -> "En retard"
        "unpaid" -> "Impayé"
        "paused" -> "En pause"
        else -> status
    }
    val tint = if (status == "active") Leaf else if (status == "on_trial") Lemon else Muted
    Surface(color = tint.copy(alpha = 0.13f), shape = RoundedCornerShape(100.dp)) {
        Text(label, fontSize = 11.sp, color = tint, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
    }
}

@Composable
private fun PayoutScreen() {
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Versements", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(22.dp)) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null,
                    modifier = Modifier.size(42.dp), tint = Lemon)
                Spacer(Modifier.height(16.dp))
                Text("Versements confirmés : non disponibles",
                    fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("L'API publique Lemon Squeezy ne fournit pas de registre complet des virements réels. " +
                    "Nous n'affichons donc pas de faux solde, de fausse date ni de faux montant à recevoir.",
                    color = Muted, lineHeight = 22.sp)
                Spacer(Modifier.height(18.dp))
                Button(onClick = { uri.openUri("https://app.lemonsqueezy.com/") }) {
                    Text("Consulter les versements officiels ↗")
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Explanation("Le chiffre d'affaires total n'est pas le montant à recevoir : frais, taxes, retenues, remboursement et calendrier bancaire peuvent modifier le versement final.")
    }
}

@Composable
private fun SettingsScreen(snapshot: Snapshot, s: UiState, vm: DashboardViewModel) {
    var confirm by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Réglages", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        SettingCard("Boutiques") { StoreChips(snapshot, s.selectedStore, vm::store) }
        SettingCard("Devise affichée") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = s.currency == "USD", onClick = { vm.currency("USD") }, label = { Text("USD $") })
                FilterChip(selected = s.currency == "EUR", onClick = { vm.currency("EUR") }, label = { Text("EUR €") })
            }
            Text(
                if (s.currency == "EUR") {
                    if (snapshot.eurPerUsd == null) "Taux EUR indisponible : les valeurs restent en USD."
                    else "Conversion indicative au taux du ${snapshot.fxDate} (1 USD = ${snapshot.eurPerUsd} EUR)."
                } else "Montants convertis en USD par Lemon Squeezy à la transaction.",
                color = Muted, fontSize = 12.sp
            )
        }
        SettingCard("Mode test") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Inclure les commandes de test", modifier = Modifier.weight(1f))
                Switch(checked = s.includeTest, onCheckedChange = vm::tests)
            }
        }
        SettingCard("Données et confidentialité") {
            Text("Dernière synchronisation : ${friendlyDate(snapshot.fetchedAt)}", color = Muted)
            Text("Cache local chiffré, capture d'écran bloquée et sauvegarde Android désactivée.",
                fontSize = 12.sp, color = Muted)
            Text("Clé API stockée sur cet appareil uniquement.", fontSize = 12.sp, color = Muted)
            TextButton(onClick = { uri.openUri("https://docs.lemonsqueezy.com/api") }) {
                Text("Documentation de l'API ↗")
            }
            OutlinedButton(onClick = { confirm = true }) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(Modifier.width(9.dp))
                Text("Effacer la clé et toutes les données")
            }
        }
        Explanation("Application personnelle et indépendante, non affiliée à Lemon Squeezy. Lecture seule : aucune modification de commande ou d'abonnement.")
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Tout supprimer ?") },
            text = { Text("La clé API et le cache local seront effacés de ce téléphone. Les données chez Lemon Squeezy ne seront pas modifiées.") },
            confirmButton = {
                TextButton(onClick = { confirm = false; vm.disconnect() }) { Text("Supprimer", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun SettingCard(title: String, body: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            body()
        }
    }
}

@Composable
private fun SectionTitle(title: String, hint: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(hint, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun Explanation(value: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(Color(0xFF213334)).padding(14.dp)) {
        Icon(Icons.Default.Info, contentDescription = null, tint = Leaf, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(value, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun EmptyCard(message: String) {
    Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = Muted, modifier = Modifier.padding(20.dp))
    }
}

@Composable
private fun EmptyArea(title: String, message: String, retry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(30.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(56.dp), tint = Muted)
        Spacer(Modifier.height(14.dp))
        Text(title, fontWeight = FontWeight.Bold)
        Text(message, color = Muted)
        Spacer(Modifier.height(14.dp))
        Button(onClick = retry) { Text("Réessayer") }
    }
}

private fun friendlyDate(iso: String): String {
    val day = Analytics.dateOf(iso) ?: return "indisponible"
    return day.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRANCE))
}
