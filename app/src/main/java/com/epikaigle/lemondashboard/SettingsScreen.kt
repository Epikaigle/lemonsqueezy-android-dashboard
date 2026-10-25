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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Réglages", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        // Store Selector
        if (snapshot.stores.size > 1) {
            DashboardCard {
                Text("Boutiques actives", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                StoreChips(snapshot, s.selectedStore, vm::store)
            }
        }

        // Currency Preference
        DashboardCard {
            Text("Devise d'affichage", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = s.currency == "USD",
                    onClick = { vm.currency("USD") },
                    label = { Text("USD ($)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
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
                    label = { Text("EUR (€)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Lemon,
                        selectedLabelColor = DarkBackground,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextSecondary
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
            DetailRow("Dernière synchronisation", friendlyDateTime(snapshot.fetchedAt))
            DetailRow("Dernier rapprochement complet", friendlyDateTime(snapshot.lastFullSyncAt))
            DetailRow("Commandes en cache", "${snapshot.sales.size} transactions")
            DetailRow("Abonnements en cache", "${snapshot.subscriptions.size} abonnements")

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = vm::refreshFull,
                enabled = !s.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Lemon.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = Lemon, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Rapprocher tout l'historique", color = Lemon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Export / Share Summary
        DashboardCard {
            Text("Partage du bilan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Lemon.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Lemon, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Partager le récapitulatif", color = Lemon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // SaaS Tools & Accounting Export
        DashboardCard {
            Text("Outils & Comptabilité", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showFeeCalculator = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Lemon.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = Lemon, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Simulateur de Frais MoR", color = Lemon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Exporter en CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Security & Disconnect
        DashboardCard {
            Text("Zone de déconnexion", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { confirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralDangerContainer)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = CoralDangerLight, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Effacer la clé et se déconnecter", color = CoralDangerLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
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
            title = { Text("Se déconnecter ?", fontWeight = FontWeight.Bold) },
            text = { Text("La clé API et le cache local seront supprimés de cet appareil.") },
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
    var showAdvanced by remember { mutableStateOf(false) }
    var origin by remember { mutableStateOf(s.pushOrigin.ifBlank { PushNotifications.DEFAULT_ORIGIN }) }
    var pairingSecret by remember { mutableStateOf(PushNotifications.DEFAULT_SECRET) }
    var reveal by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.enablePush(origin, pairingSecret)
    }

    DashboardCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Lemon,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Notifications instantanées", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            }
            if (s.pushEnabled) {
                Surface(
                    color = MintSuccessContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        "Activées",
                        color = MintSuccessLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (!PushNotifications.available(context)) {
            Surface(
                color = CoralDangerContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CoralDangerLight, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Firebase non configuré dans ce build.",
                        color = CoralDangerLight,
                        fontSize = 12.sp
                    )
                }
            }
        } else if (s.pushEnabled) {
            Surface(
                color = MintSuccessContainer.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintSuccessLight, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Alertes en direct actives à chaque vente ou abonnement.",
                        color = MintSuccessLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = vm::disablePush,
                enabled = !s.pushBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CoralDanger.copy(alpha = 0.5f))
            ) {
                Text("Désactiver les alertes", color = CoralDangerLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                "Soyez alerté en direct avec le montant et le produit dès qu'une vente a lieu.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    if (PushNotifications.permissionGranted(context)) {
                        vm.enablePush(origin, pairingSecret)
                    } else {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                enabled = !s.pushBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (s.pushBusy) "Connexion…" else "Activer les notifications",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        if (s.pushMessage.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = s.pushMessage,
                color = if (s.pushEnabled) MintSuccessLight else CoralDangerLight,
                fontSize = 11.sp,
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
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Mise à jour de l'app", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            }
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    color = Lemon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        when (val status = s.updateStatus) {
            is UpdateStatus.Idle -> {
                OutlinedButton(
                    onClick = { vm.checkForUpdate(isManual = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Lemon.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Lemon, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Vérifier les mises à jour", color = Lemon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            is UpdateStatus.Checking -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Lemon, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Recherche en cours…", color = TextSecondary, fontSize = 12.sp)
                }
            }
            is UpdateStatus.UpToDate -> {
                Surface(
                    color = MintSuccessContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintSuccessLight, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Votre application est à jour (v${status.latestVersion})",
                            color = MintSuccessLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vm.checkForUpdate(isManual = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Revérifier", color = TextSecondary, fontSize = 12.sp)
                }
            }
            is UpdateStatus.Available -> {
                val info = status.info
                Surface(
                    color = LemonContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NewReleases, contentDescription = null, tint = Lemon, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Nouvelle version v${info.latestVersion} disponible !", color = Lemon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(AppUpdateManager.formatFileSize(info.apkSize), color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (AppUpdateManager.canRequestPackageInstalls(context)) {
                            vm.startDownloadUpdate(context, info)
                        } else {
                            showPermissionDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mettre à jour maintenant", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            is UpdateStatus.Downloading -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Téléchargement…", fontSize = 12.sp, color = TextPrimary)
                        Text("${(status.progress * 100).toInt()}%", fontSize = 12.sp, color = Lemon, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Lemon,
                        trackColor = DarkSurfaceVariant
                    )
                }
            }
            is UpdateStatus.ReadyToInstall -> {
                Button(
                    onClick = {
                        if (AppUpdateManager.canRequestPackageInstalls(context)) {
                            AppUpdateManager.installApk(context, status.apkFile)
                        } else {
                            showPermissionDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground)
                ) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Installer la mise à jour", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            is UpdateStatus.Error -> {
                Surface(
                    color = CoralDangerContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CoralDangerLight, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(status.message, color = CoralDangerLight, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vm.checkForUpdate(isManual = true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Lemon.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Lemon, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Réessayer", color = Lemon, fontSize = 12.sp)
                }
            }
        }

        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Autorisation requise", fontWeight = FontWeight.Bold) },
                text = { Text("Pour mettre à jour automatiquement l'application, Android nécessite l'autorisation d'installer des packages inconnus.") },
                containerColor = DarkSurface,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionDialog = false
                            context.startActivity(AppUpdateManager.createInstallPermissionIntent(context))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground),
                        shape = RoundedCornerShape(8.dp)
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
}
