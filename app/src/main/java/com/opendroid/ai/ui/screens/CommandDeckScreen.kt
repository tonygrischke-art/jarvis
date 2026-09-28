package com.opendroid.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.opendroid.ai.ui.theme.*
import com.opendroid.ai.ui.viewmodel.CommandDeckViewModel
import kotlinx.coroutines.launch

/**
 * Command Deck - Agentic Home Launcher & Command Deck
 * Replaces stock launcher with real-time agent execution stream,
 * push-to-talk voice/text command bar, and Termux integration.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CommandDeckScreen(
    modifier: Modifier = Modifier,
    viewModel: CommandDeckViewModel = viewModel()
) {
    val context = LocalContext.current

    var commandText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Request microphone permission for push-to-talk
    val micPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    // Header telemetry
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val npuUsage by viewModel.npuUsage.collectAsState()
    val memoryUsage by viewModel.memoryUsage.collectAsState()

    // Terminal log
    val logs by viewModel.logFlow.collectAsState()

    // Auto-scroll to bottom
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        listState.animateScrollToItem(logs.size - 1)
    }

    // Initial log entry
    LaunchedEffect(Unit) {
        viewModel.addLog("JARVIS Command Deck initialized")
        viewModel.addLog("System ready. Awaiting commands.")
        viewModel.addLog("Type 'help' for available commands.")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(WindowInsets.systemBars.asPaddingValues())
            .padding(WindowInsets.ime.asPaddingValues())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ─── Top Header Bar: OLED Telemetry ───
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colors.background,
                    contentColor = AppTheme.colors.accentNeonGreen
                ),
                border = BorderStroke(1.dp, AppTheme.colors.borderColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // JARVIS branding
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "JARVIS",
                            tint = AppTheme.colors.accentNeonGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "JARVIS COMMAND DECK",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AppTheme.colors.accentNeonGreen
                        )
                    }

                    // System telemetry pills
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TelemetryPill(
                            icon = Icons.Default.BatteryStd,
                            label = "BAT",
                            value = "$batteryLevel%",
                            color = if (batteryLevel < 20) AppTheme.colors.accentRed else AppTheme.colors.accentNeonGreen
                        )
                        TelemetryPill(
                            icon = Icons.Default.Memory,
                            label = "NPU",
                            value = "$npuUsage%",
                            color = AppTheme.colors.accentNeonGreen
                        )
                        TelemetryPill(
                            icon = Icons.Default.Storage,
                            label = "MEM",
                            value = "${memoryUsage}MB",
                            color = AppTheme.colors.accentNeonGreen
                        )
                    }
                }
            }

            // ─── Central Live Terminal Card ───
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colors.background,
                    contentColor = AppTheme.colors.accentNeonGreen
                ),
                border = BorderStroke(1.dp, AppTheme.colors.borderColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    userScrollEnabled = true
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = AppTheme.colors.accentNeonGreen,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            style = androidx.compose.ui.text.TextStyle(
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }

            // ─── Quick Launch Operations Strip ───
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colors.background,
                    contentColor = AppTheme.colors.accentNeonGreen
                ),
                border = BorderStroke(1.dp, AppTheme.colors.borderColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickLaunchChip(
                        icon = Icons.Default.Terminal,
                        label = "TERMUX",
                        onClick = {
                            launchPackage(context, "com.termux", viewModel)
                            viewModel.addLog("Launching Termux")
                        }
                    )
                    QuickLaunchChip(
                        icon = Icons.Default.Folder,
                        label = "FILES",
                        onClick = {
                            launchFilesApp(context, viewModel)
                            viewModel.addLog("Opening Files")
                        }
                    )
                    QuickLaunchChip(
                        icon = Icons.Default.Settings,
                        label = "SETTINGS",
                        onClick = {
                            launchSettings(context, viewModel)
                            viewModel.addLog("Opening Settings")
                        }
                    )
                }
            }

            // ─── Persistent Bottom Action Bar ───
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colors.background,
                    contentColor = AppTheme.colors.accentNeonGreen
                ),
                border = BorderStroke(1.dp, AppTheme.colors.borderColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Multi-line command input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Push-to-talk button
                        PushToTalkButton(
                            modifier = Modifier.padding(end = 8.dp),
                            onPress = {
                                viewModel.addLog("🎤 Push-to-talk activated")
                            },
                            onRelease = {
                                viewModel.addLog("🎤 Push-to-talk released")
                            },
                            isRecording = false,
                            enabled = micPermission.status == PermissionStatus.Granted
                        )

                        // Command input field
                        OutlinedTextField(
                            value = commandText,
                            onValueChange = { commandText = it },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            singleLine = false,
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (commandText.isNotBlank()) {
                                        executeCommand(commandText, keyboardController, viewModel)
                                    }
                                }
                            ),
                            visualTransformation = VisualTransformation.None,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = AppTheme.colors.accentNeonGreen,
                                unfocusedBorderColor = AppTheme.colors.borderColor.copy(alpha = 0.5f),
                                disabledBorderColor = AppTheme.colors.borderColor.copy(alpha = 0.3f),
                                textColor = AppTheme.colors.accentNeonGreen,
                                cursorColor = AppTheme.colors.accentNeonGreen,
                                disabledTextColor = AppTheme.colors.textSecondary
                            ),
                            label = { Text("Command...", color = AppTheme.colors.textSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                            placeholder = { Text("Enter command or 'help'", color = AppTheme.colors.textSecondary.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                        )

                        // Execute button
                        Button(
                            onClick = {
                                val cmd = commandText.trim()
                                if (cmd.isNotEmpty()) {
                                    commandText = ""
                                    keyboardController?.hide()
                                    viewModel.executeCommand(cmd)
                                }
                            },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(48.dp)
                                .width(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.accentNeonGreen,
                                contentColor = AppTheme.colors.background
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Execute",
                                tint = AppTheme.colors.background,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Helper Functions (outside Composable) ───

fun launchPackage(context: android.content.Context, packageName: String, viewModel: CommandDeckViewModel) {
    try {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            viewModel.addLog("Package not found: $packageName")
        }
    } catch (e: Exception) {
        viewModel.addLog("Launch failed: ${e.message}")
    }
}

fun launchFilesApp(context: android.content.Context, viewModel: CommandDeckViewModel) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("file:///sdcard/")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        launchPackage(context, "com.google.android.documentsui", viewModel)
    }
}

fun launchSettings(context: android.content.Context, viewModel: CommandDeckViewModel) {
    try {
        val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        viewModel.addLog("Settings launch failed: ${e.message}")
    }
}

// ─── Command Execution ───

fun executeCommand(
    command: String,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    viewModel: CommandDeckViewModel
) {
    viewModel.addLog("Executing: $command")
    keyboardController?.hide()
    viewModel.executeCommand(command)
}

// ─── Reusable Components ───

@Composable
fun TelemetryPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(AppTheme.colors.background.copy(alpha = 0.6f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = AppTheme.colors.textSecondary
                )
                Text(
                    text = value,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
fun QuickLaunchChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.3f)
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.background,
            contentColor = AppTheme.colors.accentNeonGreen
        ),
        border = BorderStroke(1.dp, AppTheme.colors.borderColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AppTheme.colors.accentNeonGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.accentNeonGreen,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PushToTalkButton(
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    isRecording: Boolean,
    enabled: Boolean
) {
    var pressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                if (isRecording) Color.Red
                else if (!enabled) AppTheme.colors.textSecondary.copy(alpha = 0.3f)
                else AppTheme.colors.accentNeonGreen.copy(alpha = if (pressed) 1f else 0.3f)
            )
            .graphicsLayer { clip = true }
            .border(
                if (isRecording) BorderStroke(2.dp, Color.Red)
                else BorderStroke(1.dp, AppTheme.colors.borderColor.copy(alpha = 0.5f)),
                RoundedCornerShape(24.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        if (enabled) onPress()
                    },
                    onTap = {
                        pressed = false
                        if (enabled) onRelease()
                    }
                )
            }
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = if (isRecording) "Recording..." else "Push to Talk",
            tint = if (isRecording || !enabled) AppTheme.colors.background else AppTheme.colors.accentNeonGreen,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center)
        )
    }
}
