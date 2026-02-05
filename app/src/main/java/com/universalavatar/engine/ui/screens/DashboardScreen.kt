package com.universalavatar.engine.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         DASHBOARD SCREEN                                     ║
 * ║                                                                              ║
 * ║  Shows system status, performance metrics, and service controls.             ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "System Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Monitor and control the AR Avatar Engine",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Status Overview
        StatusOverviewCard(
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Performance Metrics
        PerformanceMetricsCard(
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Service Controls
        Text(
            text = "Service Controls",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        ServiceControlItem(
            icon = Icons.Default.Visibility,
            title = "System Watcher",
            description = "Monitors camera activation",
            isActive = true,
            onToggle = { }
        )
        
        ServiceControlItem(
            icon = Icons.Default.Layers,
            title = "AR Overlay",
            description = "Renders avatar overlay",
            isActive = false,
            onToggle = { }
        )
        
        ServiceControlItem(
            icon = Icons.Default.Face,
            title = "Face Tracking",
            description = "468-point face mesh",
            isActive = false,
            onToggle = { }
        )
        
        ServiceControlItem(
            icon = Icons.Default.AutoFixHigh,
            title = "Avatar Renderer",
            description = "Deepfake avatar generation",
            isActive = false,
            onToggle = { }
        )
        
        ServiceControlItem(
            icon = Icons.Default.Videocam,
            title = "Screen Share Bridge",
            description = "WebRTC virtual camera",
            isActive = false,
            onToggle = { }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick Actions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.PlayArrow,
                label = "Start All",
                onClick = { },
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.Stop,
                label = "Stop All",
                onClick = { },
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.Refresh,
                label = "Restart",
                onClick = { },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Status overview card.
 */
@Composable
fun StatusOverviewCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "System Online",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusItem(
                    label = "Active Services",
                    value = "2/5"
                )
                StatusItem(
                    label = "FPS",
                    value = "30"
                )
                StatusItem(
                    label = "Latency",
                    value = "32ms"
                )
            }
        }
    }
}

/**
 * Status item.
 */
@Composable
fun StatusItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * Performance metrics card.
 */
@Composable
fun PerformanceMetricsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            MetricBar(
                label = "CPU Usage",
                value = 0.45f,
                color = Color(0xFF2196F3)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            MetricBar(
                label = "GPU Usage",
                value = 0.62f,
                color = Color(0xFF9C27B0)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            MetricBar(
                label = "Memory",
                value = 0.38f,
                color = Color(0xFF4CAF50)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            MetricBar(
                label = "Battery",
                value = 0.78f,
                color = Color(0xFFFF9800)
            )
        }
    }
}

/**
 * Metric bar.
 */
@Composable
fun MetricBar(label: String, value: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = value,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

/**
 * Service control item.
 */
@Composable
fun ServiceControlItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

/**
 * Quick action button.
 */
@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp)
    }
}
