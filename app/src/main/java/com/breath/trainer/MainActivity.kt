package com.breath.trainer

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.breath.trainer.ui.TrainerViewModel
import com.breath.trainer.ui.components.IntroDialog
import com.breath.trainer.ui.screens.BreathingScreen
import com.breath.trainer.ui.theme.BreathTrainerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TrainerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            BreathTrainerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val settings by viewModel.uiSettings.collectAsStateWithLifecycle()
                    val state by viewModel.state.collectAsStateWithLifecycle()

                    LaunchedEffect(settings.keepScreenOn) {
                        if (settings.keepScreenOn) {
                            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    AppRoot(
                        viewModel = viewModel,
                        isRunning = state.running,
                    )
                }
            }
        }

        // 让状态栏/导航栏图标颜色与主题一致
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isNight
            isAppearanceLightNavigationBars = !isNight
        }
    }
}

@Composable
private fun AppRoot(
    viewModel: TrainerViewModel,
    isRunning: Boolean,
) {
    var firstLaunch by remember { mutableStateOf(true) }
    BreathingScreen(viewModel = viewModel, keepScreenOn = true)

    if (firstLaunch && !isRunning) {
        IntroDialog(
            onDismiss = { firstLaunch = false },
            onStart = {
                firstLaunch = false
                viewModel.start()
            },
        )
    }
}
