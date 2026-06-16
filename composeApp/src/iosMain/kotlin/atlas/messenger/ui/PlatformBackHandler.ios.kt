package atlas.messenger.ui

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS edge-swipe back is handled by UINavigationController when embedded;
    // standalone Compose apps have no system back gesture to catch here.
}
