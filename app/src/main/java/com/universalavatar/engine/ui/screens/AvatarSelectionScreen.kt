package com.universalavatar.engine.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalavatar.engine.model.AvatarPresets
import com.universalavatar.engine.model.AvatarStyle
import com.universalavatar.engine.R

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                    AVATAR SELECTION SCREEN                                   ║
 * ║                                                                              ║
 * ║  Allows users to select and customize their AR avatar.                       ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSelectionScreen() {
    var selectedStyle by remember { mutableStateOf(AvatarStyle.REALISTIC) }
    var selectedPreset by remember { mutableStateOf<String?>(null) }
    var showStyleSelector by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Choose Your Avatar",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Select a style and customize your digital identity",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Current Avatar Preview
        AvatarPreviewCard(
            style = selectedStyle,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Style Selector
        Text(
            text = "Avatar Style",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        StyleSelectorRow(
            selectedStyle = selectedStyle,
            onStyleSelected = { selectedStyle = it },
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Presets Grid
        Text(
            text = "${selectedStyle.getDisplayName()} Presets",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        PresetsGrid(
            style = selectedStyle,
            selectedPreset = selectedPreset,
            onPresetSelected = { selectedPreset = it },
            modifier = Modifier.weight(1f)
        )
        
        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { /* Upload custom avatar */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload")
            }
            
            Button(
                onClick = { /* Apply selected avatar */ },
                modifier = Modifier.weight(1f),
                enabled = selectedPreset != null
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply")
            }
        }
    }
}

/**
 * Avatar preview card.
 */
@Composable
fun AvatarPreviewCard(
    style: AvatarStyle,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 4.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (style) {
                            AvatarStyle.REALISTIC -> Icons.Default.Person
                            AvatarStyle.ANIME -> Icons.Default.Face
                            AvatarStyle.CARTOON -> Icons.Default.SentimentVerySatisfied
                            AvatarStyle.ROBOT -> Icons.Default.SmartToy
                            AvatarStyle.METAHUMAN -> Icons.Default.Star
                        },
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = style.getDisplayName(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = style.getDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

/**
 * Style selector row.
 */
@Composable
fun StyleSelectorRow(
    selectedStyle: AvatarStyle,
    onStyleSelected: (AvatarStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AvatarStyle.values().forEach { style ->
            StyleChip(
                style = style,
                isSelected = style == selectedStyle,
                onClick = { onStyleSelected(style) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Style chip composable.
 */
@Composable
fun StyleChip(
    style: AvatarStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = style.getDisplayName(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Presets grid composable.
 */
@Composable
fun PresetsGrid(
    style: AvatarStyle,
    selectedPreset: String?,
    onPresetSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = remember(style) {
        AvatarPresets.getPresetsForStyle(style)
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(presets) { preset ->
            PresetCard(
                presetName = preset.name,
                presetDescription = preset.description,
                isSelected = preset.id == selectedPreset,
                onClick = { onPresetSelected(preset.id) }
            )
        }
    }
}

/**
 * Preset card composable.
 */
@Composable
fun PresetCard(
    presetName: String,
    presetDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Preset thumbnail placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = presetName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = presetDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
