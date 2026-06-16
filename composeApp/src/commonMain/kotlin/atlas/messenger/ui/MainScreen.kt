package atlas.messenger.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Add
import com.composables.icons.materialsymbols.roundedfilled.Android
import com.composables.icons.materialsymbols.roundedfilled.Archive
import com.composables.icons.materialsymbols.roundedfilled.Arrow_back
import com.composables.icons.materialsymbols.roundedfilled.Chat
import com.composables.icons.materialsymbols.roundedfilled.Chat_bubble
import com.composables.icons.materialsymbols.roundedfilled.Close
import com.composables.icons.materialsymbols.roundedfilled.Delete
import com.composables.icons.materialsymbols.roundedfilled.Search
import com.composables.icons.materialsymbols.roundedfilled.Settings
import com.composables.icons.materialsymbols.roundedfilled.Star
import com.composables.icons.materialsymbols.roundedfilled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import atlas.messenger.data.ChatMessage
import atlas.messenger.viewmodel.ChatViewModel
import atlas.messenger.ui.components.AvatarBox
import atlas.messenger.ui.components.ChatTile
import atlas.messenger.ui.components.CircularUnreadBadge
import atlas.messenger.ui.components.AtlasAppBarColor
import atlas.messenger.ui.components.displayNameFor
import atlas.messenger.ui.components.AtlasSpaceViewer
import atlas.messenger.ui.components.AtlasXScreen
import atlas.messenger.ui.components.AtlasXPaymentScreen
import atlas.messenger.ui.components.AtlasXActivatedScreen
import atlas.messenger.ui.components.EmptyPane
import atlas.messenger.ui.chat.ChatPane
import atlas.messenger.ui.mite.MiteChatPane
import atlas.messenger.ui.atlas.AtlasBroadcastPane
import atlas.messenger.ui.settings.SettingsPane
import atlas.messenger.ui.UserDiscoveryScreen
import atlas.messenger.network.ConnectionState
import atlas.messenger.util.formatTime

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

            if (state.showUserDiscovery && maxWidth < MOBILE_BREAKPOINT) {
                UserDiscoveryScreen(viewModel)
            }
        }

        state.activeAtlasSpaceHtml?.let { html ->
            AtlasSpaceViewer(
                html = html,
                title = state.activeAtlasSpaceTitle ?: "Atlas Space",
                onClose = viewModel::closeAtlasSpace,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (state.showAtlasXScreen) {
            AtlasXScreen(
                imageDataUrl = state.atlasXImageData,
                onSubscribe = viewModel::openAtlasXPaymentScreen,
                onClose = viewModel::closeAtlasXScreen,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (state.showAtlasXPaymentScreen) {
            AtlasXPaymentScreen(
                onBack = viewModel::openAtlasXScreen,
                onPay = viewModel::activateAtlasXSubscription,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (state.showAtlasXActivatedScreen) {
            AtlasXActivatedScreen(
                onDone = viewModel::closeAtlasXScreen,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileMainScreen(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()

    val showChat = state.selectedPeer != null
    var selectedTab by remember { mutableStateOf(0) }

    val totalUnread by remember(state.unreadCounts, state.archivedConversations) {
        derivedStateOf {
            state.unreadCounts
                .filterKeys { it !in state.archivedConversations }
                .values
                .sum()
        }
    }

    val hasMobileBackTarget = state.showUserDiscovery ||
            showChat ||
            state.selectedMiteChatId != null ||
            state.showMiteChats ||
            state.showArchive ||
            state.showAtlasXScreen ||
            state.showAtlasXPaymentScreen ||
            state.showAtlasXActivatedScreen

    PlatformBackHandler(enabled = hasMobileBackTarget) {
        when {
            state.showUserDiscovery -> viewModel.closeUserDiscovery()
            state.showAtlasXPaymentScreen -> viewModel.openAtlasXScreen()
            state.showAtlasXScreen || state.showAtlasXActivatedScreen -> viewModel.closeAtlasXScreen()
            showChat -> viewModel.closeChat()
            state.selectedMiteChatId != null -> viewModel.closeMiteChat()
            state.showMiteChats -> viewModel.closeMiteChats()
            state.showArchive -> viewModel.closeArchive()
        }
    }

    if (showChat) {
        ChatPane(viewModel = viewModel, showBackButton = true, onBack = viewModel::closeChat)
        return
    }

    if (state.selectedMiteChatId != null) {
        MiteChatPane(viewModel = viewModel, showBackButton = true, onBack = viewModel::closeMiteChat)
        return
    }

    if (state.showMiteChats) {
        MiteChatsScreen(viewModel = viewModel, showBackButton = true, onBack = viewModel::closeMiteChats)
        return
    }

    if (state.showArchive) {
        ArchiveScreen(viewModel = viewModel, showBackButton = true, onBack = viewModel::closeArchive)
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
                    icon = {
                        BadgedBox(badge = { CircularUnreadBadge(totalUnread) }) {
                            Icon(MaterialSymbols.RoundedFilled.Chat, contentDescription = null)
                        }
                    },
                    label = { Text("Чаты") },
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.onSecondaryContainer,
                        selectedTextColor = colors.secondary,
                        unselectedIconColor = colors.onSurfaceVariant,
                        unselectedTextColor = colors.onSurfaceVariant,
                        indicatorColor = colors.secondaryContainer,
                    ),
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(MaterialSymbols.RoundedFilled.Settings, contentDescription = null) },
                    label = { Text("Настройки") },
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.onSecondaryContainer,
                        selectedTextColor = colors.secondary,
                        unselectedIconColor = colors.onSurfaceVariant,
                        unselectedTextColor = colors.onSurfaceVariant,
                        indicatorColor = colors.secondaryContainer,
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
    val state by viewModel.state.collectAsState()
    var fabExpanded by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    val searchText = rememberTextFieldState()
    val fabContainerColor = MaterialTheme.colorScheme.primary
    val fabContentColor = MaterialTheme.colorScheme.onPrimary

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
                title = {
                    Column {
                        Text("Atlas")
                        val connLabel = when (state.connectionState) {
                            ConnectionState.CONNECTED -> "Online"
                            ConnectionState.CONNECTING -> "Connecting..."
                            ConnectionState.DISCONNECTED -> "Offline"
                        }
                        val connColor = when (state.connectionState) {
                            ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                            ConnectionState.CONNECTING -> MaterialTheme.colorScheme.onSurfaceVariant
                            ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.error
                        }
                        Text(connLabel, style = MaterialTheme.typography.labelSmall, color = connColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AtlasAppBarColor()),
            )
        },
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = fabExpanded,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabExpanded,
                        onCheckedChange = { expanded -> fabExpanded = expanded },
                        containerColor = { fabContainerColor },
                        containerSize = ToggleFloatingActionButtonDefaults.containerSize(
                            initialSize = 56.dp,
                            finalSize = 56.dp,
                        ),
                    ) {
                        Icon(
                            imageVector = if (checkedProgress > 0.5f) MaterialSymbols.RoundedFilled.Close else MaterialSymbols.RoundedFilled.Add,
                            contentDescription = "Новый чат",
                            modifier = Modifier.size(24.dp),
                            tint = fabContentColor,
                        )
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    onClick = { fabExpanded = false; viewModel.openMiteChats() },
                    text = { Text("Чат с Mite") },
                    icon = { Icon(MaterialSymbols.RoundedFilled.Android, contentDescription = null) },
                )
                FloatingActionButtonMenuItem(
                    onClick = { fabExpanded = false; viewModel.openUserDiscovery() },
                    text = { Text("Найти людей") },
                    icon = { Icon(MaterialSymbols.RoundedFilled.Search, contentDescription = null) },
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
                    IconButton(onClick = onDismiss) {
                        Icon(MaterialSymbols.RoundedFilled.Arrow_back, contentDescription = "Назад")
                    }
                    BasicTextField(
                        state = searchText,
                        decorator = { inner ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (searchText.text.isEmpty()) {
                                    Text("Поиск пользователей...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    )
                }

                HorizontalDivider()

                if (state.searchResults.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(state.searchResults) { username ->
                            ChatTile(
                                username = username,
                                displayName = displayNameFor(state.displayNames, username),
                                avatarUrl = state.avatars[username],
                                supportingText = if (username in state.onlineUsers) "В сети" else "Не в сети",
                                isOnline = username in state.onlineUsers,
                                onClick = { onUserSelected(username) },
                            )
                        }
                    }
                } else if (searchText.text.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("Ничего не найдено", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("Введите имя пользователя", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val density = LocalDensity.current

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavRail(viewModel)

        Surface(
            modifier = Modifier.width(sidebarWidth).fillMaxHeight().widthIn(min = 220.dp, max = 500.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            when {
                state.showUserDiscovery -> UserDiscoveryScreen(viewModel)
                state.showMiteChats -> MiteChatsScreen(viewModel, embeddedInSidebar = true, onBack = viewModel::closeMiteChats)
                else -> ConversationList(viewModel = viewModel, archived = state.showArchive, showSidebarHeader = true)
            }
        }

        VerticalSplitHandle(onDrag = { delta ->
            sidebarWidth = with(density) { (sidebarWidth.toPx() + delta).toDp().coerceIn(220.dp, 500.dp) }
        })

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when {
                state.showSettings -> SettingsPane(viewModel)
                state.selectedMiteChatId != null -> MiteChatPane(viewModel)
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
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(4.dp)
            .horizontalResizeCursor()
            .hoverable(interactionSource)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { _, dragAmount -> onDrag(dragAmount) },
                )
            }
            .background(if (isHovered || isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    )

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is HoverInteraction.Enter -> isHovered = true
                is HoverInteraction.Exit -> isHovered = false
            }
        }
    }
}

@Composable
private fun NavRail(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val totalUnread = state.unreadCounts.filterKeys { it !in state.archivedConversations }.values.sum()

    val navRailColors = NavigationRailItemDefaults.colors(
        selectedIconColor = colors.onSecondaryContainer,
        selectedTextColor = colors.onSurface,
        indicatorColor = colors.secondaryContainer,
        unselectedIconColor = colors.onSurfaceVariant,
        unselectedTextColor = colors.onSurfaceVariant,
    )

    NavigationRail(modifier = Modifier.fillMaxHeight(), containerColor = colors.surfaceContainerLow, header = {}) {
        NavigationRailItem(
            selected = !state.showSearch && !state.showSettings && !state.showUserDiscovery && !state.showArchive && !state.showMiteChats && state.selectedMiteChatId == null,
            onClick = {
                viewModel.closeSearch()
                viewModel.closeSettings()
                viewModel.closeUserDiscovery()
                viewModel.closeArchive()
                viewModel.closeMiteChats()
            },
            icon = {
                BadgedBox(badge = { CircularUnreadBadge(totalUnread) }) {
                    Icon(MaterialSymbols.RoundedFilled.Chat_bubble, contentDescription = "Чаты")
                }
            },
            label = { Text("Чаты") },
            colors = navRailColors,
        )
        Spacer(Modifier.weight(1f))
        NavigationRailItem(
            selected = state.showSettings,
            onClick = viewModel::openSettings,
            icon = { Icon(MaterialSymbols.RoundedFilled.Settings, contentDescription = "Настройки") },
            label = { Text("Настройки") },
            colors = navRailColors,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConversationList(
    viewModel: ChatViewModel,
    archived: Boolean = false,
    showSidebarHeader: Boolean = false,
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val visibleConversations = remember(state.conversations, state.archivedConversations, state.pinnedConversations, archived) {
        state.conversations
            .filter { peer -> (peer in state.archivedConversations) == archived }
            .sortedByDescending { peer -> peer in state.pinnedConversations }
    }
    val archivedUnread = remember(state.unreadCounts, state.archivedConversations) {
        state.unreadCounts.filterKeys { it in state.archivedConversations }.values.sum()
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.surface)) {
        if (showSidebarHeader) {
            TopAppBar(
                title = { Text(if (archived) "Архив" else "Atlas", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (archived) {
                        IconButton(shapes = IconButtonDefaults.shapes(), onClick = viewModel::closeArchive) {
                            Icon(MaterialSymbols.RoundedFilled.Arrow_back, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    if (!archived) {
                        IconButton(shapes = IconButtonDefaults.shapes(), onClick = viewModel::openUserDiscovery) {
                            Icon(MaterialSymbols.RoundedFilled.Search, contentDescription = "Поиск")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface),
            )
        }

        if (!archived) {
            MiteEntryTile(onClick = viewModel::openMiteChats)
            ArchiveEntryTile(count = state.archivedConversations.size, unreadCount = archivedUnread, onClick = viewModel::openArchive)
        }

        if (visibleConversations.isEmpty()) {
            LaunchedEffect(Unit) { viewModel.refreshPublicUsers() }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val scrollState = rememberScrollState()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp).verticalScroll(scrollState),
                ) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape).background(colors.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(MaterialSymbols.RoundedFilled.Chat, contentDescription = null, modifier = Modifier.size(40.dp), tint = colors.onPrimaryContainer)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(if (archived) "Архив пуст" else "Начните чат!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    Text(
                        if (archived) "Заархивированные диалоги появятся здесь" else "Найдите пользователей, чтобы начать общение",
                        style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (!archived) {
                        Button(onClick = { viewModel.openUserDiscovery() }, shape = RoundedCornerShape(16.dp)) {
                            Icon(MaterialSymbols.RoundedFilled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Найти пользователей")
                        }
                    }
                    if (!archived && state.publicUsers.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("ДОСТУПНЫЕ ПОЛЬЗОВАТЕЛИ", style = MaterialTheme.typography.labelLarge, color = colors.primary, modifier = Modifier.align(Alignment.Start))
                        state.publicUsers.take(5).forEach { user ->
                            UserItem(displayName = displayNameFor(state.displayNames, user.username), avatarUrl = state.avatars[user.username], username = user.username, isOnline = user.isOnline, onClick = { viewModel.onUserSelected(user.username) })
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items = visibleConversations, key = { peer -> peer }) { peer ->
                    val unread = state.unreadCounts[peer] ?: 0
                    val avatarUrl = state.avatars[peer]
                    ConversationItem(
                        peer = peer,
                        displayName = displayNameFor(state.displayNames, peer),
                        isSelected = state.selectedPeer == peer,
                        isOnline = peer in state.onlineUsers,
                        lastMessage = state.allMessages[peer]?.lastOrNull()?.text,
                        unreadCount = unread,
                        avatarUrl = avatarUrl,
                        isPinned = peer in state.pinnedConversations,
                        onClick = { viewModel.onUserSelected(peer) },
                        onDelete = { viewModel.deleteConversation(peer) },
                        onArchiveToggle = { if (archived) viewModel.unarchiveConversation(peer) else viewModel.archiveConversation(peer) },
                        onPinToggle = { viewModel.togglePinConversation(peer) },
                        archived = archived,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArchiveScreen(viewModel: ChatViewModel, showBackButton: Boolean = false, onBack: (() -> Unit)? = null) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TopAppBar(
            title = { Text("Архив", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                if (showBackButton && onBack != null) {
                    IconButton(shapes = IconButtonDefaults.shapes(), onClick = onBack) {
                        Icon(MaterialSymbols.RoundedFilled.Arrow_back, contentDescription = "Назад")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AtlasAppBarColor()),
        )
        ConversationList(viewModel = viewModel, archived = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MiteChatsScreen(viewModel: ChatViewModel, showBackButton: Boolean = false, embeddedInSidebar: Boolean = false, onBack: (() -> Unit)? = null) {
    val state by viewModel.state.collectAsState()
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Чат с Mite", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if ((showBackButton || embeddedInSidebar) && onBack != null) {
                        IconButton(shapes = IconButtonDefaults.shapes(), onClick = onBack) {
                            Icon(MaterialSymbols.RoundedFilled.Arrow_back, contentDescription = "Назад")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AtlasAppBarColor()),
            )
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                if (state.miteChats.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            Text("Нет чатов с Mite", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(state.miteChats, key = { it.id }) { chat ->
                        MiteChatListTile(chat = chat, onClick = { viewModel.openMiteChat(chat.id) }, onDelete = { viewModel.deleteMiteChat(chat.id) })
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = viewModel::startMiteChat,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(MaterialSymbols.RoundedFilled.Add, contentDescription = "Новый чат")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MiteChatListTile(chat: atlas.messenger.viewmodel.MiteChat, onClick: () -> Unit, onDelete: () -> Unit) {
    val preview = chat.messages.lastOrNull { it.text.isNotBlank() }?.text ?: "Нет сообщений"
    val density = LocalDensity.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить чат с Mite?") },
            text = { Text("Чат «${chat.title}» будет удалён из локальной истории.") },
            confirmButton = { TextButton(onClick = { showDeleteDialog = false; onDelete() }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") } },
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        var pressOffset by remember { mutableStateOf(Offset.Zero) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { offset -> pressOffset = offset; menuExpanded = true }) }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                pressOffset = event.changes.first().position
                                menuExpanded = true
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(MaterialSymbols.RoundedFilled.Android, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(chat.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, modifier = Modifier.weight(1f))
                    Text(formatTime(chat.updatedAtMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(preview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        DropdownMenuPopup(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            offset = DpOffset(with(density) { pressOffset.x.toDp() }, with(density) { pressOffset.y.toDp() }),
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1)) {
                DropdownMenuItem(
                    text = { Text("Удалить чат") },
                    shape = MenuDefaults.leadingItemShape,
                    leadingIcon = { Icon(MaterialSymbols.RoundedFilled.Delete, modifier = Modifier.size(MenuDefaults.LeadingIconSize), contentDescription = null) },
                    onClick = { menuExpanded = false; showDeleteDialog = true },
                    colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error, leadingIconColor = MaterialTheme.colorScheme.error),
                )
            }
        }
    }
}

@Composable
private fun MiteEntryTile(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(MaterialSymbols.RoundedFilled.Android, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("Чат с Mite", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("ИИ-помощник", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun ArchiveEntryTile(count: Int, unreadCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(MaterialSymbols.RoundedFilled.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("Архив", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(if (count == 0) "Нет диалогов" else "$count диалогов", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (unreadCount > 0) {
            Badge(containerColor = MaterialTheme.colorScheme.error) { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConversationItem(
    peer: String, displayName: String, isSelected: Boolean, isOnline: Boolean,
    lastMessage: String?, unreadCount: Int, avatarUrl: String?,
    isPinned: Boolean = false,
    onClick: () -> Unit, onDelete: () -> Unit, onArchiveToggle: () -> Unit, onPinToggle: () -> Unit, archived: Boolean,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        when (value) {
            SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
            SwipeToDismissBoxValue.StartToEnd -> { onArchiveToggle(); true }
            else -> false
        }
    })

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить чат?") },
            text = { Text("Диалог с «$displayName» будет удалён навсегда.") },
            confirmButton = { TextButton(onClick = { showDeleteDialog = false; onDelete() }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") } },
        )
    }

    SwipeToDismissBox(state = dismissState, backgroundContent = {
        val direction = dismissState.dismissDirection
        val color = when {
            direction == SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
            direction == SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.tertiaryContainer
            else -> Color.Transparent
        }
        Box(modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp)) {
            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Icon(if (archived) MaterialSymbols.RoundedFilled.Unarchive else MaterialSymbols.RoundedFilled.Archive, contentDescription = null, modifier = Modifier.align(Alignment.CenterStart), tint = MaterialTheme.colorScheme.onTertiaryContainer)
            } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                Icon(MaterialSymbols.RoundedFilled.Delete, contentDescription = null, modifier = Modifier.align(Alignment.CenterEnd), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }) {
        Box(modifier = Modifier.fillMaxWidth()) {
            var pressOffset by remember { mutableStateOf(Offset.Zero) }
            ChatTile(
                username = peer, displayName = displayName, avatarUrl = avatarUrl,
                supportingText = lastMessage ?: if (isOnline) "В сети" else "Не в сети",
                isOnline = isOnline, unreadCount = unreadCount, selected = isSelected,
                onLongClick = { offset -> pressOffset = offset; showContextMenu = true },
                onClick = onClick,
            )
            DropdownMenuPopup(expanded = showContextMenu, onDismissRequest = { showContextMenu = false }, offset = DpOffset(with(density) { pressOffset.x.toDp() }, with(density) { pressOffset.y.toDp() })) {
                DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 3)) {
                    DropdownMenuItem(text = { Text(if (archived) "Разархивировать" else "Архивировать") }, shape = MenuDefaults.leadingItemShape, leadingIcon = { Icon(if (archived) MaterialSymbols.RoundedFilled.Unarchive else MaterialSymbols.RoundedFilled.Archive, modifier = Modifier.size(MenuDefaults.LeadingIconSize), contentDescription = null) }, onClick = { showContextMenu = false; onArchiveToggle() })
                    DropdownMenuItem(text = { Text(if (isPinned) "Открепить" else "Закрепить") }, shape = MenuDefaults.shape, leadingIcon = { Icon(MaterialSymbols.RoundedFilled.Star, modifier = Modifier.size(MenuDefaults.LeadingIconSize), contentDescription = null) }, onClick = { showContextMenu = false; onPinToggle() })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Удалить чат") }, shape = MenuDefaults.trailingItemShape, leadingIcon = { Icon(MaterialSymbols.RoundedFilled.Delete, modifier = Modifier.size(MenuDefaults.LeadingIconSize), contentDescription = null) }, onClick = { showContextMenu = false; showDeleteDialog = true }, colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error, leadingIconColor = MaterialTheme.colorScheme.error))
                }
            }
        }
    }
}
