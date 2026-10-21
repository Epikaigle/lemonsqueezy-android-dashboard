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
fun SettingsScreen(snapshot: Snapshot, s: UiState, vm: DashboardViewModel) {
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    var showFeeCalculator by remember { mutableStateOf(false) }
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

        // Application Updates
        AppUpdateCard(vm, s)

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

        // Export / Share Summary
        DashboardCard {
            Text("Partage du bilan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Exportez un récapitulatif synthétique de vos indicateurs clés (revenu net, MRR, commandes) sans aucune donnée personnelle ou confidentielle.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val mrr = Analytics.mrrEstimateUsdCents(snapshot.subscriptions)
                    val paidSales = snapshot.sales.filter { it.paid }
                    val totalNet = paidSales.sumOf { it.netUsd }
                    val activeSubs = snapshot.subscriptions.count { it.status == "active" }
                    val storeNames = snapshot.stores.joinToString { it.name }.ifBlank { "Toutes" }
                    val summaryText = buildString {
                        appendLine("📊 Lemon Squeezy — Bilan d'activité")
                        appendLine("• Boutiques synchronisées : $storeNames")
                        appendLine("• Revenu net cumulé : ${Analytics.money(totalNet, s.currency, snapshot.eurPerUsd)}")
                        appendLine("• Transactions payées : ${paidSales.size}")
                        appendLine("• Abonnements actifs : $activeSubs")
                        appendLine("• MRR estimé : ${Analytics.money(mrr, s.currency, snapshot.eurPerUsd)}")
                        appendLine("• Synchronisé le : ${friendlyDateTime(snapshot.fetchedAt)}")
                    }
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Bilan Lemon Squeezy")
                        putExtra(Intent.EXTRA_TEXT, summaryText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Partager le bilan"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Lemon.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Lemon, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Partager le récapitulatif", color = Lemon, fontWeight = FontWeight.Bold)
            }
        }

        // SaaS Tools & Accounting Export
        DashboardCard {
            Text("Outils Vendeur SaaS & Comptabilité", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Simulateur de commissions Lemon Squeezy MoR et export complet de vos transactions au format CSV standard pour votre comptabilité.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = { showFeeCalculator = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Lemon.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = Lemon, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Simulateur de Frais & Marge MoR", color = Lemon, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    try {
                        val csvData = Analytics.generateCsvExport(snapshot.sales, s.currency, snapshot.eurPerUsd)
                        val cacheFile = File(context.cacheDir, "lemon_transactions_${LocalDate.now()}.csv")
                        cacheFile.writeText(csvData, Charsets.UTF_8)

                        val contentUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            cacheFile
                        )

                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            putExtra(Intent.EXTRA_SUBJECT, "Export transactions Lemon Squeezy (${LocalDate.now()})")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Exporter le fichier CSV"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Erreur lors de l'export CSV : ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Exporter les transactions en CSV", fontWeight = FontWeight.Bold)
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

    if (showFeeCalculator) {
        FeeCalculatorDialog(
            currency = s.currency,
            eurPerUsd = snapshot.eurPerUsd,
            onDismiss = { showFeeCalculator = false }
        )
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
fun AppUpdateCard(vm: DashboardViewModel, s: UiState) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    DashboardCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = Lemon,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Mise à jour de l'app", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            }
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    color = Lemon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Vérification et installation automatique des dernières versions publiées sur GitHub.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(Modifier.height(12.dp))

        when (val status = s.updateStatus) {
            is UpdateStatus.Idle -> {
                OutlinedButton(
                    onClick = { vm.checkForUpdate(isManual = true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Lemon.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Lemon, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Rechercher une mise à jour", color = Lemon, fontWeight = FontWeight.Bold)
                }
            }
            is UpdateStatus.Checking -> {
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Lemon,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Recherche sur GitHub Releases…",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            is UpdateStatus.UpToDate -> {
                Surface(
                    color = MintSuccessContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintSuccessLight, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Votre application est à jour (v${status.latestVersion})",
                            color = MintSuccessLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vm.checkForUpdate(isManual = true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Revérifier", color = TextSecondary, fontSize = 12.sp)
                }
            }
            is UpdateStatus.Available -> {
                val info = status.info
                Surface(
                    color = Lemon.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Lemon.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NewReleases, contentDescription = null, tint = Lemon, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Version v${info.latestVersion} disponible",
                                color = Lemon,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        if (info.releaseNotes.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = info.releaseNotes.take(160) + if (info.releaseNotes.length > 160) "…" else "",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Taille du fichier : ${AppUpdateManager.formatFileSize(info.apkSize)}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.startDownloadUpdate(context, info) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Télécharger la version v${info.latestVersion}", fontWeight = FontWeight.Bold)
                }
            }
            is UpdateStatus.Downloading -> {
                val info = status.info
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Téléchargement de la v${info.latestVersion}…",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            "${(status.progress * 100).toInt()}%",
                            color = Lemon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Lemon,
                        trackColor = DarkSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${AppUpdateManager.formatFileSize(status.downloadedBytes)} / ${AppUpdateManager.formatFileSize(status.totalBytes)}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
            is UpdateStatus.ReadyToInstall -> {
                Surface(
                    color = MintSuccessContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MintSuccess.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintSuccessLight, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Mise à jour v${status.info.latestVersion} prête",
                                color = MintSuccessLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Le fichier d'installation est téléchargé. Appuyez pour lancer l'installation.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (AppUpdateManager.canRequestPackageInstalls(context)) {
                            AppUpdateManager.installApk(context, status.apkFile)
                        } else {
                            showPermissionDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground)
                ) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Installer maintenant", fontWeight = FontWeight.Bold)
                }
            }
            is UpdateStatus.Error -> {
                Surface(
                    color = CoralDangerContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CoralDangerLight, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(status.message, color = CoralDangerLight, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vm.checkForUpdate(isManual = true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CoralDanger.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CoralDangerLight, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Réessayer", color = CoralDangerLight, fontSize = 12.sp)
                }
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Autorisation requise", fontWeight = FontWeight.Bold) },
            text = {
                Text("Android nécessite que vous autorisiez Lemon Dashboard à installer des applications. Cliquez sur « Ouvrir les paramètres » et activez « Autoriser cette source ».")
            },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        context.startActivity(AppUpdateManager.createInstallPermissionIntent(context))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground)
                ) {
                    Text("Ouvrir les paramètres", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
    }
}

