package atlas.messenger

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import atlas.messenger.ui.AtlasAppTheme
import atlas.messenger.ui.AuthScreen
import atlas.messenger.ui.MainScreen
import atlas.messenger.ui.CallScreen
import atlas.messenger.viewmodel.ChatViewModel
import atlas.messenger.viewmodel.Screen

@Composable
fun App() {
    val viewModel = viewModel { ChatViewModel() }
    val state by viewModel.state.collectAsState()

    AtlasAppTheme(textScale = state.textScale, accentColor = state.accentColor, contrast = state.contrast) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state.screen) {
                Screen.AUTH -> AuthScreen(viewModel)
                Screen.CHAT -> MainScreen(viewModel)
            }
            
            // Global Call Overlay
            if (state.activeCallPeer != null) {
                CallScreen(viewModel)
            }
        }
    }
}
