package atlas.messenger.ui

import androidx.compose.runtime.Composable

@Composable
expect fun isAtlasWebViewReady(): Boolean

@Composable
expect fun atlasWebViewStatusText(): String?
