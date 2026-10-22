@file:Suppress("DEPRECATION")
package com.epikaigle.lemondashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun SalesScreen(snapshot: Snapshot, sales: List<Sale>, s: UiState, vm: DashboardViewModel) {
    val focusManager = LocalFocusManager.current
    val today = remember { LocalDate.now() }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var sortByAmountDesc by remember { mutableStateOf(false) }
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
    val sortedRows = remember(rows, sortByAmountDesc) {
        if (sortByAmountDesc) {
            rows.sortedByDescending { it.netUsd }
        } else {
            rows.sortedByDescending { it.createdAt }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Transactions", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
                placeholder = { Text("Rechercher un produit, référence...", color = TextMuted, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (filter == i) FontWeight.Bold else FontWeight.Normal) },
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${sortedRows.size} résultat(s)", color = TextSecondary, fontSize = 12.sp)
                Surface(
                    color = if (sortByAmountDesc) LemonContainer else DarkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (sortByAmountDesc) Lemon else DarkBorder),
                    modifier = Modifier.clickable { sortByAmountDesc = !sortByAmountDesc }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = if (sortByAmountDesc) Lemon else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (sortByAmountDesc) "Montant ↓" else "Date ↓",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (sortByAmountDesc) Lemon else TextSecondary
                        )
                    }
                }
            }
        }

        if (sortedRows.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.ReceiptLong,
                    title = if (inPeriod.isEmpty()) "Aucune transaction" else "Aucun résultat",
                    message = if (inPeriod.isEmpty()) "Sélectionnez une période plus large."
                    else "Modifiez les filtres de recherche.",
                    actionText = if (query.isNotBlank() || filter != 0) "Effacer les filtres" else null,
                    onAction = { query = ""; filter = 0 }
                )
            }
        } else {
            items(sortedRows, key = { it.id }) { sale ->
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sale.source == "Commande") MintSuccessContainer.copy(alpha = 0.5f) else LemonContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (sale.source == "Commande") Icons.Default.ShoppingBag else Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = if (sale.source == "Commande") MintSuccessLight else Lemon,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = sale.product,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${sale.source} • ${friendlyDate(sale.createdAt)}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                if (sale.testMode) {
                    Text("MODE TEST", fontSize = 9.sp, color = Lemon, fontWeight = FontWeight.Bold)
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
                    fontSize = 14.sp,
                    color = if (sale.paid) TextPrimary else TextMuted
                )
                if (!sale.paid || sale.currency.uppercase() != currency.uppercase()) {
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
}
