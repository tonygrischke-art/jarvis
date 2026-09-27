package com.opendroid.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.opendroid.ai.data.repository.SettingsRepository
import com.opendroid.ai.ui.screens.CommandDeckScreen
import com.opendroid.ai.ui.theme.OpenDroidTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OpenDroidTheme {
                Surface(modifier = Modifier) {
                    CommandDeckScreen()
                }
            }
        }
    }
}