package atlas.messenger

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import atlas.messenger.ui.AtlasAppTheme
import atlas.messenger.ui.AuthScreen
import atlas.messenger.ui.MainScreen
import atlas.messenger.ui.CallScreen
import atlas.messenger.viewmodel.AppTheme
import atlas.messenger.viewmodel.ChatViewModel
import atlas.messenger.viewmodel.Screen

@Composable
fun App() {
    val viewModel = viewModel { ChatViewModel() }
    val state by viewModel.state.collectAsState()
    val systemInDark = isSystemInDarkTheme()

    val darkTheme = when (state.theme) {
        AppTheme.SYSTEM -> systemInDark
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    AtlasAppTheme(
        textScale = state.textScale,
        accentColor = state.accentColor,
        contrast = state.contrast,
        colorPreset = state.colorPreset,
        darkTheme = darkTheme,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state.screen) {
                Screen.AUTH -> AuthScreen(viewModel)
                Screen.CHAT -> MainScreen(viewModel)
            }

            // let the call take over the screen when someone is live
            if (state.activeCallPeer != null) {
                CallScreen(viewModel)
            }
        }
    }
}
