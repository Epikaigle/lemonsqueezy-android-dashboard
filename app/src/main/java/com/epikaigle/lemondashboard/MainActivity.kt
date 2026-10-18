package com.epikaigle.lemondashboard

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val vm: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!BuildConfig.DEBUG) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        PushNotifications.createChannel(this)
        handleNotificationIntent(intent)
        setContent {
            MaterialTheme(
                colorScheme = LemonDashboardColorScheme,
                shapes = AppShapes
            ) {
                LemonApp(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val targetTab = intent?.getIntExtra("navigate_to_tab", -1) ?: -1
        if (targetTab in 0..4) {
            vm.tab(targetTab)
        }
    }
}

@Composable
private fun LemonApp(vm: DashboardViewModel) {
    val s = vm.state

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            if (s.connected) {
                Header(
                    busy = s.loading,
                    lastSync = s.snapshot?.fetchedAt.orEmpty(),
                    onRefresh = vm::refresh
                )
            }
        },
        bottomBar = {
            if (s.connected) {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 8.dp
                ) {
                    val tabs = listOf(
                        NavigationItem("Accueil", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, 0),
                        NavigationItem("Ventes", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, 1),
                        NavigationItem("Abos", Icons.Filled.Autorenew, Icons.Outlined.Autorenew, 2),
                        NavigationItem("Versements", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, 3),
                        NavigationItem("Réglages", Icons.Filled.Settings, Icons.Outlined.Settings, 4)
                    )
                    tabs.forEach { item ->
                        val isSelected = s.tab == item.page
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { vm.tab(item.page) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.name
                                )
                            },
                            label = {
                                Text(
                                    text = item.name,
                                    maxLines = 1,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkBackground,
                                indicatorColor = Lemon,
                                selectedTextColor = Lemon,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Loading Progress Bar
            AnimatedVisibility(
                visible = s.loading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Lemon,
                        trackColor = DarkSurfaceVariant
                    )
                    if (s.progress.isNotBlank()) {
                        Surface(color = DarkSurfaceVariant, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = s.progress,
                                style = MaterialTheme.typography.labelSmall,
                                color = Lemon,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Error Callout Banner
            AnimatedVisibility(
                visible = s.error != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = CoralDangerContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = CoralDangerLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = s.error.orEmpty(),
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = vm::clearError, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Screen Content
            if (s.initializing) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Lemon, strokeWidth = 3.dp)
                        Spacer(Modifier.height(18.dp))
                        Text("Ouverture du cache local sécurisé…", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else if (!s.connected) {
                LoginScreen(loading = s.loading, onConnect = vm::connect)
            } else {
                val snapshot = s.snapshot
                if (snapshot == null) {
                    EmptyArea(
                        title = "Aucune donnée synchronisée",
                        message = "Vérifiez votre connexion Internet et actualisez votre tableau de bord.",
                        retry = vm::refresh,
                        disconnect = vm::disconnect
                    )
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
                        3 -> PayoutScreen(snapshot, s.currency)
                        else -> SettingsScreen(snapshot, s, vm)
                    }
                }
            }
        }
    }
}

private data class NavigationItem(
    val name: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val page: Int
)

@Composable
private fun Header(busy: Boolean, lastSync: String, onRefresh: () -> Unit) {
    // Rotation animation when syncing
    val infiniteTransition = rememberInfiniteTransition(label = "refreshRotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Surface(
        color = DarkBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lemon Squircle App Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Lemon),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = DarkBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = "Lemon Dashboard",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Synchronisé le ${friendlyDateTime(lastSync)}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onRefresh,
                enabled = !busy,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualiser",
                    tint = if (busy) Lemon else TextSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .then(if (busy) Modifier.rotate(angle) else Modifier)
                )
            }
        }
    }
}

@Composable
private fun EmptyArea(title: String, message: String, retry: () -> Unit, disconnect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(40.dp), tint = TextMuted)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(message, color = TextSecondary, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = retry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Lemon, contentColor = DarkBackground)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Réessayer", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = disconnect) {
            Text("Effacer la clé et se déconnecter", color = CoralDangerLight)
        }
    }
}
