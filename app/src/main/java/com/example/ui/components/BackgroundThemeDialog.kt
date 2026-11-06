package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BackgroundTheme
import com.example.model.TimeOfDay

@Composable
fun BackgroundThemeDialog(
    currentTheme: BackgroundTheme,
    currentTimeOfDay: TimeOfDay,
    onSelectTheme: (BackgroundTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🏠 ", fontSize = 22.sp)
                Text(
                    text = "Pilih Ruangan Rumah Kartun",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Pilih ruangan rumah 3D ala Talking Tom yang ceria dan penuh mainan (Ruang Tamu, Kamar Tidur, Dapur).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    items(BackgroundTheme.values()) { theme ->
                        val isSelected = theme == currentTheme

                        val itemBrush = if (isSelected) {
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0x351F2A44),
                                    Color(0x201F2A44)
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(itemBrush)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    brush = if (isSelected) Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)) else LiquidGlassDefaults.SpecularBorderBrush,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectTheme(theme) }
                                .testTag("theme_option_${theme.id}")
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Theme preview color disc
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(theme.topColor, theme.bottomColor))
                                        )
                                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Text(text = theme.icon, fontSize = 20.sp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = theme.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        if (theme.isOrnate) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "HD Ornamen",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFFFFB703),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                    if (theme == BackgroundTheme.AUTO_REALTIME) {
                                        Text(
                                            text = "Aktif sekarang: ${currentTimeOfDay.displayName} ${currentTimeOfDay.icon}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Tema terpilih",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("confirm_theme_dialog_button")
            ) {
                Text("Selesai")
            }
        }
    )
}
