package atlas.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import atlas.messenger.data.ChatMessage
import atlas.messenger.ui.shapes.AnimatedEmptyState
import atlas.messenger.viewmodel.ChatViewModel

private val MOBILE_BREAKPOINT = 600.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth < MOBILE_BREAKPOINT) {
                MobileMainScreen(viewModel)
            } else {
                DesktopMainScreen(viewModel)
            }
        }
        
        if (state.showUserDiscovery) {
            UserDiscoveryScreen(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileMainScreen(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()

    val showChat = state.selectedPeer != null
    var selectedTab by remember { mutableStateOf(0) }

    if (showChat) {
        ChatPane(viewModel = viewModel, showBackButton = true, onBack = viewModel::closeChat)
        return
    }

    Scaffold(
        bottomBar = {
            val colors = MaterialTheme.colorScheme
            NavigationBar(
                containerColor = colors.surfaceContainer,
                contentColor = colors.onSurface,
            ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                        label = { Text("Чаты") },
                        modifier = Modifier,
                        colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.onPrimaryContainer,
                        selectedTextColor = colors.primary,
                        unselectedIconColor = colors.onSurfaceVariant,
                        unselectedTextColor = colors.onSurfaceVariant,
                        indicatorColor = colors.primaryContainer,
                    ),
                )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Настройки") },
                        modifier = Modifier,
                        colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.onPrimaryContainer,
                        selectedTextColor = colors.primary,
                        unselectedIconColor = colors.onSurfaceVariant,
                        unselectedTextColor = colors.onSurfaceVariant,
                        indicatorColor = colors.primaryContainer,
                    ),
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> MobileConversationListTab(viewModel)
                1 -> SettingsPane(viewModel, showHeader = true)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MobileConversationListTab(viewModel: ChatViewModel) {
    var fabExpanded by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    val searchText = rememberTextFieldState()

    LaunchedEffect(searchText.text) {
        val query = searchText.text.toString()
        viewModel.onSearchQueryChanged(query)
        if (query.isNotBlank()) viewModel.submitSearch()
    }

    if (showSearchDialog) {
        MobileSearchDialog(
            viewModel = viewModel,
            searchText = searchText,
            onDismiss = {
                showSearchDialog = false
                viewModel.onSearchQueryChanged("")
            },
            onUserSelected = { username ->
                showSearchDialog = false
                viewModel.onSearchQueryChanged("")
                viewModel.onUserSelected(username)
            },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Atlas") },
            )
        },
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = fabExpanded,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabExpanded,
                        onCheckedChange = { fabExpanded = it },
                        modifier = Modifier,
                        containerSize = ToggleFloatingActionButtonDefaults.containerSize(
                            initialSize = 96.dp,
                            finalSize = 96.dp,
                        ),
                    ) {
                        Icon(
                            imageVector = if (checkedProgress > 0.5f) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Новый чат",
                            modifier = Modifier.size(48.dp),
                        )
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        fabExpanded = false
                        viewModel.openUserDiscovery()
                    },
                    text = { Text("Find People") },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier,
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            ConversationList(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileSearchDialog(
    viewModel: ChatViewModel,
    searchText: TextFieldState,
    onDismiss: () -> Unit,
    onUserSelected: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                    BasicTextField(
                        state = searchText,
                        decorator = { inner ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (searchText.text.isEmpty()) {
                                    Text(
                                        "Поиск пользователей...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }

                HorizontalDivider()

                if (state.searchResults.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(state.searchResults) { username ->
                                    ListItem(
                                        headlineContent = { Text(username) },
                                        supportingContent = {
                                            if (username in state.onlineUsers) {
                                                Text("В сети", color = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        leadingContent = {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = username.first().uppercaseChar().toString(),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        },
                                        modifier = Modifier.clickable { onUserSelected(username) },
                                    )
                        }
                    }
                } else if (searchText.text.isNotBlank()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Ничего не найдено",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Введите имя пользователя",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopMainScreen(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()
    var sidebarWidth by remember { mutableStateOf(300.dp) }
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(textFieldState.text) {
        val query = textFieldState.text.toString()
        viewModel.onSearchQueryChanged(query)
        if (query.isNotBlank()) viewModel.submitSearch()
    }

    val density = LocalDensity.current

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavRail(viewModel)

        Surface(
            modifier = Modifier.width(sidebarWidth).fillMaxHeight().widthIn(min = 220.dp, max = 500.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            val inputField: @Composable () -> Unit = {
                SearchBarDefaults.InputField(
                    searchBarState = searchBarState,
                    textFieldState = textFieldState,
                    onSearch = {
                        scope.launch { searchBarState.animateToCollapsed() }
                        viewModel.closeSearch()
                    },
                    placeholder = { Text("Поиск...") },
                    leadingIcon = {
                        if (searchBarState.currentValue == SearchBarValue.Expanded) {
                            IconButton(
                                onClick = {
                                    scope.launch { searchBarState.animateToCollapsed() }
                                    viewModel.closeSearch()
                                },
                                modifier = Modifier
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                            }
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    },
                    trailingIcon = {
                        if (textFieldState.text.toString().isNotEmpty()) {
                            IconButton(
                                onClick = { textFieldState.setTextAndPlaceCursorAtEnd("") },
                                modifier = Modifier
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    colors = SearchBarDefaults.inputFieldColors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }

            Scaffold(
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    AppBarWithSearch(
                        state = searchBarState,
                        inputField = inputField,
                        colors = SearchBarDefaults.appBarWithSearchColors(
                            searchBarColors = SearchBarDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                inputFieldColors = SearchBarDefaults.inputFieldColors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ),
                        ),
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface,
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    ExpandedDockedSearchBar(
                        state = searchBarState,
                        inputField = inputField,
                    ) {
                        if (state.searchResults.isNotEmpty()) {
                            LazyColumn {
                                items(state.searchResults) { username ->
                                    ListItem(
                                        headlineContent = { Text(username) },
                                        supportingContent = {
                                            if (username in state.onlineUsers) {
                                                Text("В сети", color = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        leadingContent = {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = username.first().uppercaseChar().toString(),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        },
                                        trailingContent = {
                                            TextButton(
                                                onClick = {
                                                    scope.launch { searchBarState.animateToCollapsed() }
                                                    viewModel.onUserSelected(username)
                                                },
                                                modifier = Modifier
                                            ) {
                                                Text("Написать")
                                            }
                                        },
                                        modifier = Modifier.clickable {
                                            scope.launch { searchBarState.animateToCollapsed() }
                                            viewModel.onUserSelected(username)
                                        },
                                    )
                                }
                            }
                        } else if (textFieldState.text.toString().isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "Ничего не найдено",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (!state.showSettings) {
                        ConversationList(viewModel)
                    }
                }
            }
        }

        VerticalSplitHandle(onDrag = { delta ->
            sidebarWidth = with(density) {
                (sidebarWidth.toPx() + delta).toDp().coerceIn(220.dp, 500.dp)
            }
        })

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when {
                state.showSettings -> SettingsPane(viewModel)
                state.selectedPeer != null -> ChatPane(viewModel)
                else -> EmptyPane()
            }
        }
    }
}

@Composable
private fun VerticalSplitHandle(onDrag: (Float) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    var isHovered by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(4.dp)
            .hoverable(interactionSource)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    onDrag(dragAmount)
                }
            }
            .background(
                if (isHovered) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant
            ),
    )

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            isHovered = interaction != null
        }
    }
}

@Composable
private fun NavRail(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    val navRailColors = NavigationRailItemDefaults.colors(
        selectedIconColor = colors.onPrimaryContainer,
        selectedTextColor = colors.onSurface,
        indicatorColor = colors.primaryContainer,
        unselectedIconColor = colors.onSurfaceVariant,
        unselectedTextColor = colors.onSurfaceVariant,
    )

    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = colors.surface,
        header = {},
    ) {
        NavigationRailItem(
            selected = !state.showSearch && !state.showSettings && !state.showUserDiscovery,
            onClick = { viewModel.closeSearch(); viewModel.closeSettings(); viewModel.closeUserDiscovery() },
            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Чаты") },
            label = { Text("Чаты") },
            modifier = Modifier,
            colors = navRailColors,
        )
        NavigationRailItem(
            selected = state.showUserDiscovery,
            onClick = viewModel::openUserDiscovery,
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search") },
            modifier = Modifier,
            colors = navRailColors,
        )
        Spacer(Modifier.weight(1f))
        NavigationRailItem(
            selected = state.showSettings,
            onClick = viewModel::openSettings,
            icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
            label = { Text("Настройки") },
            modifier = Modifier,
            colors = navRailColors,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ConversationList(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AnimatedEmptyState(
                        shapeSize = 64.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Найдите пользователей",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.conversations) { peer ->
                    ConversationItem(
                        peer = peer,
                        isSelected = state.selectedPeer == peer,
                        isOnline = peer in state.onlineUsers,
                        lastMessage = state.allMessages[peer]?.lastOrNull()?.text,
                        onClick = { viewModel.onUserSelected(peer) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    peer: String,
    isSelected: Boolean,
    isOnline: Boolean,
    lastMessage: String?,
    onClick: () -> Unit,
) {
    val bg = if (isSelected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    else
        Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = peer.first().uppercaseChar().toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)),
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = peer,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = lastMessage ?: if (isOnline) "В сети" else "Нет сообщений",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ChatPane(
    viewModel: ChatViewModel,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val peer = state.selectedPeer ?: return

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            val groups = groupMessages(state.messages)
            val totalItems = state.messages.size + groups.size
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (showBackButton && onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = peer.first().uppercaseChar().toString(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(peer, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (peer in state.onlineUsers) "Online" else "Offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (peer in state.onlineUsers) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.startCall(peer) },
                        modifier = Modifier
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

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

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
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

        ExpressiveMessageInput(
            text = state.inputText,
            onTextChange = viewModel::onInputTextChanged,
            onSendClick = viewModel::sendMessage,
            onAttachClick = {},
            onVoiceClick = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private data class MessageGroup(val isOwn: Boolean, val messages: List<ChatMessage>)

private fun groupMessages(messages: List<ChatMessage>): List<MessageGroup> {
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

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isFirst: Boolean,
    isLast: Boolean,
    isSingle: Boolean,
) {
    val radiusLarge = 20.dp
    val radiusSmall = 4.dp

    val shape = if (message.isOwn) {
        RoundedCornerShape(
            topStart    = radiusLarge,
            topEnd      = if (isFirst || isSingle) radiusLarge else radiusSmall,
            bottomStart = radiusLarge,
            bottomEnd   = if (isLast  || isSingle) radiusLarge else radiusSmall,
        )
    } else {
        RoundedCornerShape(
            topStart    = if (isFirst || isSingle) radiusLarge else radiusSmall,
            topEnd      = radiusLarge,
            bottomStart = if (isLast  || isSingle) radiusLarge else radiusSmall,
            bottomEnd   = radiusLarge,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = if (message.isOwn) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = shape,
            color = if (message.isOwn)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.widthIn(max = 480.dp),
            shadowElevation = if (message.isOwn) 2.dp else 0.dp,
        ) {
            Text(
                text = message.text,
                color = if (message.isOwn) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun EmptyPane() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedEmptyState(
                shapeSize = 100.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            )
            Text(
                "Select a conversation to start chatting",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "All messages are end-to-end encrypted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun SettingsGroupHeader(text: String, colors: ColorScheme) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = colors.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsTile(
    icon: ImageVector,
    iconContainerColor: Color,
    title: String,
    subtitle: String,
    colors: ColorScheme,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainer,
    ) {
        Row(
            modifier = clickableModifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = iconContainerColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    subtitle,
                    color = colors.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }

            if (trailing != null) {
                trailing()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsPane(viewModel: ChatViewModel, showHeader: Boolean = true) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    val presetColors = listOf(
        0xFF2196F3.toInt() to "Синий",
        0xFF9C27B0.toInt() to "Фиолетовый",
        0xFFE91E63.toInt() to "Розовый",
        0xFF4CAF50.toInt() to "Зелёный",
        0xFFFF9800.toInt() to "Оранжевый",
        0xFFF44336.toInt() to "Красный",
        0xFF00BCD4.toInt() to "Голубой",
        0xFF607D8B.toInt() to "Серый",
        0xFF795548.toInt() to "Коричневый",
        0xFF000000.toInt() to "Чёрный",
    )

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        if (showHeader) {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                ),
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // ── Profile header ────────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = colors.surfaceContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = state.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = state.username,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50)),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "В сети",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            // ── Accent colour ─────────────────────────────────────────────
            item {
                SettingsGroupHeader("ЦВЕТ АКЦЕНТА", colors)
                HorizontalMultiBrowseCarousel(
                    state = rememberCarouselState { presetColors.size },
                    modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp),
                    preferredItemWidth = 64.dp,
                    itemSpacing = 4.dp,
                    minSmallItemWidth = 28.dp,
                    maxSmallItemWidth = 44.dp,
                ) { i ->
                    val (colorInt, _) = presetColors[i]
                    val isSelected = state.accentColor == colorInt
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .maskClip(MaterialTheme.shapes.medium)
                            .background(Color(colorInt))
                            .clickable { viewModel.onAccentColorChanged(colorInt) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }

            // ── Text scale ────────────────────────────────────────────────
            item {
                SettingsGroupHeader("РАЗМЕР ТЕКСТА", colors)
                Surface(
                    color = colors.surfaceContainer,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("А", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            Text("А", style = MaterialTheme.typography.titleLarge, color = colors.onSurfaceVariant)
                        }
                        Slider(
                            value = state.textScale,
                            onValueChange = viewModel::onTextScaleChanged,
                            valueRange = 0.8f..1.3f,
                            steps = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "Пример сообщения в чате",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (14 * state.textScale).sp,
                            ),
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }

            // ── Contrast ──────────────────────────────────────────────────
            item {
                SettingsGroupHeader("КОНТРАСТ", colors)
                Surface(
                    color = colors.surfaceContainer,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.BrightnessLow, contentDescription = null,
                                tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Icon(Icons.Filled.BrightnessHigh, contentDescription = null,
                                tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                        Slider(
                            value = state.contrast,
                            onValueChange = viewModel::onContrastChanged,
                            valueRange = 0.5f..1.5f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "Яркость и насыщенность",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }

            // ── Privacy ───────────────────────────────────────────────────
            item {
                SettingsGroupHeader("PRIVACY", colors)
                SettingsTile(
                    icon = Icons.Filled.Public,
                    iconContainerColor = Color(0xFF4A90D9),
                    title = "Public Profile",
                    subtitle = "Allow others to find you in discovery",
                    colors = colors,
                    trailing = {
                        Switch(
                            checked = state.isPublic,
                            onCheckedChange = viewModel::onPublicStatusChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.primary,
                            ),
                        )
                    },
                )
                SettingsTile(
                    icon = Icons.Filled.Mic,
                    iconContainerColor = Color(0xFF2E7D32),
                    title = "Mic Input",
                    subtitle = if (state.micEnabled) "Using real microphone" else "Simulated idle animation",
                    colors = colors,
                    trailing = {
                        Switch(
                            checked = state.micEnabled,
                            onCheckedChange = viewModel::onMicEnabledChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.primary,
                            ),
                        )
                    },
                )
            }

            item { Spacer(Modifier.height(20.dp)) }

            // ── Server ────────────────────────────────────────────────────
            item {
                SettingsGroupHeader("СЕРВЕР", colors)
                SettingsTile(
                    icon = Icons.Filled.Cloud,
                    iconContainerColor = Color(0xFF7B1FA2),
                    title = "Адрес сервера",
                    subtitle = state.serverUrl,
                    colors = colors,
                    onClick = { viewModel.openServerUrlDialog() },
                    trailing = {
                        IconButton(onClick = { viewModel.openServerUrlDialog() }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Изменить",
                                tint = colors.onSurfaceVariant,
                            )
                        }
                    },
                )
            }

            item { Spacer(Modifier.height(20.dp)) }

            // ── Security ────────────────────────────────────────────────
            item {
                SettingsGroupHeader("БЕЗОПАСНОСТЬ", colors)
                SettingsTile(
                    icon = Icons.Outlined.Fingerprint,
                    iconContainerColor = Color(0xFF00695C),
                    title = "Отпечаток ключа",
                    subtitle = state.publicKeyFingerprint,
                    colors = colors,
                )
            }

            item { Spacer(Modifier.height(20.dp)) }

            // ── Account ─────────────────────────────────────────────────
            item {
                SettingsGroupHeader("АККАУНТ", colors)
                SettingsTile(
                    icon = Icons.Outlined.Logout,
                    iconContainerColor = colors.errorContainer,
                    title = "Выйти из аккаунта",
                    subtitle = "Disconnect and clear local data",
                    colors = colors,
                    onClick = { viewModel.disconnect() },
                )
            }

            item { Spacer(Modifier.height(32.dp)) }

            // ── App info footer ───────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Atlas",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        "Зашифрованный мессенджер · v1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.outlineVariant,
                    )
                }
            }
        }
    }

    if (state.showServerUrlDialog) {
        ServerUrlDialog(
            currentUrl = state.serverUrl,
            onUrlChanged = viewModel::onServerUrlChanged,
            onDismiss = viewModel::closeServerUrlDialog,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerUrlDialog(
    currentUrl: String,
    onUrlChanged: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var urlText by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Адрес сервера") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Введите адрес сервера для подключения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("ws://адрес:порт") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onUrlChanged(urlText.trim())
                    onDismiss()
                },
                modifier = Modifier
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
            ) {
                Text("Отмена")
            }
        },
    )
}

private fun formatTime(epochMs: Long): String {
    val totalSeconds = epochMs / 1000
    val hours = (totalSeconds / 3600) % 24
    val minutes = (totalSeconds / 60) % 60
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

@Composable
private fun ExpressiveMessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            IconButton(
                onClick = onAttachClick,
                modifier = Modifier
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach file",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 24.dp, max = 150.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text(
                                text = "Message...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    },
                )
            }

            IconButton(
                onClick = {},
                modifier = Modifier
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mood,
                    contentDescription = "Emojis",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(4.dp))

            val isTyping = text.isNotBlank()
            IconButton(
                onClick = { if (isTyping) onSendClick() else onVoiceClick() },
                modifier = Modifier
                    .size(40.dp)
                    
                    .clip(CircleShape)
                    .background(if (isTyping) MaterialTheme.colorScheme.primary else Color.Transparent),
            ) {
                Icon(
                    imageVector = if (isTyping) Icons.Rounded.Send else Icons.Default.Mic,
                    contentDescription = if (isTyping) "Send message" else "Record voice",
                    tint = if (isTyping) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
