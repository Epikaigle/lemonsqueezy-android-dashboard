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
fun StoreChips(snapshot: Snapshot, selected: String, onSelect: (String) -> Unit) {
    if (snapshot.stores.size <= 1) return
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
