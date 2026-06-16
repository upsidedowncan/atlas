package atlas.messenger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import atlas.messenger.ui.AtlasAppTheme
import atlas.messenger.ui.AuthScreen
import atlas.messenger.ui.MainScreen
import atlas.messenger.ui.CallScreen
import atlas.messenger.viewmodel.AppTheme
import atlas.messenger.viewmodel.ChatViewModel
import atlas.messenger.viewmodel.Screen
import kotlinx.coroutines.delay

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

            if (state.activeCallPeer != null) {
                CallScreen(viewModel)
            }

            ToastOverlay(
                message = state.toastMessage,
                onDismiss = viewModel::dismissToast,
            )
        }
    }
}

@Composable
private fun ToastOverlay(message: String?, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).align(Alignment.BottomCenter),
    ) {
        message?.let { text ->
            Snackbar(
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(3000)
            onDismiss()
        }
    }
}
