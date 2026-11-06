package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccessoryStyle
import com.example.model.AvatarCustomization
import com.example.model.CostumeStyle
import com.example.model.CustomizationCatalog
import com.example.model.EarStyle
import com.example.model.EyeStyle
import com.example.model.RoomTheme

enum class WardrobeTab(val title: String, val icon: String) {
    COSTUME("Kostum", "🧥"),
    EYES("Mata & Ekspresi", "👀"),
    EARS("Telinga", "👂"),
    FUR("Warna Bulu", "🎨"),
    ACCESSORIES("Aksesoris", "✨"),
    ROOM("Ruangan", "🏠")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WardrobeSheet(
    sheetState: SheetState,
    customization: AvatarCustomization,
    onCustomizationChanged: (AvatarCustomization) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(WardrobeTab.COSTUME) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("wardrobe_modal_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = "Kustomisasi Avatar",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Pilih gaya visual sesuai keinginanmu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_wardrobe_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            // Tab chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(WardrobeTab.entries) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text("${tab.icon} ${tab.title}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("wardrobe_tab_${tab.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(260.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    WardrobeTab.COSTUME -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CostumeStyle.entries.forEach { costume ->
                                val isSelected = customization.costume == costume
                                SelectionCard(
                                    title = costume.displayName,
                                    icon = costume.icon,
                                    isSelected = isSelected,
                                    onClick = {
                                        onCustomizationChanged(customization.copy(costume = costume))
                                    },
                                    modifier = Modifier.testTag("costume_item_${costume.id}")
                                )
                            }
                        }
                    }

                    WardrobeTab.EYES -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            EyeStyle.entries.forEach { eye ->
                                val isSelected = customization.eyeStyle == eye
                                SelectionCard(
                                    title = eye.displayName,
                                    icon = eye.icon,
                                    isSelected = isSelected,
                                    onClick = {
                                        onCustomizationChanged(customization.copy(eyeStyle = eye))
                                    },
                                    modifier = Modifier.testTag("eye_style_${eye.id}")
                                )
                            }
                        }
                    }

                    WardrobeTab.EARS -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            EarStyle.entries.forEach { ear ->
                                val isSelected = customization.earStyle == ear
                                SelectionCard(
                                    title = ear.displayName,
                                    icon = ear.icon,
                                    isSelected = isSelected,
                                    onClick = {
                                        onCustomizationChanged(customization.copy(earStyle = ear))
                                    },
                                    modifier = Modifier.testTag("ear_style_${ear.id}")
                                )
                            }
                        }
                    }

                    WardrobeTab.FUR -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CustomizationCatalog.furColors.forEach { opt ->
                                val isSelected = customization.furColorId == opt.id
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            onCustomizationChanged(
                                                customization.copy(furColor = opt.color, furColorId = opt.id)
                                            )
                                        }
                                        .padding(8.dp)
                                        .testTag("fur_color_${opt.id}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(opt.color)
                                            .border(
                                                width = if (isSelected) 3.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = CircleShape
                                            )
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Dipilih",
                                                tint = if (opt.color == Color(0xFFFBFBFA) || opt.color == Color(0xFFF7EBE1)) Color.Black else Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = opt.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    WardrobeTab.ACCESSORIES -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AccessoryStyle.entries.forEach { acc ->
                                val isSelected = customization.accessory == acc
                                SelectionCard(
                                    title = acc.displayName,
                                    icon = acc.icon,
                                    isSelected = isSelected,
                                    onClick = {
                                        onCustomizationChanged(customization.copy(accessory = acc))
                                    },
                                    modifier = Modifier.testTag("accessory_${acc.id}")
                                )
                            }
                        }
                    }

                    WardrobeTab.ROOM -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RoomTheme.entries.forEach { room ->
                                val isSelected = customization.roomTheme == room
                                SelectionCard(
                                    title = room.displayName,
                                    icon = room.icon,
                                    isSelected = isSelected,
                                    onClick = {
                                        onCustomizationChanged(customization.copy(roomTheme = room))
                                    },
                                    modifier = Modifier.testTag("room_theme_${room.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionCard(
    title: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
            .width(148.dp)
            .height(72.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = icon, fontSize = 26.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 2
            )
        }
    }
}
