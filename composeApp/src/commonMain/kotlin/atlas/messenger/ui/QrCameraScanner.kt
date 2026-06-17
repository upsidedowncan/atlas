package atlas.messenger.ui

import androidx.compose.runtime.Composable

@Composable
expect fun QrCameraScanner(
    onQrCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
)
