package atlas.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.kdroidfilter.webview.web.WebView
import io.github.kdroidfilter.webview.web.rememberWebViewStateWithHTMLData
import atlas.messenger.ui.PlatformBackHandler

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AtlasSpaceViewer(
    html: String,
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlatformBackHandler(enabled = true, onBack = onClose)

    val colors = MaterialTheme.colorScheme
    val safeHtml = remember(html, colors.primary, colors.background, colors.onBackground, colors.onPrimary) {
        prepareAtlasSpaceHtml(
            rawHtml = html,
            accent = colors.primary.toCssColor(),
            background = colors.background.toCssColor(),
            onBackground = colors.onBackground.toCssColor(),
            onAccent = colors.onPrimary.toCssColor(),
        )
    }
    val state = rememberWebViewStateWithHTMLData(
        data = safeHtml,
        baseUrl = "https://atlas.local/",
        encoding = "utf-8",
        mimeType = "text/html",
        historyUrl = null,
    )

    DisposableEffect(state) {
        state.webSettings.isJavaScriptEnabled = true
        onDispose { }
    }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(title.ifBlank { "Atlas Space" }) },
                navigationIcon = {
                    IconButton(
                        shapes = IconButtonDefaults.shapes(),
                        onClick = onClose,
                    ) {
                        Icon(MaterialSymbols.RoundedFilled.Close, contentDescription = "Закрыть")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AtlasAppBarColor()),
            )
            WebView(
                state = state,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            )
        }
    }
}

private fun prepareAtlasSpaceHtml(
    rawHtml: String,
    accent: String,
    background: String,
    onBackground: String,
    onAccent: String,
): String {
    val themeStyle = """
        <style id="atlas-space-theme">
            :root {
                color-scheme: dark;
                --atlas-accent: $accent;
                --atlas-bg: $background;
                --atlas-fg: $onBackground;
                --atlas-on-accent: $onAccent;
            }
            html, body {
                margin: 0;
                min-height: 100%;
                background: var(--atlas-bg) !important;
                color: var(--atlas-fg) !important;
            }
            button, input[type="button"], input[type="submit"] {
                background: var(--atlas-accent);
                color: var(--atlas-on-accent);
            }
        </style>
    """.trimIndent()
    val fallbackBody = """
        <div id="atlas-space-fallback" style="padding:24px;font-family:system-ui,sans-serif;background:$background;color:$onBackground;min-height:100vh;box-sizing:border-box;">
            <h1 style="color:$accent;margin-top:0;">Atlas Space</h1>
            <p>Если вы видите это, HTML загрузился, но содержимое пространства не отрисовалось.</p>
        </div>
    """.trimIndent()
    val html = rawHtml.trim().let { value ->
        if (value.contains("<html", ignoreCase = true)) value else """
            <!doctype html>
            <html>
              <head><meta charset=\"utf-8\"><title>Atlas Space</title></head>
              <body>$value</body>
            </html>
        """.trimIndent()
    }
    val withCsp = if (html.contains("Content-Security-Policy", ignoreCase = true)) {
        html
    } else {
        val csp = """<meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data: blob:; font-src data:; connect-src 'none'; media-src data: blob:; object-src 'none'; base-uri 'none'; form-action 'none'">"""
        injectIntoHead(html, csp)
    }
    return injectIntoHead(withCsp, themeStyle)
}

private fun injectIntoHead(html: String, content: String): String {
    val headIndex = html.indexOf("<head", ignoreCase = true)
    if (headIndex == -1) return html.replace("<html>", "<html><head>$content</head>", ignoreCase = true)
    val headClose = html.indexOf('>', headIndex)
    if (headClose == -1) return html
    return html.substring(0, headClose + 1) + content + html.substring(headClose + 1)
}

private fun Color.toCssColor(): String {
    val a = (alpha * 255).toInt().coerceIn(0, 255)
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return "rgba($r, $g, $b, ${a / 255.0})"
}
