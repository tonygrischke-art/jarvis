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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.opendroid.ai.core.llm.ProviderCatalog
import com.opendroid.ai.data.models.LLMConfig
import com.opendroid.ai.data.repository.SettingsRepository
import com.opendroid.ai.ui.theme.*
import com.opendroid.ai.ui.viewmodel.SettingsViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import kotlinx.coroutines.launch

/**
 * JARVIS Settings Screen - Minimal: only NIM API Key field
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JARVISSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.llmConfig.collectAsState()

    var nimApiKey by remember { mutableStateOf(config.apiKeys[ProviderCatalog.NIM]?.orEmpty() ?: "") }
    var showKey by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "JARVIS SETTINGS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.surface)
            )
        },
        containerColor = AppTheme.colors.background,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // NIM API Key Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NIM API KEY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AppTheme.colors.accentCyan
                            )
                            Text(
                                text = "NVIDIA NIM Triple Models (FastRouter + MainReasoner + Vision)",
                                fontSize = 12.sp,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                        if (nimApiKey.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Configured",
                                tint = Color.Green,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = nimApiKey,
                        onValueChange = { nimApiKey = it },
                        label = { Text("Enter NIM API Key", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showKey) "Hide API key" else "Show API key",
                                    tint = AppTheme.colors.textSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.colors.accentCyan,
                            unfocusedBorderColor = AppTheme.colors.borderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.updateConfig { current ->
                                        val newApiKeys = current.apiKeys.toMutableMap()
                                        if (nimApiKey.isBlank()) {
                                            newApiKeys.remove(ProviderCatalog.NIM)
                                        } else {
                                            newApiKeys[ProviderCatalog.NIM] = nimApiKey
                                        }
                                        current.copy(apiKeys = newApiKeys)
                                    }
                                    onBack()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accentCyan)
                        ) {
                            Text("SAVE", color = AppTheme.colors.textPrimary)
                        }
                    }
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.accentCyan.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = AppTheme.colors.accentCyan,
                            modifier = Modifier.size(20.dp).padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = "HOW TO GET NIM API KEY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = AppTheme.colors.accentCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Go to https://build.nvidia.com/\n" +
                                       "2. Sign in with NVIDIA account\n" +
                                       "3. Go to API Keys → Create New Key\n" +
                                       "4. Copy the key and paste above\n\n" +
                                       "Models used:\n" +
                                       "• FastRouter: Qwen2.5-0.5B (intent classification)\n" +
                                       "• MainReasoner: DeepSeek-Coder-1.3B (planning)\n" +
                                       "• VisionDescriber: LLaVA-1.6-Mistral-7B (screenshots)",
                                fontSize = 12.sp,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Provider Selection (read-only for now, NIM is primary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PROVIDER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = AppTheme.colors.accentCyan
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NIM Triple (Primary)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = "FastRouter + MainReasoner + VisionDescriber",
                                fontSize = 12.sp,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                        if (nimApiKey.isBlank()) {
                            Text(
                                text = "REQUIRES KEY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .background(Color.Red.copy(alpha = 0.1f))
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}