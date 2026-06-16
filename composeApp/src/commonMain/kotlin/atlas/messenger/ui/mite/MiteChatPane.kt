package atlas.messenger.ui.mite

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Android
import com.composables.icons.materialsymbols.roundedfilled.Arrow_back
import com.composables.icons.materialsymbols.roundedfilled.Expand_less
import com.composables.icons.materialsymbols.roundedfilled.Expand_more
import com.composables.icons.materialsymbols.roundedfilled.Open_in_full
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import atlas.messenger.ui.chat.ChatBubbleSurface
import atlas.messenger.ui.ExpressiveMessageInput
import atlas.messenger.util.formatTime
import atlas.messenger.viewmodel.ChatViewModel
import atlas.messenger.viewmodel.MiteMessage

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MiteChatPane(
    viewModel: ChatViewModel,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsState()
    val chat = state.miteChats.firstOrNull { it.id == state.selectedMiteChatId }
    val messages = chat?.messages.orEmpty()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.text, messages.lastOrNull()?.reasoning) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (showBackButton && onBack != null) {
                        IconButton(
                            shapes = IconButtonDefaults.shapes(),
                            onClick = onBack,
                        ) {
                            Icon(MaterialSymbols.RoundedFilled.Arrow_back, contentDescription = "Назад")
                        }
                    }
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            MaterialSymbols.RoundedFilled.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(chat?.title ?: "Чат с Mite", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (state.miteStreamingMessageId != null) "Печатает..." else "ИИ-помощник",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            state.miteErrorMessage?.let { error ->
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    MaterialSymbols.RoundedFilled.Android,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "Спросите Mite о чём угодно",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Ответы передаются через ваш сервер Atlas.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    val groups = groupMiteMessages(messages)
                    groups.forEach { group ->
                        items(group.messages.size) { idx ->
                            val message = group.messages[idx]
                            MiteMessageBubble(
                                message = message,
                                isStreaming = message.id == state.miteStreamingMessageId,
                                isFirst = idx == 0,
                                isLast = idx == group.messages.size - 1,
                                isSingle = group.messages.size == 1,
                                onOpenAtlasSpace = viewModel::openAtlasSpace,
                            )
                        }
                        item {
                            val last = group.messages.last()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = if (last.isOwn) 0.dp else 14.dp,
                                        end = if (last.isOwn) 14.dp else 0.dp,
                                        bottom = 6.dp,
                                        top = 2.dp,
                                    ),
                                horizontalArrangement = if (last.isOwn) Arrangement.End else Arrangement.Start,
                            ) {
                                Text(
                                    text = if (last.id == state.miteStreamingMessageId) "Печатает..." else formatTime(last.timestampMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            ExpressiveMessageInput(
                text = state.miteInputText,
                onTextChange = viewModel::onMiteInputTextChanged,
                onSendClick = viewModel::sendMiteMessage,
                onAttachClick = {},
                onVoiceClick = {},
                showEmojiPicker = false,
                onEmojiToggle = {},
                onEmojiSelected = {},
                maxLength = if (state.atlasXSubscribed) 20_000 else 2_000,
                onLimitExceeded = viewModel::openAtlasXScreen,
            )
        }
    }
}

@Composable
private fun MiteMessageBubble(
    message: MiteMessage,
    isStreaming: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    isSingle: Boolean,
    onOpenAtlasSpace: (String, String) -> Unit,
) {
    ChatBubbleSurface(
        isOwn = message.isOwn,
        isFirst = isFirst,
        isLast = isLast,
        isSingle = isSingle,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!message.isOwn && message.reasoning.isNotBlank()) {
            var expanded by rememberSaveable(message.id) { mutableStateOf(isStreaming) }
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            if (expanded) MaterialSymbols.RoundedFilled.Expand_less else MaterialSymbols.RoundedFilled.Expand_more,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Ход рассуждений${if (isStreaming) "..." else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (expanded) {
                        MarkdownText(
                            text = message.reasoning,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (message.isOwn) {
            Text(
                text = message.text,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            RenderMiteMessageContent(
                text = if (message.text.isBlank() && isStreaming) "..." else message.text,
                color = MaterialTheme.colorScheme.onSurface,
                isStreaming = isStreaming,
                onOpenAtlasSpace = onOpenAtlasSpace,
            )
        }
    }
}

@Composable
private fun RenderMiteMessageContent(
    text: String,
    color: Color,
    isStreaming: Boolean,
    onOpenAtlasSpace: (String, String) -> Unit,
) {
    val parts = remember(text, isStreaming) { parseAtlasSpaceParts(text, isStreaming) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEach { part ->
            when (part) {
                is AtlasSpacePart.Markdown -> MarkdownText(text = part.text, color = color)
                is AtlasSpacePart.Space -> AtlasSpaceButton(html = part.html, onOpen = onOpenAtlasSpace)
                AtlasSpacePart.Pending -> Text(
                    text = "Mite создаёт пространство...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AtlasSpaceButton(
    html: String,
    onOpen: (String, String) -> Unit,
) {
    TextButton(
        onClick = { onOpen(html, "Atlas Space") },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Icon(
            MaterialSymbols.RoundedFilled.Open_in_full,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("Открыть Atlas Space")
    }
}

private sealed interface AtlasSpacePart {
    data class Markdown(val text: String) : AtlasSpacePart
    data class Space(val html: String) : AtlasSpacePart
    data object Pending : AtlasSpacePart
}

private fun parseAtlasSpaceParts(text: String, isStreaming: Boolean): List<AtlasSpacePart> {
    val parts = mutableListOf<AtlasSpacePart>()
    var cursor = 0
    val open = "{atlas_spaces}"
    val close = "{/atlas_spaces}"
    while (cursor < text.length) {
        val start = text.indexOf(open, cursor)
        if (start == -1) {
            text.substring(cursor).trim().takeIf { it.isNotBlank() }?.let { parts.add(AtlasSpacePart.Markdown(it)) }
            break
        }
        text.substring(cursor, start).trim().takeIf { it.isNotBlank() }?.let { parts.add(AtlasSpacePart.Markdown(it)) }
        val contentStart = start + open.length
        val end = text.indexOf(close, contentStart)
        if (end == -1) {
            if (isStreaming) parts.add(AtlasSpacePart.Pending)
            break
        }
        val html = text.substring(contentStart, end).trim()
        if (html.isNotBlank()) parts.add(AtlasSpacePart.Space(html))
        cursor = end + close.length
    }
    return parts.ifEmpty { listOf(AtlasSpacePart.Markdown(text)) }
}

@Composable
private fun MarkdownText(
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

private data class MiteMessageGroup(val isOwn: Boolean, val messages: List<MiteMessage>)

private fun groupMiteMessages(messages: List<MiteMessage>): List<MiteMessageGroup> {
    if (messages.isEmpty()) return emptyList()
    val result = mutableListOf<MiteMessageGroup>()
    var current = mutableListOf(messages[0])
    for (message in messages.drop(1)) {
        if (message.isOwn == current.last().isOwn) {
            current.add(message)
        } else {
            result.add(MiteMessageGroup(current[0].isOwn, current))
            current = mutableListOf(message)
        }
    }
    result.add(MiteMessageGroup(current[0].isOwn, current))
    return result
}
