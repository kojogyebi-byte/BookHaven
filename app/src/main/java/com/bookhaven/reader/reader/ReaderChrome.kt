package com.bookhaven.reader.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReaderTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Library")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AaSettingsSheet(
    prefs: ReaderPreferences,
    currentTheme: PageTheme,
    fontScale: Int,
    onThemeChange: (PageTheme) -> Unit,
    onFontChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    showLayoutToggle: Boolean = true,
    paged: Boolean = true,
    onPagedChange: (Boolean) -> Unit = {}
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(20.dp).navigationBarsPadding()) {
            Text("Themes & Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(18.dp))

            // Font size stepper
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = fontScale.toFloat(),
                    onValueChange = { onFontChange(it.toInt()) },
                    valueRange = 70f..220f,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
                Text("A", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))

            // Page theme swatches
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PageTheme.entries.forEach { t ->
                    val selected = t == currentTheme
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(t.bg)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onThemeChange(t) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aa", color = t.text, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (showLayoutToggle) {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Paged layout", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = paged, onCheckedChange = onPagedChange)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
