package com.opendroid.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opendroid.ai.data.repository.SettingsRepository
import com.opendroid.ai.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun JARVISSettingsScreen(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    var apiKey by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        settingsRepository.llmConfig.collect { config ->
            apiKey = config.apiKeys["NIM"]?.orEmpty() ?: ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "JARVIS Settings",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { newValue ->
                apiKey = newValue
                scope.launch {
                    settingsRepository.updateConfig { current ->
                        val newApiKeys = current.apiKeys.toMutableMap()
                        if (newValue.isBlank()) {
                            newApiKeys.remove("NIM")
                        } else {
                            newApiKeys["NIM"] = newValue
                        }
                        current.copy(apiKeys = newApiKeys)
                    }
                }
            },
            label = { Text("NIM API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "API Key stored securely in encrypted preferences",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}