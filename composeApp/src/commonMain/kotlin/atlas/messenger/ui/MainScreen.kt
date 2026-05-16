package atlas.messenger.ui

import androidx.compose.animation.core.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.drawscope.withTransform
import coil3.compose.AsyncImage
import atlas.messenger.data.ChatMessage
import atlas.messenger.viewmodel.ChatViewModel
import atlas.messenger.viewmodel.MiteChat
import atlas.messenger.viewmodel.MiteMessage
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBase64
import io.github.ismoy.imagepickerkmp.domain.models.MimeType
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import me.digitalby.emojipicker.EmojiPicker
import me.digitalby.emojipicker.rememberEmojiPickerState
import atlas.messenger.viewmodel.ColorPreset
import androidx.compose.ui.text.font.FontStyle
import io.github.kdroidfilter.webview.web.WebView
import io.github.kdroidfilter.webview.web.rememberWebViewStateWithHTMLData

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatTile(
    username: String,
    displayName: String,
    avatarUrl: String?,
    supportingText: String,
    isOnline: Boolean,
    unreadCount: Int = 0,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val onlineColor = if (colors.surface.luminance() > 0.5f) Color(0xFF2E7D32) else Color(0xFF4CAF50)
    val backgroundColor = if (selected) colors.secondaryContainer else colors.surface
    val titleColor = if (selected) colors.onSecondaryContainer else colors.onSurface
    val supportingColor = if (selected) colors.onSecondaryContainer.copy(alpha = 0.78f) else colors.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BadgedBox(
            badge = {
                if (isOnline) {
                    Badge(
                        containerColor = onlineColor,
                        modifier = Modifier.size(12.dp),
                    )
                }
            },
        ) {
            BadgedBox(
                badge = { CircularUnreadBadge(unreadCount) },
            ) {
                AvatarBox(username, avatarUrl, 44)
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = supportingColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val MOBILE_BREAKPOINT = 600.dp

@Composable
internal fun atlasAppBarColor(): Color {
    return MaterialTheme.colorScheme.surfaceContainerLow
}

internal fun displayNameFor(state: atlas.messenger.viewmodel.ChatUiState, username: String): String {
    return when (username) {
        ChatViewModel.EVERYONE_PEER -> "Все"
        else -> state.displayNames[username] ?: username
    }
}

@Composable
private fun CircularUnreadBadge(count: Int) {
    if (count <= 0) return
    Badge(
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
        modifier = Modifier.size(18.dp),
    ) {
        Text(
            text = if (count > 9) "9+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

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
        // keep one steady android back hook so gestures leave app screens, not the app
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
                            BadgedBox(
                                badge = { CircularUnreadBadge(totalUnread) },
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null)
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
                title = { Text("Atlas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = atlasAppBarColor(),
                ),
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
                            imageVector = if (checkedProgress > 0.5f) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Новый чат",
                            modifier = Modifier.size(24.dp),
                            tint = fabContentColor,
                        )
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        fabExpanded = false
                        viewModel.openMiteChats()
                    },
                    text = { Text("Чат с Mite") },
                    icon = { Icon(Icons.Default.Android, contentDescription = null) },
                    modifier = Modifier,
                )
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
                            ChatTile(
                                username = username,
                                displayName = displayNameFor(state, username),
                                avatarUrl = state.avatars[username],
                                supportingText = if (username in state.onlineUsers) "В сети" else "Не в сети",
                                isOnline = username in state.onlineUsers,
                                onClick = { onUserSelected(username) },
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
                else -> ConversationList(
                    viewModel = viewModel,
                    archived = state.showArchive,
                    showSidebarHeader = true,
                )
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
            .background(
                if (isHovered || isDragging) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant
            ),
    )

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            // hover emits both enter and exit events, so track the exact state instead of any event
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
    val totalUnread = state.unreadCounts
        .filterKeys { it !in state.archivedConversations }
        .values
        .sum()

    val navRailColors = NavigationRailItemDefaults.colors(
        selectedIconColor = colors.onPrimaryContainer,
        selectedTextColor = colors.onSurface,
        indicatorColor = colors.primaryContainer,
        unselectedIconColor = colors.onSurfaceVariant,
        unselectedTextColor = colors.onSurfaceVariant,
    )

    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        // give the rail its own shade so it does not melt into the chat list
        containerColor = colors.surfaceContainerLow,
        header = {},
    ) {
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
                BadgedBox(
                    badge = { CircularUnreadBadge(totalUnread) },
                ) {
                    Icon(Icons.Default.ChatBubble, contentDescription = "Чаты")
                }
            },
            label = { Text("Чаты") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationList(
    viewModel: ChatViewModel,
    archived: Boolean = false,
    showSidebarHeader: Boolean = false,
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val visibleConversations = remember(state.conversations, state.archivedConversations, archived) {
        state.conversations.filter { peer -> (peer in state.archivedConversations) == archived }
    }
    val archivedUnread = remember(state.unreadCounts, state.archivedConversations) {
        state.unreadCounts
            .filterKeys { it in state.archivedConversations }
            .values
            .sum()
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.surface)) {
        if (showSidebarHeader) {
            TopAppBar(
                title = { Text(if (archived) "Архив" else "Atlas", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (archived) {
                        IconButton(onClick = viewModel::closeArchive) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    if (!archived) {
                        IconButton(onClick = viewModel::openUserDiscovery) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface),
            )
        }

        if (!archived) {
            MiteEntryTile(onClick = viewModel::openMiteChats)
            ArchiveEntryTile(
                count = state.archivedConversations.size,
                unreadCount = archivedUnread,
                onClick = viewModel::openArchive,
            )
        }

        if (visibleConversations.isEmpty()) {
            LaunchedEffect(Unit) {
                viewModel.refreshPublicUsers()
            }
            Box(
                modifier = Modifier.weight(1f),
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
                        if (archived) "Архив пуст" else "Начните чат!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Text(
                        if (archived) "Заархивированные диалоги появятся здесь" else "Найдите пользователей, чтобы начать общение",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    if (!archived) {
                        Button(
                            onClick = { viewModel.openUserDiscovery() },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Найти пользователей")
                        }
                    }

                    if (!archived && state.publicUsers.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))

                        Text(
                            "ДОСТУПНЫЕ ПОЛЬЗОВАТЕЛИ",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        state.publicUsers.take(5).forEach { user ->
                            UserItem(
                                displayName = displayNameFor(state, user.username),
                                avatarUrl = state.avatars[user.username],
                                username = user.username,
                                isOnline = user.isOnline,
                                onClick = { viewModel.onUserSelected(user.username) }
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                    items = visibleConversations,
                    key = { peer -> peer },
                ) { peer ->
                    val unread = state.unreadCounts[peer] ?: 0
                    val avatarUrl = state.avatars[peer]
                    ConversationItem(
                        peer = peer,
                        displayName = displayNameFor(state, peer),
                        isSelected = state.selectedPeer == peer,
                        isOnline = peer in state.onlineUsers,
                        lastMessage = state.allMessages[peer]?.lastOrNull()?.text,
                        unreadCount = unread,
                        avatarUrl = avatarUrl,
                        onClick = { viewModel.onUserSelected(peer) },
                        onDelete = { viewModel.deleteConversation(peer) },
                        onArchiveToggle = {
                            if (archived) viewModel.unarchiveConversation(peer) else viewModel.archiveConversation(peer)
                        },
                        archived = archived,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveScreen(
    viewModel: ChatViewModel,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TopAppBar(
            title = { Text("Архив", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                if (showBackButton && onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = atlasAppBarColor()),
        )
        ConversationList(viewModel = viewModel, archived = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiteChatsScreen(
    viewModel: ChatViewModel,
    showBackButton: Boolean = false,
    embeddedInSidebar: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsState()
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Чат с Mite", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if ((showBackButton || embeddedInSidebar) && onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = atlasAppBarColor()),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                if (state.miteChats.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            Text("Нет чатов с Mite", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(state.miteChats, key = { it.id }) { chat ->
                        MiteChatListTile(
                            chat = chat,
                            onClick = { viewModel.openMiteChat(chat.id) },
                            onDelete = { viewModel.deleteMiteChat(chat.id) },
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = viewModel::startMiteChat,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Новый чат")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MiteChatListTile(
    chat: MiteChat,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val preview = chat.messages.lastOrNull { it.text.isNotBlank() }?.text ?: "Нет сообщений"
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить чат с Mite?") },
            text = { Text("Чат «${chat.title}» будет удалён из локальной истории.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true },
                    onLongClickLabel = "Удалить чат",
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        chat.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(formatTime(chat.updatedAtMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(preview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            DropdownMenuItem(
                text = { Text("Удалить чат") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    showDeleteDialog = true
                },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
            )
        }
    }
}

@Composable
private fun MiteEntryTile(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                "Чат с Mite",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                "ИИ-помощник",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ArchiveEntryTile(
    count: Int,
    unreadCount: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Archive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                "Архив",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                if (count == 0) "Нет диалогов" else "$count диалогов",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (unreadCount > 0) {
            Badge(containerColor = MaterialTheme.colorScheme.error) {
                Text(if (unreadCount > 99) "99+" else unreadCount.toString())
            }
        }
    }
}

@Composable
private fun ConversationItem(
    peer: String,
    displayName: String,
    isSelected: Boolean,
    isOnline: Boolean,
    lastMessage: String?,
    unreadCount: Int,
    avatarUrl: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onArchiveToggle: () -> Unit,
    archived: Boolean,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onArchiveToggle()
                false
            } else {
                true
            }
        },
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить чат?") },
            text = { Text("Все сообщения с '$displayName' будут удалены. Это действие нельзя отменить.") },
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

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 1.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment,
            ) {
                Icon(
                    if (archived) Icons.Default.Unarchive else Icons.Default.Archive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            ChatTile(
                username = peer,
                displayName = displayName,
                avatarUrl = avatarUrl,
                supportingText = lastMessage ?: if (isOnline) "В сети" else "Нет сообщений",
                isOnline = isOnline,
                unreadCount = unreadCount,
                selected = isSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                onLongClick = { showContextMenu = true },
                onClick = onClick,
            )

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text(if (archived) "Вернуть из архива" else "В архив") },
                    onClick = {
                        showContextMenu = false
                        onArchiveToggle()
                    }
                )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatPane(
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
    val peerDisplayName = displayNameFor(state, peer)
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
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (state.showChatSearch) viewModel.closeChatSearch() else viewModel.openChatSearch()
                    }) {
                        Icon(
                            if (state.showChatSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (state.showChatSearch) "Закрыть поиск" else "Поиск по чату",
                        )
                    }
                    if (!isEveryone) {
                        IconButton(onClick = { viewModel.startCall(peer) }) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Звонок",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = atlasAppBarColor()),
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
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (state.chatSearchMatchIds.isEmpty()) "0"
                                                    else "${state.activeChatSearchMatchIndex + 1}/${state.chatSearchMatchIds.size}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                IconButton(onClick = viewModel::prevChatSearchMatch, enabled = state.chatSearchMatchIds.isNotEmpty()) {
                                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Предыдущее совпадение")
                                                }
                                                IconButton(onClick = viewModel::nextChatSearchMatch, enabled = state.chatSearchMatchIds.isNotEmpty()) {
                                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Следующее совпадение")
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
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun MiteChatPane(
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
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Android,
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
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 72.dp),
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
                                    Icons.Default.Android,
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
            modifier = Modifier.align(Alignment.BottomCenter),
        )
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
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
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
            Icons.Default.OpenInFull,
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

private data class MessageGroup(val isOwn: Boolean, val messages: List<ChatMessage>)
private data class MiteMessageGroup(val isOwn: Boolean, val messages: List<MiteMessage>)

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

@Composable
private fun ChatBubbleSurface(
    isOwn: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    isSingle: Boolean,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val radiusLarge = 20.dp
    val radiusSmall = 4.dp
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
            color = if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.widthIn(max = 480.dp),
            shadowElevation = if (isOwn) 2.dp else 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = verticalArrangement,
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
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

    Box {
        ChatBubbleSurface(
            isOwn = message.isOwn,
            isFirst = isFirst,
            isLast = isLast,
            isSingle = isSingle,
            modifier = Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = {
                    showMenu = true
                },
            ),
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
                    Icons.Default.Star,
                    contentDescription = "Помечено",
                    tint = if (message.isOwn) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp).align(Alignment.End),
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            if (message.isOwn) {
                DropdownMenuItem(
                    text = { Text("Редактировать") },
                    onClick = {
                        showMenu = false
                        onEdit?.invoke(message.id)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Копировать") },
                onClick = {
                    showMenu = false
                    onCopy?.invoke()
                },
            )
            DropdownMenuItem(
                text = { Text(if (isStarred) "Убрать из избранного" else "В избранное") },
                onClick = {
                    showMenu = false
                    onToggleStar?.invoke(message.id)
                },
            )
            if (message.isOwn) {
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
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AtlasSpaceViewer(
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
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = atlasAppBarColor()),
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
@Composable
private fun EmptyPane() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(38.dp),
                )
            }
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
private fun atlasSwitchColors(colors: ColorScheme = MaterialTheme.colorScheme): SwitchColors {
    // switches should change mood with the theme, not stay trapped in dark mode
    return SwitchDefaults.colors(
        checkedThumbColor = colors.onPrimary,
        checkedTrackColor = colors.primary,
        checkedBorderColor = colors.primary,
        checkedIconColor = colors.primary,
        uncheckedThumbColor = colors.onSurfaceVariant,
        uncheckedTrackColor = colors.surfaceContainerHigh,
        uncheckedBorderColor = colors.outline,
        uncheckedIconColor = colors.surfaceContainerHigh,
        disabledCheckedThumbColor = colors.onSurface.copy(alpha = 0.38f),
        disabledCheckedTrackColor = colors.onSurface.copy(alpha = 0.12f),
        disabledCheckedBorderColor = colors.onSurface.copy(alpha = 0.12f),
        disabledCheckedIconColor = colors.surface.copy(alpha = 0.38f),
        disabledUncheckedThumbColor = colors.onSurface.copy(alpha = 0.38f),
        disabledUncheckedTrackColor = colors.onSurface.copy(alpha = 0.08f),
        disabledUncheckedBorderColor = colors.onSurface.copy(alpha = 0.12f),
        disabledUncheckedIconColor = colors.surface.copy(alpha = 0.38f),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AtlasBroadcastPane(viewModel: ChatViewModel, showHeader: Boolean = false) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val onAtlasTitleChanged: (String) -> Unit = { value -> viewModel.onAtlasBroadcastTitleChanged(value) }
    val onAtlasDescriptionChanged: (String) -> Unit = { value -> viewModel.onAtlasBroadcastDescriptionChanged(value) }
    val onAtlasTextChanged: (String) -> Unit = { value -> viewModel.onAtlasBroadcastTextChanged(value) }
    val onAtlasImageChanged: (String) -> Unit = { value -> viewModel.onAtlasBroadcastImageUrlChanged(value) }
    val sendAtlasDialog: () -> Unit = { viewModel.sendAtlasBroadcastDialog() }

    if (!state.username.equals("atlas", ignoreCase = true)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Доступно только для пользователя atlas",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        if (showHeader) {
            TopAppBar(
                title = { Text("Эфир Atlas", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = atlasAppBarColor()),
            )
        }

        state.errorMessage?.let { error ->
            Surface(color = colors.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = error,
                    color = colors.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = colors.surfaceContainer,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Панель трансляции", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Отправьте диалог всем пользователям Atlas. Сообщение появится в ленте ниже.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = state.atlasBroadcastTitle,
                            onValueChange = onAtlasTitleChanged,
                            label = { Text("Заголовок") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = state.atlasBroadcastDescription,
                            onValueChange = onAtlasDescriptionChanged,
                            label = { Text("Описание") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                        )
                        OutlinedTextField(
                            value = state.atlasBroadcastText,
                            onValueChange = onAtlasTextChanged,
                            label = { Text("Текст диалога") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )
                        OutlinedTextField(
                            value = state.atlasBroadcastImageUrl,
                            onValueChange = onAtlasImageChanged,
                            label = { Text("Ссылка на изображение (необязательно)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = sendAtlasDialog) {
                                Text("Отправить")
                            }
                        }
                    }
                }
            }

            item {
                Text("Лента эфира", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            if (state.atlasDialogs.isEmpty()) {
                item {
                    Surface(shape = MaterialTheme.shapes.large, color = colors.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Пока нет отправленных диалогов",
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(state.atlasDialogs.reversed(), key = { it.id }) { dialog ->
                    val bodyText = dialog.text
                        .lines()
                        .dropWhile { it.startsWith("Заголовок: ") || it.startsWith("Описание: ") }
                        .joinToString("\n")
                        .trim()
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = colors.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (dialog.title.isNotBlank()) {
                                Text(dialog.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            if (dialog.description.isNotBlank()) {
                                Text(dialog.description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                            if (bodyText.isNotBlank()) {
                                Text(bodyText, style = MaterialTheme.typography.bodyLarge)
                            }
                            Text(
                                formatTime(dialog.timestampMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurfaceVariant,
                            )
                            dialog.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Изображение эфира",
                                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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
    bottomDivider: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    val iconTint = Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = shape,
        color = colors.surfaceContainer,
    ) {
        Column {
            Row(
                modifier = clickableModifier
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = iconContainerColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = iconTint, modifier = Modifier.size(21.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        color = colors.onSurface,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            subtitle,
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp
                            ),
                        )
                    }
                }

                if (trailing != null) {
                    Spacer(Modifier.width(12.dp))
                    trailing()
                }
            }

            if (bottomDivider) {
                // android settings rows whisper their borders instead of shouting them
                HorizontalDivider(
                    modifier = Modifier.padding(start = 74.dp),
                    color = colors.outlineVariant.copy(alpha = 0.35f),
                )
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
    // varied little color pops, with white icons so the tiles still feel related
    val settingsIconColors = listOf(
        Color(0xFF5B7CFA),
        Color(0xFF16A34A),
        Color(0xFF0EA5E9),
        Color(0xFF8B5CF6),
        Color(0xFFEF4444),
    )

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
                        containerColor = atlasAppBarColor(),
                    ),
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // start settings with the person, not the machinery
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
                                    text = displayNameFor(state, state.username),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface,
                                )
                                Text(
                                    text = "@${state.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
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

                item {
                    SettingsGroupHeader("ПРОФИЛЬ", colors)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = colors.surfaceContainer,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedTextField(
                                value = state.displayNameInput,
                                onValueChange = viewModel::onDisplayNameChanged,
                                label = { Text("Отображаемое имя") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(onClick = viewModel::submitDisplayName) {
                                    Text("Сохранить имя")
                                }
                            }
                        }
                    }
                }

            item { Spacer(Modifier.height(8.dp)) }

            // the fun color choice lives up front
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
                                            // keep the wheel focused here so the whole page does not jump
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

            // quick moods for people who do not want to tune every color
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

            // readability controls, because pretty is useless if it hurts
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

            // the privacy switches should be impossible to miss
            item {
                SettingsGroupHeader("ПРИВАТНОСТЬ", colors)
                SettingsTile(
                    icon = Icons.Filled.Public,
                    iconContainerColor = settingsIconColors[0],
                    title = "Публичный профиль",
                    subtitle = "Разрешить другим находить вас в поиске",
                    colors = colors,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    trailing = {
                        Switch(
                            checked = state.isPublic,
                            onCheckedChange = viewModel::onPublicStatusChanged,
                            colors = atlasSwitchColors(colors),
                        )
                    },
                )
                Spacer(Modifier.height(3.dp))
                SettingsTile(
                    icon = Icons.Filled.Mic,
                    iconContainerColor = settingsIconColors[1],
                    title = "Микрофон",
                    subtitle = if (state.micEnabled) "Используется реальный микрофон" else "Симуляция бездействия",
                    colors = colors,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                    trailing = {
                        Switch(
                            checked = state.micEnabled,
                            onCheckedChange = viewModel::onMicEnabledChanged,
                            colors = atlasSwitchColors(colors),
                        )
                    },
                )
            }

            item { Spacer(Modifier.height(20.dp)) }

            // server settings for the moments when connection gets fussy
            item {
                SettingsGroupHeader("СЕРВЕР", colors)
                SettingsTile(
                    icon = Icons.Filled.Cloud,
                    iconContainerColor = settingsIconColors[2],
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

            // key details live here for the careful users
            item {
                SettingsGroupHeader("БЕЗОПАСНОСТЬ", colors)
                SettingsTile(
                    icon = Icons.Outlined.Fingerprint,
                    iconContainerColor = settingsIconColors[3],
                    title = "Отпечаток ключа",
                    subtitle = state.publicKeyFingerprint,
                    colors = colors,
                    shape = RoundedCornerShape(28.dp),
                )
            }

            item { Spacer(Modifier.height(20.dp)) }

            // account exit stays separated so it is not tapped by accident
            item {
                SettingsGroupHeader("АККАУНТ", colors)
                SettingsTile(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    iconContainerColor = settingsIconColors[4],
                    title = "Выйти из аккаунта",
                    subtitle = "Отключиться и очистить локальные данные",
                    colors = colors,
                    shape = RoundedCornerShape(28.dp),
                    onClick = { viewModel.disconnect() },
                )
            }

            item { Spacer(Modifier.height(32.dp)) }

            // a small footer to let the screen breathe at the end
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
    onLimitExceeded: () -> Unit = {},
    maxLength: Int = 2000,
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
                                    if (textFieldState.text.length <= maxLength) {
                                        onSendClick()
                                    } else {
                                        onLimitExceeded()
                                    }
                                    true
                                } else {
                                    false
                                }
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

                val chars = textFieldState.text.length
                val isTyping = textFieldState.text.isNotBlank()
                val nearLimit = chars >= (maxLength * 0.85f).toInt()
                if (nearLimit || chars > maxLength) {
                    Text(
                        text = "$chars/$maxLength",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (chars > maxLength) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = {
                        if (isTyping) {
                            if (chars <= maxLength) onSendClick() else onLimitExceeded()
                        } else {
                            onVoiceClick()
                        }
                    },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AtlasXScreen(
    imageDataUrl: String?,
    onSubscribe: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlatformBackHandler(enabled = true, onBack = onClose)

    val primary = MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = modifier,
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AtlasXLogo(modifier = Modifier.size(28.dp))
                        Text("Atlas X")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onSubscribe,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                        ),
                    ) {
                        Text("Подписаться на Atlas X", fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                        Text("Продолжить бесплатно", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val isDesktop = maxWidth >= 840.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (isDesktop) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 980.dp)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    AtlasXHeroCard(
                        imageDataUrl = imageDataUrl,
                        modifier = Modifier.weight(1.05f),
                    )
                    Column(
                        modifier = Modifier.weight(0.95f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AtlasXFeatureCard("20 000 символов в сообщении", "Отправляйте большие тексты без разбиения.", Icons.Default.Message)
                        AtlasXFeatureCard("Приоритет Mite", "Ускоренные ответы и расширенные AI-возможности.", Icons.Default.Bolt)
                        AtlasXFeatureCard("Эксклюзивные темы", "Новые стили интерфейса и расширенная персонализация.", Icons.Default.Palette)
                        AtlasXFeatureCard("Скоростной канал", "Более низкая задержка и приоритетная доставка.", Icons.Default.Speed)
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else {
                val contentMaxWidth = 520.dp
                AtlasXHeroCard(
                    imageDataUrl = imageDataUrl,
                    modifier = Modifier
                        .widthIn(max = contentMaxWidth)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Column(
                    modifier = Modifier
                        .widthIn(max = contentMaxWidth)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AtlasXFeatureCard("20 000 символов в сообщении", "Отправляйте большие тексты без разбиения.", Icons.Default.Message)
                    AtlasXFeatureCard("Приоритет Mite", "Ускоренные ответы и расширенные AI-возможности.", Icons.Default.Bolt)
                    AtlasXFeatureCard("Эксклюзивные темы", "Новые стили интерфейса и расширенная персонализация.", Icons.Default.Palette)
                    AtlasXFeatureCard("Скоростной канал", "Более низкая задержка и приоритетная доставка.", Icons.Default.Speed)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AtlasXPaymentScreen(
    onBack: () -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlatformBackHandler(enabled = true, onBack = onBack)

    val primary = MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Оплата Atlas X") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, shadowElevation = 6.dp) {
                Button(
                    onClick = onPay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                    ),
                ) {
                    Text("Оплатить и активировать", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ListItem(
                        headlineContent = { Text("Atlas X Monthly", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) },
                        supportingContent = { Text("Фейковый экран оплаты для предпросмотра подписки.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        trailingContent = { Text("${'$'}4.99", fontWeight = FontWeight.Bold, color = primary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    HorizontalDivider()
                    OutlinedTextField(
                        value = "4242 4242 4242 4242",
                        onValueChange = {},
                        label = { Text("Номер карты") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = "12/30",
                            onValueChange = {},
                            label = { Text("Срок") },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = "123",
                            onValueChange = {},
                            label = { Text("CVC") },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        "Это заглушка. Деньги не списываются.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AtlasXActivatedScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlatformBackHandler(enabled = true, onBack = onDone)

    val primary = MaterialTheme.colorScheme.primary
    val unlockedFeatures = listOf(
        Triple(Icons.Default.AutoAwesome, "20 000 символов", "Отправляйте большие тексты без ограничений"),
        Triple(Icons.Default.Bolt, "Приоритет Mite", "Ускоренные ответы и расширенные AI-возможности"),
        Triple(Icons.Default.Palette, "Эксклюзивные темы", "Новые стили интерфейса и персонализация"),
        Triple(Icons.Default.Speed, "Скоростной канал", "Низкая задержка и приоритетная доставка"),
        Triple(Icons.Default.Star, "Расширенные лимиты", "Увеличенные квоты на все операции"),
        Triple(Icons.Default.Verified, "X-бейдж", "Отметка подписчика в профиле"),
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, shadowElevation = 6.dp) {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                    ),
                ) {
                    Text("Продолжить", fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            ParticleCelebration(
                modifier = Modifier.fillMaxSize(),
                particleCount = 50,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(32.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(96.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Atlas X активирован",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Text(
                    "Добро пожаловать в премиум",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "Разблокировано",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )

                        unlockedFeatures.forEach { (icon, title, desc) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                                tint = primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun ParticleCelebration(
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
) {
    val particles = remember {
        List(particleCount) {
            Particle(
                x = Random.nextFloat() * 100f,
                y = Random.nextFloat() * 100f,
                size = Random.nextFloat() * 8f + 4f,
                color = listOf(
                    Color(0xFFFF6B6B),
                    Color(0xFF4ECDC4),
                    Color(0xFFFFE66D),
                    Color(0xFF95E1D3),
                    Color(0xFFF38181),
                    Color(0xFFAA96DA),
                    Color(0xFFFFA07A),
                    Color(0xFF87CEEB),
                ).random(),
                speed = Random.nextFloat() * 0.7f + 0.3f,
                wobble = Random.nextFloat() * 1.5f + 0.5f,
                wobbleSpeed = Random.nextFloat() * 3f + 1f,
                rotationSpeed = (Random.nextFloat() * 2.5f + 0.5f) * (if (Random.nextBoolean()) 1f else -1f),
                isCircle = Random.nextBoolean(),
                delay = Random.nextFloat() * 3f,
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
        ),
        label = "time",
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        particles.forEach { particle ->
            val t = ((time * particle.speed + particle.delay) % 100f) / 100f
            val x = (particle.x / 100f) * width + sin((t * particle.wobbleSpeed * PI).toDouble()).toFloat() * particle.wobble * 30f
            val y = ((particle.y / 100f) * height + t * height * 1.2f) % (height + 50f) - 25f
            val alpha = if (t < 0.1f) t / 0.1f else if (t > 0.85f) (1f - t) / 0.15f else 1f
            val rotation = t * particle.rotationSpeed * 360f

            withTransform({
                translate(x, y)
                rotate(rotation)
            }) {
                drawRect(
                    color = particle.color.copy(alpha = alpha * 0.85f),
                    size = androidx.compose.ui.geometry.Size(particle.size, particle.size * 1.5f),
                )
            }
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Color,
    val speed: Float,
    val wobble: Float,
    val wobbleSpeed: Float,
    val rotationSpeed: Float,
    val isCircle: Boolean,
    val delay: Float,
)

@Composable
private fun AtlasXHeroCard(
    imageDataUrl: String?,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SuggestionChip(
                onClick = {},
                label = { Text("Premium") },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = primary) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    iconContentColor = primary,
                ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(718f / 310f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                imageDataUrl?.let { image ->
                    AsyncImage(
                        model = image,
                        contentDescription = "Atlas X",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } ?: Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AtlasXLogo(modifier = Modifier.size(48.dp))
                    Text(
                        "Atlas X",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                "Прокачайте Atlas с подпиской X",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                "Больше лимиты, быстрее ответы и продвинутые инструменты в одном плане.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AtlasXLogo(modifier: Modifier = Modifier) {
    val path = remember {
        PathParser().parsePathString(
            "M76.8965 5.56317C76.8965 7.01929 76.3109 8.14446 74.1883 10.9905L47.9113 46.4006L73.6027 81.3473C75.579 83.9286 76.2377 85.3186 76.2377 86.8409C76.2377 90.084 73.5295 92.202 69.4306 92.202C66.7224 92.202 65.2585 91.143 62.3307 87.1718L39.6403 55.2035L38.9816 55.2035L16.1448 87.1718C13.2902 91.143 11.8995 92.202 9.33764 92.202C5.53151 92.202 2.89649 90.0179 2.89649 86.8409C2.89649 85.3848 3.48205 84.2596 5.6047 81.4135L32.3208 45.5402L5.5315 11.0567C3.55524 8.47541 2.89649 7.08548 2.89649 5.56318C2.89649 2.32001 5.60469 0.202025 9.70361 0.202025C12.4118 0.202025 13.8025 1.19483 16.7303 5.23224L40.0795 36.8035L40.7382 36.8035L63.6482 5.23223C66.576 1.19482 67.8935 0.20202 70.4553 0.20202C74.3347 0.202019 76.8965 2.38619 76.8965 5.56317Z",
        ).toPath(Path())
    }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Canvas(modifier = modifier) {
        val scale = minOf(size.width / 80f, size.height / 98f)
        val dx = (size.width - 80f * scale) / 2f
        val dy = (size.height - 98f * scale) / 2f
        withTransform({
            translate(dx, dy)
            scale(scale, scale)
        }) {
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(primary, secondary),
                    center = androidx.compose.ui.geometry.Offset(58f, 72f),
                    radius = 78f,
                ),
            )
        }
    }
}

@Composable
private fun AtlasXFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            headlineContent = {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            },
            supportingContent = {
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            leadingContent = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
        )
    }
}

private fun highlightedMessageText(
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
