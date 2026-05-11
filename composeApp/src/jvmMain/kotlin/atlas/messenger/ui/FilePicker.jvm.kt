package atlas.messenger.ui

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.Base64

actual fun selectImageFile(onSelected: (String?) -> Unit) {
    val dialog = FileDialog(null as Frame?, "Select Image", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name ->
        val lower = name.lowercase()
        lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
        lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp")
    }
    dialog.isVisible = true
    
    if (dialog.file != null) {
        val file = File("${dialog.directory}${dialog.file}")
        if (file.exists()) {
            val bytes = file.readBytes()
            val base64 = Base64.getEncoder().encodeToString(bytes)
            val mimeType = when {
                dialog.file.lowercase().endsWith(".png") -> "image/png"
                dialog.file.lowercase().endsWith(".gif") -> "image/gif"
                dialog.file.lowercase().endsWith(".webp") -> "image/webp"
                dialog.file.lowercase().endsWith(".bmp") -> "image/bmp"
                else -> "image/jpeg"
            }
            val dataUrl = "data:$mimeType;base64,$base64"
            onSelected(dataUrl)
        } else {
            onSelected(null)
        }
    } else {
        onSelected(null)
    }
}