package com.universalavatar.engine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.universalavatar.engine.model.AREffect
import com.universalavatar.engine.model.AREffectsCollection

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         EFFECTS SCREEN                                       ║
 * ║                                                                              ║
 * ║  Allows users to select and configure AR effects.                            ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectsScreen() {
    var selectedCategory by remember { mutableStateOf(EffectCategory.ALL) }
    var activeEffects by remember { mutableStateOf(setOf<String>()) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "AR Effects",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Enhance your avatar with filters and effects",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Category Chips
        EffectCategoryChips(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Effects List
        val effects = when (selectedCategory) {
            EffectCategory.ALL -> AREffectsCollection.ALL_EFFECTS
            EffectCategory.PARTICLES -> AREffectsCollection.PARTICLE_EFFECTS
            EffectCategory.FILTERS -> AREffectsCollection.FILTER_EFFECTS
            EffectCategory.LIGHTING -> AREffectsCollection.LIGHTING_EFFECTS
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(effects) { effect ->
                EffectCard(
                    effect = effect,
                    isActive = effect.id in activeEffects,
                    onToggle = {
                        activeEffects = if (effect.id in activeEffects) {
                            activeEffects - effect.id
                        } else {
                            activeEffects + effect.id
                        }
                    }
                )
            }
        }
    }
}

/**
 * Effect categories.
 */
enum class EffectCategory {
    ALL, PARTICLES, FILTERS, LIGHTING
}

/**
 * Category chips row.
 */
@Composable
fun EffectCategoryChips(
    selectedCategory: EffectCategory,
    onCategorySelected: (EffectCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EffectCategory.values().forEach { category ->
            CategoryChip(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Category chip.
 */
@Composable
fun CategoryChip(
    category: EffectCategory,
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
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = category.name.capitalize(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Effect card.
 */
@Composable
fun EffectCard(
    effect: AREffect,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Effect icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (effect.type) {
                            AREffect.Type.PARTICLE -> Color(0xFFFFD700).copy(alpha = 0.3f)
                            AREffect.Type.FILTER -> Color(0xFF9C27B0).copy(alpha = 0.3f)
                            AREffect.Type.LIGHTING -> Color(0xFF00BCD4).copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (effect.type) {
                        AREffect.Type.PARTICLE -> Icons.Default.Star
                        AREffect.Type.FILTER -> Icons.Default.Filter
                        AREffect.Type.OVERLAY -> Icons.Default.Image
                        AREffect.Type.ANIMATION -> Icons.Default.Animation
                        AREffect.Type.LIGHTING -> Icons.Default.WbSunny
                        AREffect.Type.DISTORTION -> Icons.Default.Waves
                        AREffect.Type.MASK -> Icons.Default.Face
                        AREffect.Type.BACKGROUND -> Icons.Default.Wallpaper
                    },
                    contentDescription = null,
                    tint = when (effect.type) {
                        AREffect.Type.PARTICLE -> Color(0xFFFFD700)
                        AREffect.Type.FILTER -> Color(0xFF9C27B0)
                        AREffect.Type.LIGHTING -> Color(0xFF00BCD4)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = effect.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = effect.type.name.capitalize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            // Toggle switch
            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

/**
 * Extension function to capitalize first letter.
 */
private fun String.capitalize(): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
