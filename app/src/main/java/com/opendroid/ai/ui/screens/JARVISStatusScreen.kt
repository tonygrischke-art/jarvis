package com.opendroid.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.opendroid.ai.ui.theme.*
import com.opendroid.ai.data.repository.SettingsRepository
import com.opendroid.ai.core.llm.ProviderCatalog
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.navigation.compose.hiltViewModel
import com.opendroid.ai.ui.viewmodel.SettingsViewModel

/**
 * JARVIS Status Screen - Shows agent status, connection, and quick actions
 */
@Composable
fun JARVISStatusScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val config by settingsViewModel.llmConfig.collectAsState()

    val isNimConfigured = config.apiKeys[ProviderCatalog.NIM]?.isNotBlank() == true
    val activeProvider = config.activeProvider

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "JARVIS",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.onPrimaryContainer
                        )
                        Text(
                            text = "Autonomous Form-Filling Agent",
                            fontSize = 14.sp,
                            color = AppTheme.colors.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "JARVIS",
                        tint = AppTheme.colors.onPrimaryContainer,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        // Connection Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CONNECTION STATUS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AppTheme.colors.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isNimConfigured) Color.Green else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "NIM API",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppTheme.colors.onSurface
                            )
                            Text(
                                text = if (isNimConfigured) "Connected" else "Not configured",
                                fontSize = 12.sp,
                                color = AppTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                    if (!isNimConfigured) {
                        Button(onClick = onNavigateToSettings) {
                            Text("Configure")
                        }
                    }
                }
            }
        }

        // Active Provider
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ACTIVE PROVIDER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AppTheme.colors.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = activeProvider,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.onSurface
                    )
                    Text(
                        text = config.activeModel,
                        fontSize = 12.sp,
                        color = AppTheme.colors.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "QUICK ACTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AppTheme.colors.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: Test NIM connection */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = "Test", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test NIM")
                    }

                    OutlinedButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Settings")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* TODO: Start form filling test */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Form Fill")
                    }

                    OutlinedButton(
                        onClick = { /* TODO: View logs */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = "Logs", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Logs")
                    }
                }
            }
        }

        // Version info
        Text(
            text = "JARVIS 0.1.0-alpha | Build 1",
            fontSize = 10.sp,
            color = AppTheme.colors.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}