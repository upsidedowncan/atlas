package atlas.messenger.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor

actual fun Modifier.horizontalResizeCursor(): Modifier {
    // desktop gets the real resize cursor; the other platforms keep this as a harmless no-op
    return pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
}
