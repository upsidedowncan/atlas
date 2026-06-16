package atlas.messenger.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Attach_file
import com.composables.icons.materialsymbols.roundedfilled.Mic
import com.composables.icons.materialsymbols.roundedfilled.Mood
import com.composables.icons.materialsymbols.roundedfilled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import me.digitalby.emojipicker.EmojiPicker
import me.digitalby.emojipicker.rememberEmojiPickerState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveMessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
    onVoiceClick: () -> Unit,
    showEmojiPicker: Boolean,
    onEmojiToggle: () -> Unit,
    onEmojiSelected: (String) -> Unit,
    onLimitExceeded: () -> Unit = {},
    maxLength: Int = 2000,
    modifier: Modifier = Modifier,
) {
    val textFieldState = rememberTextFieldState(text)
    val motionScheme = MaterialTheme.motionScheme

    LaunchedEffect(text) {
        if (textFieldState.text.toString() != text) {
            if (text.isEmpty()) {
                textFieldState.setTextAndPlaceCursorAtEnd("")
            } else {
                textFieldState.edit { replace(0, length, text) }
            }
        }
    }

    LaunchedEffect(textFieldState.text) {
        val currentText = textFieldState.text.toString()
        if (currentText != text) {
            onTextChange(currentText)
        }
    }

    val chars = textFieldState.text.length
    val isTyping = textFieldState.text.isNotBlank()
    val sendProgress by animateFloatAsState(
        targetValue = if (isTyping) 1f else 0f,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "send-button-progress",
    )
    val sendContainerColor = lerp(Color.Transparent, MaterialTheme.colorScheme.primary, sendProgress)
    val sendContentColor = lerp(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.onPrimary, sendProgress)

    Column(modifier = modifier) {
        if (showEmojiPicker) {
            AtlasEmojiPicker(
                onEmojiSelected = onEmojiSelected,
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            ) {
                IconButton(
                    onClick = onEmojiToggle,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (showEmojiPicker) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (showEmojiPicker) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Icon(MaterialSymbols.RoundedFilled.Mood, contentDescription = "Эмодзи")
                }

                Spacer(Modifier.width(4.dp))

                Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp, horizontal = 8.dp)) {
                    BasicTextField(
                        state = textFieldState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 24.dp, max = 150.dp)
                            .imePadding()
                            .onKeyEvent { event ->
                                if (event.key == Key.Enter && textFieldState.text.isNotBlank()) {
                                    if (textFieldState.text.length <= maxLength) onSendClick() else onLimitExceeded()
                                    true
                                } else false
                            },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorator = { innerTextField ->
                            if (textFieldState.text.isEmpty()) {
                                Text("Сообщение...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            innerTextField()
                        },
                    )
                }

                Spacer(Modifier.width(4.dp))

                val nearLimit = chars >= (maxLength * 0.85f).toInt()
                if (nearLimit || chars > maxLength) {
                    Text(
                        text = "$chars/$maxLength",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (chars > maxLength) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = onAttachClick,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Icon(MaterialSymbols.RoundedFilled.Attach_file, contentDescription = "Прикрепить файл")
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = {
                        if (isTyping) {
                            if (chars <= maxLength) onSendClick() else onLimitExceeded()
                        } else {
                            onVoiceClick()
                        }
                    },
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = sendContainerColor, contentColor = sendContentColor),
                ) {
                    Crossfade(targetState = isTyping, animationSpec = motionScheme.fastSpatialSpec(), label = "send-icon-crossfade") { typing ->
                        if (typing) {
                            Icon(MaterialSymbols.RoundedFilled.Send, contentDescription = "Отправить", tint = sendContentColor, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(MaterialSymbols.RoundedFilled.Mic, contentDescription = "Голос", tint = sendContentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AtlasEmojiPicker(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberEmojiPickerState()
    EmojiPicker(
        state = state,
        onEmojiSelected = { emoji -> onEmojiSelected(emoji.details.string) },
        modifier = modifier.heightIn(max = 350.dp),
    )
}
