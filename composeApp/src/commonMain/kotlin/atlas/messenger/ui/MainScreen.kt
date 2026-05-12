package atlas.messenger.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.geometry.Offset
import coil3.compose.AsyncImage
import atlas.messenger.data.ChatMessage
import atlas.messenger.ui.shapes.AnimatedEmptyState
import atlas.messenger.viewmodel.ChatViewModel
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBase64
import io.github.ismoy.imagepickerkmp.domain.models.MimeType
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import me.digitalby.emojipicker.EmojiPicker
import me.digitalby.emojipicker.rememberEmojiPickerState
import atlas.messenger.viewmodel.ColorPreset
import androidx.compose.ui.text.font.FontStyle

@Composable
private fun AvatarBox(
    username: String,
    avatarUrl: String?,
    size: Int = 56,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Аватар $username",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileMainScreen(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()

    val showChat = state.selectedPeer != null
    var selectedTab by remember { mutableStateOf(0) }

    val totalUnread by remember(state.unreadCounts) {
        derivedStateOf { state.unreadCounts.values.sum() }
    }

    if (showChat) {
        PlatformBackHandler(enabled = true) {
            viewModel.closeChat()
        }
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
                        icon = {
                            Box {
                                Icon(Icons.Default.Chat, contentDescription = null)
                                if (totalUnread > 0) {
                                    Badge(containerColor = colors.error) {
                                        Text(if (totalUnread > 99) "99+" else totalUnread.toString())
                                    }
                                }
                            }
                        },
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
                if (state.username == "atlas") {
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                        label = { Text("Atlas") },
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
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> MobileConversationListTab(viewModel)
                1 -> SettingsPane(viewModel, showHeader = true)
                2 -> AtlasBroadcastTab(viewModel)
            }
        }
    }
}

@Composable
private fun AtlasBroadcastTab(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()
    if (state.username != "atlas") return
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.atlasBroadcastText,
            onValueChange = { viewModel.onAtlasBroadcastTextChanged(it) },
            label = { Text("Текст диалога") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.atlasBroadcastImageUrl,
            onValueChange = { viewModel.onAtlasBroadcastImageUrlChanged(it) },
            label = { Text("Image URL (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { viewModel.sendAtlasBroadcastDialog() }, modifier = Modifier.align(Alignment.End)) {
            Text("Отправить всем")
        }
        HorizontalDivider()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.atlasDialogs) { dialog ->
                ListItem(
                    headlineContent = { Text(dialog.text) },
                    supportingContent = {
                        Column {
                            Text(formatTime(dialog.timestampMs))
                            dialog.imageUrl?.let {
                                AsyncImage(
                                    model = it,
                                    contentDescription = "Dialog image",
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                )
                            }
                        }
                    },
                )
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
                    text = { Text("Найти людей") },
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
                                                AvatarBox(username, state.avatars[username], 40)
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
                                                AvatarBox(username, state.avatars[username], 40)
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
                state.showUserDiscovery -> UserDiscoveryScreen(viewModel)
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
    val totalUnread = state.unreadCounts.values.sum()

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
            icon = {
                Box {
                    Icon(Icons.Default.ChatBubble, contentDescription = "Чаты")
                    if (totalUnread > 0) {
                        Badge(containerColor = colors.error, modifier = Modifier.offset(x = 12.dp, y = (-4).dp)) {
                            Text(if (totalUnread > 99) "99+" else totalUnread.toString())
                        }
                    }
                }
            },
            label = { Text("Чаты") },
            modifier = Modifier,
            colors = navRailColors,
        )
        NavigationRailItem(
            selected = state.showUserDiscovery,
            onClick = viewModel::openUserDiscovery,
            icon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
            label = { Text("Поиск") },
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
    val colors = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.conversations.isEmpty()) {
            LaunchedEffect(Unit) {
                viewModel.refreshPublicUsers()
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val scrollState = rememberScrollState()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 32.dp)
                        .verticalScroll(scrollState),
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = colors.onPrimaryContainer
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Начните чат!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Text(
                        "Найдите пользователей, чтобы начать общение",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.openUserDiscovery() },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Найти пользователей")
                    }

                    if (state.publicUsers.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))

                        Text(
                            "ДОСТУПНЫЕ ПОЛЬЗОВАТЕЛИ",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        state.publicUsers.take(5).forEach { user ->
                            UserItem(
                                username = user.username,
                                isOnline = user.isOnline,
                                onClick = { viewModel.onUserSelected(user.username) }
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.conversations) { peer ->
                    val unread = state.unreadCounts[peer] ?: 0
                    val avatarUrl = state.avatars[peer]
                    ConversationItem(
                        peer = peer,
                        isSelected = state.selectedPeer == peer,
                        isOnline = peer in state.onlineUsers,
                        lastMessage = state.allMessages[peer]?.lastOrNull()?.text,
                        unreadCount = unread,
                        avatarUrl = avatarUrl,
                        onClick = { viewModel.onUserSelected(peer) },
                        onDelete = { viewModel.deleteConversation(peer) },
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
    unreadCount: Int,
    avatarUrl: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val bg = if (isSelected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    else
        Color.Transparent

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить чат?") },
            text = { Text("Все сообщения с '$peer' будут удалены. Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showContextMenu = true }
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box {
                AvatarBox(peer, avatarUrl, 56)

                if (isOnline) {
                    val onlineColor = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color(0xFF2E7D32) else Color(0xFF4CAF50)
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
                                .background(onlineColor),
                        )
                    }
                }
                if (unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Text(if (unreadCount > 99) "99+" else unreadCount.toString())
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

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Удалить чат") },
                onClick = {
                    showContextMenu = false
                    showDeleteDialog = true
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
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
                            onClick = onBack,
                            modifier = Modifier
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                    AvatarBox(peer, state.avatars[peer], 40)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(peer, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (peer in state.onlineUsers) "В сети" else "Не в сети",
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
                            contentDescription = "Звонок",
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
                                Icons.Default.Lock,
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
                            onEdit   = { id -> viewModel.editMessage(id, msg.text) },
                            onDelete = { id -> viewModel.deleteMessage(id) },
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
            showEmojiPicker = state.showEmojiPicker,
            onEmojiToggle = viewModel::toggleEmojiPicker,
            onEmojiSelected = viewModel::insertEmoji,
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
    onEdit: ((String) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
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
    var menuPosition by remember { mutableStateOf(Offset.Zero) }

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

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = { 
                        if (message.isOwn) {
                            showMenu = true
                        }
                    },
                ),
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
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text,
                        color = if (message.isOwn) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (message.isEdited) {
                        Text(
                            text = "(ред.)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (message.isOwn) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.End),
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Редактировать") },
                onClick = {
                    showMenu = false
                    onEdit?.invoke(message.id)
                },
            )
            DropdownMenuItem(
                text = { Text("Удалить") },
                onClick = {
                    showMenu = false
                    onDelete?.invoke(message.id)
                },
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
                "Выберите диалог, чтобы начать чат",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Все сообщения защищены сквозным шифрованием",
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    val iconTint = if (iconContainerColor.luminance() > 0.5f) Color.Black else Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = shape,
        color = colors.surfaceContainer,
    ) {
        Row(
            modifier = clickableModifier
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = iconContainerColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = colors.onSurface,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                    )
                }
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
    var showAvatarPicker by remember { mutableStateOf(false) }

    val presetColors = listOf(
        0xFF6750A4.toInt() to "Стандартный",
        0xFF3B5BA9.toInt() to "Синий океан",
        0xFF006A6A.toInt() to "Морской",
        0xFF386A20.toInt() to "Травяной",
        0xFF695F00.toInt() to "Песочный",
        0xFFBA1A1A.toInt() to "Кирпичный",
        0xFF2196F3.toInt() to "Классический синий",
        0xFF007AFF.toInt() to "Яркий синий",
        0xFF5856D6.toInt() to "Индиго",
        0xFF9C27B0.toInt() to "Фиолетовый",
        0xFFE91E63.toInt() to "Розовый",
        0xFFFF2D55.toInt() to "Малиновый",
        0xFFF44336.toInt() to "Красный",
        0xFFFF9500.toInt() to "Яркий оранжевый",
        0xFFFF9800.toInt() to "Оранжевый",
        0xFFFFCC00.toInt() to "Жёлтый",
        0xFF4CAF50.toInt() to "Зелёный",
        0xFF28CD41.toInt() to "Яркий зелёный",
        0xFF00BCD4.toInt() to "Голубой",
        0xFF5AC8FA.toInt() to "Светло-голубой",
        0xFF607D8B.toInt() to "Серый",
        0xFF8E8E93.toInt() to "Светло-серый",
        0xFF795548.toInt() to "Коричневый",
        0xFF000000.toInt() to "Чёрный",
    )

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        if (showAvatarPicker) {
            GalleryPickerLauncher(
                onPhotosSelected = { photos ->
                    showAvatarPicker = false
                    val photo = photos.firstOrNull() ?: return@GalleryPickerLauncher
                    val base64 = photo.loadBase64()
                    val mimeType = photo.mimeType ?: "image/jpeg"
                    viewModel.updateAvatar("data:$mimeType;base64,$base64")
                },
                onError = { showAvatarPicker = false },
                onDismiss = { showAvatarPicker = false },
                allowMultiple = false,
                mimeTypes = listOf(
                    MimeType.IMAGE_JPEG,
                    MimeType.IMAGE_PNG,
                    MimeType.IMAGE_WEBP,
                    MimeType.IMAGE_HEIC,
                    MimeType.IMAGE_HEIF,
                    MimeType.IMAGE_BMP,
                    MimeType.IMAGE_GIF,
                ),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
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
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = !state.avatarUploading) {
                                        showAvatarPicker = true
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(colors.primaryContainer, CircleShape)
                                        .border(
                                            if (state.avatarUploading) 3.dp else 0.dp,
                                            colors.primary,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (state.avatarUploading) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularWavyProgressIndicator(
                                                modifier = Modifier.size(48.dp),
                                                color = colors.primary,
                                            )
                                        }
                                    } else {
                                        AvatarBox(state.username, state.avatars[state.username], 72)
                                    }
                                }
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Сменить фото",
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(24.dp)
                                        .background(colors.primary, CircleShape)
                                        .padding(4.dp),
                                    tint = colors.onPrimary,
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.username,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val onlineColor = if (colors.surface.luminance() > 0.5f) Color(0xFF2E7D32) else Color(0xFF4CAF50)
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(onlineColor),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "В сети",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Нажмите на фото для смены",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.primary,
                                )
                            }
                        }
                    }
                }

            item { Spacer(Modifier.height(8.dp)) }

            // ── Accent colour ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ЦВЕТ АКЦЕНТА",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    
                    val currentColorName = presetColors.find { it.first == state.accentColor }?.second ?: "Пользовательский"
                    Surface(
                        color = colors.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = currentColorName,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                val carouselState = rememberCarouselState { presetColors.size }
                val scope = rememberCoroutineScope()
                HorizontalMultiBrowseCarousel(
                    state = carouselState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 16.dp)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Scroll) {
                                        val delta = event.changes.first().scrollDelta.y
                                        if (delta != 0f) {
                                            scope.launch {
                                                carouselState.scrollBy(delta * 100f)
                                            }
                                            // Consume the event to prevent parent from scrolling
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                            }
                        },
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
                            val tint = if (Color(colorInt).luminance() > 0.5f) Color.Black else Color.White
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }

            // ── Color Preset ─────────────────────────────────────────────
            item {
                SettingsGroupHeader("ПРЕСЕТ ЦВЕТОВ", colors)
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    val isMobile = maxWidth < MOBILE_BREAKPOINT
                    if (isMobile) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ColorPreset.entries.forEach { preset ->
                                val isSelected = state.colorPreset == preset
                                ToggleButton(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.onColorPresetChanged(preset) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = when (preset) {
                                            ColorPreset.DEFAULT -> "Стандартная"
                                            ColorPreset.VIBRANT -> "Яркая"
                                            ColorPreset.MUTED -> "Приглушённая"
                                            ColorPreset.PASTEL -> "Пастельная"
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        ButtonGroup(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            ColorPreset.entries.forEach { preset ->
                                val isSelected = state.colorPreset == preset
                                ToggleButton(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.onColorPresetChanged(preset) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = when (preset) {
                                            ColorPreset.DEFAULT -> "Стандартная"
                                            ColorPreset.VIBRANT -> "Яркая"
                                            ColorPreset.MUTED -> "Приглушённая"
                                            ColorPreset.PASTEL -> "Пастельная"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.BrightnessLow, contentDescription = null,
                                    tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                if (state.contrast < 0.75f) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Трудно читать",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.contrast > 1.25f) {
                                    Text(
                                        "Слишком ярко",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Icon(Icons.Filled.BrightnessHigh, contentDescription = null,
                                    tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
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
                SettingsGroupHeader("ПРИВАТНОСТЬ", colors)
                SettingsTile(
                    icon = Icons.Filled.Public,
                    iconContainerColor = colors.primary.copy(alpha = 0.8f),
                    title = "Публичный профиль",
                    subtitle = "Разрешить другим находить вас в поиске",
                    colors = colors,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    trailing = {
                        Switch(
                            checked = state.isPublic,
                            onCheckedChange = viewModel::onPublicStatusChanged,
                        )
                    },
                )
                Spacer(Modifier.height(2.dp))
                SettingsTile(
                    icon = Icons.Filled.Mic,
                    iconContainerColor = colors.secondary.copy(alpha = 0.8f),
                    title = "Микрофон",
                    subtitle = if (state.micEnabled) "Используется реальный микрофон" else "Симуляция бездействия",
                    colors = colors,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                    trailing = {
                        Switch(
                            checked = state.micEnabled,
                            onCheckedChange = viewModel::onMicEnabledChanged,
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
                    iconContainerColor = colors.tertiaryContainer,
                    title = "Адрес сервера",
                    subtitle = state.serverUrl,
                    colors = colors,
                    shape = RoundedCornerShape(28.dp),
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
                    iconContainerColor = colors.secondaryContainer,
                    title = "Отпечаток ключа",
                    subtitle = state.publicKeyFingerprint,
                    colors = colors,
                    shape = RoundedCornerShape(28.dp),
                )
            }

            item { Spacer(Modifier.height(20.dp)) }

            item { Spacer(Modifier.height(20.dp)) }

            // ── Account ─────────────────────────────────────────────────
            item {
                SettingsGroupHeader("АККАУНТ", colors)
                SettingsTile(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    iconContainerColor = colors.errorContainer,
                    title = "Выйти из аккаунта",
                    subtitle = "Отключиться и очистить локальные данные",
                    colors = colors,
                    shape = RoundedCornerShape(28.dp),
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
    showEmojiPicker: Boolean,
    onEmojiToggle: () -> Unit,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textFieldState = rememberTextFieldState(text)

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
    
    Column(modifier = modifier) {
        if (showEmojiPicker) {
            EmojiPicker(
                onEmojiSelected = onEmojiSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
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
                        contentDescription = "Прикрепить файл",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                ) {
                    BasicTextField(
                        state = textFieldState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 24.dp, max = 150.dp)
                            .imePadding()
                            .onKeyEvent { event ->
                                if (event.key == Key.Enter && textFieldState.text.isNotBlank()) {
                                    onSendClick()
                                    true
                                } else false
                            },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorator = { innerTextField ->
                            if (textFieldState.text.isEmpty()) {
                                Text(
                                    text = "Сообщение...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        },
                    )
                }

                IconButton(
                    onClick = onEmojiToggle,
                    modifier = Modifier
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mood,
                        contentDescription = "Эмодзи",
                        tint = if (showEmojiPicker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.width(4.dp))

                val isTyping = textFieldState.text.isNotBlank()
                IconButton(
                    onClick = { if (isTyping) onSendClick() else onVoiceClick() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isTyping) MaterialTheme.colorScheme.primary else Color.Transparent),
                ) {
                    Icon(
                        imageVector = if (textFieldState.text.isNotBlank()) Icons.Rounded.Send else Icons.Default.Mic,
                        contentDescription = if (isTyping) "Отправить сообщение" else "Записать голос",
                        tint = if (isTyping) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberEmojiPickerState()

    EmojiPicker(
        state = state,
        onEmojiSelected = { emoji ->
            onEmojiSelected(emoji.details.string)
        },
        modifier = modifier.heightIn(max = 350.dp),
    )
}
