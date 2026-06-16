package atlas.messenger.ui.chat

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Arrow_back
import com.composables.icons.materialsymbols.roundedfilled.Attach_file
import com.composables.icons.materialsymbols.roundedfilled.Call
import com.composables.icons.materialsymbols.roundedfilled.Close
import com.composables.icons.materialsymbols.roundedfilled.Content_copy
import com.composables.icons.materialsymbols.roundedfilled.Delete
import com.composables.icons.materialsymbols.roundedfilled.Edit
import com.composables.icons.materialsymbols.roundedfilled.Keyboard_arrow_down
import com.composables.icons.materialsymbols.roundedfilled.Keyboard_arrow_up
import com.composables.icons.materialsymbols.roundedfilled.Lock
import com.composables.icons.materialsymbols.roundedfilled.Mic
import com.composables.icons.materialsymbols.roundedfilled.Mood
import com.composables.icons.materialsymbols.roundedfilled.Search
import com.composables.icons.materialsymbols.roundedfilled.Send
import com.composables.icons.materialsymbols.roundedfilled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import atlas.messenger.data.ChatMessage
import atlas.messenger.viewmodel.ChatViewModel
import atlas.messenger.ui.components.AvatarBox
import atlas.messenger.ui.components.AtlasAppBarColor
import atlas.messenger.ui.components.displayNameFor
import atlas.messenger.ui.ExpressiveMessageInput
import atlas.messenger.util.formatTime

internal data class MessageGroup(val isOwn: Boolean, val messages: List<ChatMessage>)

internal fun groupMessages(messages: List<ChatMessage>): List<MessageGroup> {
    if (messages.isEmpty()) return emptyList()
    val result = mutableListOf<MessageGroup>()
    var current = mutableListOf(messages[0])
    for (i in 1 until messages.size) {
        if (messages[i].isOwn == messages[i - 1].isOwn) {
            current.add(messages[i])
        } else {
            result.add(MessageGroup(current[0].isOwn, current))
            current = mutableListOf(messages[i])
        }
    }
    result.add(MessageGroup(current[0].isOwn, current))
    return result
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ChatPane(
    viewModel: ChatViewModel,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val chatSearchBarState = rememberSearchBarState()
    val chatSearchFieldState = rememberTextFieldState(state.chatSearchQuery)
    val peer = state.selectedPeer ?: return
    val peerDisplayName = displayNameFor(state.displayNames, peer)
    val isEveryone = peer == ChatViewModel.EVERYONE_PEER

    LaunchedEffect(peer) {
        viewModel.clearUnreadForPeer(peer)
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            val groups = groupMessages(state.messages)
            val totalItems = state.messages.size + groups.size
            listState.animateScrollToItem(totalItems - 1)
        }
    }
    val activeMatchId = state.chatSearchMatchIds.getOrNull(state.activeChatSearchMatchIndex)
    val messageIndexById = remember(state.messages) { state.messages.mapIndexed { index, msg -> msg.id to index }.toMap() }
    LaunchedEffect(activeMatchId) {
        val messageIndex = activeMatchId?.let { messageIndexById[it] } ?: return@LaunchedEffect
        val groupsBefore = groupMessages(state.messages.take(messageIndex))
        val groupBreaksBefore = groupsBefore.size
        val targetItem = 1 + messageIndex + groupBreaksBefore
        listState.animateScrollToItem(targetItem.coerceAtLeast(0))
    }
    LaunchedEffect(state.chatSearchQuery) {
        val query = state.chatSearchQuery
        if (chatSearchFieldState.text.toString() != query) {
            chatSearchFieldState.setTextAndPlaceCursorAtEnd(query)
        }
    }
    LaunchedEffect(chatSearchFieldState.text) {
        val query = chatSearchFieldState.text.toString()
        if (query != state.chatSearchQuery) {
            viewModel.onChatSearchQueryChanged(query)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AvatarBox(peerDisplayName, state.avatars[peer], 40)
                        Column {
                            Text(peerDisplayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (isEveryone) "Общий чат" else if (peer in state.onlineUsers) "В сети" else "Не в сети",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!isEveryone && peer in state.onlineUsers) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (showBackButton && onBack != null) {
                        IconButton(
                            shapes = IconButtonDefaults.shapes(),
                            onClick = onBack,
                        ) {
                            Icon(MaterialSymbols.RoundedFilled.Arrow_back, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    IconButton(
                        shapes = IconButtonDefaults.shapes(),
                        onClick = {
                            if (state.showChatSearch) viewModel.closeChatSearch() else viewModel.openChatSearch()
                        },
                    ) {
                        Icon(
                            if (state.showChatSearch) MaterialSymbols.RoundedFilled.Close else MaterialSymbols.RoundedFilled.Search,
                            contentDescription = if (state.showChatSearch) "Закрыть поиск" else "Поиск по чату",
                        )
                    }
                    if (!isEveryone) {
                        IconButton(
                            shapes = IconButtonDefaults.shapes(),
                            onClick = { viewModel.startCall(peer) },
                        ) {
                            Icon(
                                MaterialSymbols.RoundedFilled.Call,
                                contentDescription = "Звонок",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AtlasAppBarColor()),
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                // the rounded chat canvas gives the appbar that nice little google-chat lift
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        state.errorMessage?.let { error ->
                            Surface(color = MaterialTheme.colorScheme.errorContainer) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                )
                            }
                        }
                        if (state.showChatSearch) {
                            SearchBar(
                                state = chatSearchBarState,
                                inputField = {
                                    SearchBarDefaults.InputField(
                                        searchBarState = chatSearchBarState,
                                        textFieldState = chatSearchFieldState,
                                        onSearch = {},
                                        placeholder = { Text("Поиск сообщений…") },
                                        leadingIcon = { Icon(MaterialSymbols.RoundedFilled.Search, contentDescription = null) },
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (state.chatSearchMatchIds.isEmpty()) "0"
                                                    else "${state.activeChatSearchMatchIndex + 1}/${state.chatSearchMatchIds.size}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                IconButton(onClick = viewModel::prevChatSearchMatch, enabled = state.chatSearchMatchIds.isNotEmpty()) {
                                                    Icon(MaterialSymbols.RoundedFilled.Keyboard_arrow_up, contentDescription = "Предыдущее совпадение")
                                                }
                                                IconButton(onClick = viewModel::nextChatSearchMatch, enabled = state.chatSearchMatchIds.isNotEmpty()) {
                                                    Icon(MaterialSymbols.RoundedFilled.Keyboard_arrow_down, contentDescription = "Следующее совпадение")
                                                }
                                            }
                                        },
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            MaterialSymbols.RoundedFilled.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "Защищено сквозным шифрованием",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            val groups = groupMessages(state.messages)
                            groups.forEach { group ->
                                items(group.messages.size) { idx ->
                                    val msg = group.messages[idx]
                                    val isFirst = idx == 0
                                    val isLast  = idx == group.messages.size - 1
                                    MessageBubble(
                                        message  = msg,
                                        isFirst  = isFirst,
                                        isLast   = isLast,
                                        isSingle = group.messages.size == 1,
                                        isStarred = msg.id in (state.starredMessageIds ?: emptySet()),
                                        isSearchHit = msg.id in state.chatSearchMatchIds,
                                        isActiveSearchHit = msg.id == activeMatchId,
                                        searchQuery = state.chatSearchQuery,
                                        onEdit   = { id -> viewModel.editMessage(id, msg.text) },
                                        onDelete = { id -> viewModel.deleteMessage(id) },
                                        onCopy = { clipboard.setText(AnnotatedString(msg.text)) },
                                        onToggleStar = { id -> viewModel.toggleStarMessage(id) },
                                    )
                                }
                                item {
                                    val last = group.messages.last()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start  = if (last.isOwn) 0.dp else 14.dp,
                                                end    = if (last.isOwn) 14.dp else 0.dp,
                                                bottom = 6.dp,
                                                top    = 2.dp,
                                            ),
                                        horizontalArrangement = if (last.isOwn) Arrangement.End else Arrangement.Start,
                                    ) {
                                        Text(
                                            text  = formatTime(last.timestampMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ExpressiveMessageInput(
                text = state.inputText,
                onTextChange = viewModel::onInputTextChanged,
                onSendClick = viewModel::sendMessage,
                onAttachClick = {},
                onVoiceClick = {},
                showEmojiPicker = state.showEmojiPicker,
                onEmojiToggle = viewModel::toggleEmojiPicker,
                onEmojiSelected = viewModel::insertEmoji,
                maxLength = if (state.atlasXSubscribed) 20_000 else 2_000,
                onLimitExceeded = viewModel::openAtlasXScreen,
            )
        }
    }
}

@Composable
internal fun ChatBubbleSurface(
    isOwn: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    isSingle: Boolean,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val radiusLarge = 22.dp
    val radiusSmall = 6.dp
    val shape = if (isOwn) {
        RoundedCornerShape(
            topStart = radiusLarge,
            topEnd = if (isFirst || isSingle) radiusLarge else radiusSmall,
            bottomStart = radiusLarge,
            bottomEnd = if (isLast || isSingle) radiusLarge else radiusSmall,
        )
    } else {
        RoundedCornerShape(
            topStart = if (isFirst || isSingle) radiusLarge else radiusSmall,
            topEnd = radiusLarge,
            bottomStart = if (isLast || isSingle) radiusLarge else radiusSmall,
            bottomEnd = radiusLarge,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = shape,
            color = if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 480.dp),
            shadowElevation = if (isOwn) 1.dp else 0.dp,
            tonalElevation = if (isOwn) 0.dp else 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = verticalArrangement,
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MessageBubble(
    message: ChatMessage,
    isFirst: Boolean,
    isLast: Boolean,
    isSingle: Boolean,
    isStarred: Boolean = false,
    isSearchHit: Boolean = false,
    isActiveSearchHit: Boolean = false,
    searchQuery: String = "",
    onEdit: ((String) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onToggleStar: ((String) -> Unit)? = null,
) {
    if (message.isDeleted) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
            horizontalArrangement = if (message.isOwn) Arrangement.End else Arrangement.Start,
        ) {
            Text(
                text = "Сообщение удалено",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        return
    }

    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    Box {
        ChatBubbleSurface(
            isOwn = message.isOwn,
            isFirst = isFirst,
            isLast = isLast,
            isSingle = isSingle,
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            pressOffset = offset
                            showMenu = true
                        },
                    )
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                pressOffset = event.changes.first().position
                                showMenu = true
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
        ) {
            val textColor = if (message.isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Text(
                text = highlightedMessageText(
                    text = message.text,
                    query = searchQuery,
                    highlightColor = if (message.isOwn) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    },
                ),
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (isSearchHit) {
                Text(
                    text = if (isActiveSearchHit) "Активное совпадение" else "Совпадение",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isOwn) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.primary,
                )
            }
            if (message.isEdited) {
                Text(
                    text = "(ред.)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isOwn) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
            if (isStarred) {
                Icon(
                    MaterialSymbols.RoundedFilled.Star,
                    contentDescription = "Помечено",
                    tint = if (message.isOwn) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp).align(Alignment.End),
                )
            }
        }

        DropdownMenuPopup(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(with(density) { pressOffset.x.toDp() }, with(density) { pressOffset.y.toDp() }),
        ) {
            val visibleItems = buildList {
                if (message.isOwn) add("edit")
                add("copy")
                add("star")
                if (message.isOwn) add("delete")
            }
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(0, 1),
            ) {
                visibleItems.forEachIndexed { index, kind ->
                    val shape = if (index == 0) {
                        MenuDefaults.leadingItemShape
                    } else if (index == visibleItems.lastIndex) {
                        MenuDefaults.trailingItemShape
                    } else {
                        MenuDefaults.middleItemShape
                    }
                    when (kind) {
                        "edit" -> DropdownMenuItem(
                            text = { Text("Редактировать") },
                            shape = shape,
                            leadingIcon = {
                                Icon(
                                    MaterialSymbols.RoundedFilled.Edit,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showMenu = false
                                onEdit?.invoke(message.id)
                            },
                        )
                        "copy" -> DropdownMenuItem(
                            text = { Text("Копировать") },
                            shape = shape,
                            leadingIcon = {
                                Icon(
                                    MaterialSymbols.RoundedFilled.Content_copy,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showMenu = false
                                onCopy?.invoke()
                            },
                        )
                        "star" -> DropdownMenuItem(
                            text = { Text(if (isStarred) "Убрать из избранного" else "В избранное") },
                            shape = shape,
                            leadingIcon = {
                                Icon(
                                    if (isStarred) MaterialSymbols.RoundedFilled.Star else MaterialSymbols.RoundedFilled.Star,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleStar?.invoke(message.id)
                            },
                        )
                        "delete" -> DropdownMenuItem(
                            text = { Text("Удалить") },
                            shape = shape,
                            leadingIcon = {
                                Icon(
                                    MaterialSymbols.RoundedFilled.Delete,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete?.invoke(message.id)
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error,
                            ),
                        )
                    }
                }
            }
        }
    }
}

internal fun highlightedMessageText(
    text: String,
    query: String,
    highlightColor: Color,
): AnnotatedString {
    val q = query.trim()
    if (q.isEmpty()) return AnnotatedString(text)
    val source = text.lowercase()
    val target = q.lowercase()
    return buildAnnotatedString {
        append(text)
        var start = 0
        while (start < source.length) {
            val index = source.indexOf(target, startIndex = start)
            if (index == -1) break
            addStyle(
                style = SpanStyle(
                    background = highlightColor,
                    fontWeight = FontWeight.SemiBold,
                ),
                start = index,
                end = index + target.length,
            )
            start = index + target.length
        }
    }
}
