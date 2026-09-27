package com.opendroid.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opendroid.ai.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first

@Composable
fun JARVISStatusScreen(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    var isConnected by remember { mutableStateOf(false) }
    var providerName by remember { mutableStateOf("NIM Triple") }
    var statusMessage by remember { mutableStateOf("Initializing...") }

    LaunchedEffect(Unit) {
        try {
            val config = settingsRepository.llmConfig.first()
            isConnected = config.apiKeys["NIM"]?.isNotBlank() == true
            statusMessage = if (isConnected) "Connected" else "API Key not configured"
        } catch (e: Exception) {
            statusMessage = "Error: ${e.message}"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "JARVIS Status",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isConnected) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Connection Status",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    statusMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Provider Info", style = MaterialTheme.typography.labelLarge)
                Text(
                    providerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}