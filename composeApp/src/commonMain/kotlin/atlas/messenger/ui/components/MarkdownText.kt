package atlas.messenger.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun MarkdownText(
    text: String,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val lines = text.lines()
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            if (line.trim().startsWith("```")) {
                val codeLines = mutableListOf<String>()
                index++
                while (index < lines.size && !lines[index].trim().startsWith("```")) {
                    codeLines.add(lines[index])
                    index++
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = codeLines.joinToString("\n"),
                        color = color,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(10.dp),
                    )
                }
            } else if (line.isBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
            } else {
                val trimmed = line.trimStart()
                val style = when {
                    trimmed.startsWith("### ") -> MaterialTheme.typography.titleSmall
                    trimmed.startsWith("## ") -> MaterialTheme.typography.titleMedium
                    trimmed.startsWith("# ") -> MaterialTheme.typography.titleLarge
                    else -> MaterialTheme.typography.bodyLarge
                }
                val display = when {
                    trimmed.startsWith("### ") -> trimmed.removePrefix("### ")
                    trimmed.startsWith("## ") -> trimmed.removePrefix("## ")
                    trimmed.startsWith("# ") -> trimmed.removePrefix("# ")
                    trimmed.startsWith("- ") -> "• ${trimmed.removePrefix("- ")}"
                    trimmed.startsWith("* ") -> "• ${trimmed.removePrefix("* ")}"
                    else -> trimmed
                }
                Text(
                    text = display,
                    color = color,
                    style = style,
                    fontWeight = if (style != MaterialTheme.typography.bodyLarge) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            index++
        }
    }
}
