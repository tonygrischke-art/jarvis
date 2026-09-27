package com.opendroid.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opendroid.ai.data.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ViewModel for the Command Deck - Agentic Home Launcher
 * Manages terminal log stream, system telemetry, and command execution
 */
class CommandDeckViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // Terminal log stream - thread-safe queue for high-frequency updates
    private val _logQueue = ConcurrentLinkedQueue<String>()
    private val _logFlow = MutableStateFlow<List<String>>(emptyList())
    val logFlow: StateFlow<List<String>> = _logFlow.asStateFlow()

    // System telemetry
    private val _batteryLevel = MutableStateFlow<Int>(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _npuUsage = MutableStateFlow<Int>(0)
    val npuUsage: StateFlow<Int> = _npuUsage.asStateFlow()

    private val _memoryUsage = MutableStateFlow<Long>(0)
    val memoryUsage: StateFlow<Long> = _memoryUsage.asStateFlow()

    // Command execution channel
    private val commandChannel = Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)

    init {
        startTelemetryUpdates()
        startCommandProcessor()
    }

    /** Add a log entry to the terminal stream */
    fun addLog(entry: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date())
        val formatted = "[$timestamp] $entry"
        _logQueue.add(formatted)
        // Keep last 500 entries
        while (_logQueue.size > 500) {
            _logQueue.poll()
        }
        _logFlow.value = _logQueue.toList()
    }

    /** Execute a shell command via Termux RunCommandService */
    fun executeCommand(command: String) {
        addLog("> $command")
        commandChannel.trySend(command)
    }

    /** Clear the terminal log */
    fun clearLog() {
        _logQueue.clear()
        _logFlow.value = emptyList()
    }

    /** Start periodic system telemetry updates */
    private fun startTelemetryUpdates() {
        viewModelScope.launch {
            while (true) {
                updateTelemetry()
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    /** Update system telemetry (battery, NPU, memory) */
    private fun updateTelemetry() {
        // Battery level - simulated for now, replace with actual BatteryManager
        _batteryLevel.value = (Math.random() * 100).toInt()

        // NPU usage - simulated, replace with actual NPU telemetry
        _npuUsage.value = (Math.random() * 100).toInt()

        // Memory usage
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        _memoryUsage.value = usedMemory
    }

    /** Process commands via Termux RunCommandService */
    private fun startCommandProcessor() {
        viewModelScope.launch {
            for (command in commandChannel) {
                executeViaTermux(command)
            }
        }
    }

    /** Execute command via Termux RunCommandService intent */
    private fun executeViaTermux(command: String) {
        try {
            val context = com.opendroid.ai.OpenDroidApp.instance
            val intent = android.content.Intent().apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                action = "com.termux.RUN_COMMAND"
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            }
            context.startForegroundService(intent)
            addLog("Command dispatched to Termux")
        } catch (e: Exception) {
            addLog("ERROR: Failed to dispatch command - ${e.message}")
        }
    }

    override fun onCleared() {
        commandChannel.close()
        super.onCleared()
    }

    companion object {
        private var INSTANCE: CommandDeckViewModel? = null
        fun getInstance(repository: SettingsRepository): CommandDeckViewModel {
            return INSTANCE ?: CommandDeckViewModel(repository).also { INSTANCE = it }
        }
    }
}