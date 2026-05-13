package atlas.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import atlas.messenger.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDiscoveryScreen(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val searchQuery = textFieldState.text.toString()

    LaunchedEffect(textFieldState.text) {
        val query = textFieldState.text.toString()
        viewModel.onSearchQueryChanged(query)
        if (query.isNotBlank()) viewModel.submitSearch()
    }

    BoxWithConstraints {
        val isMobile = maxWidth < 600.dp

        val content: @Composable (PaddingValues) -> Unit = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(colors.surface)
            ) {
                SearchBar(
                    state = searchBarState,
                    inputField = {
                        SearchBarDefaults.InputField(
                            searchBarState = searchBarState,
                            textFieldState = textFieldState,
                            onSearch = {},
                            placeholder = { Text("Поиск по имени пользователя...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (textFieldState.text.isNotEmpty()) {
                                    IconButton(onClick = { textFieldState.setTextAndPlaceCursorAtEnd("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Очистить")
                                    }
                                }
                            },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (searchQuery.isBlank()) {
                        // Contacts Section (Users we have conversations with)
                        if (state.conversations.isNotEmpty()) {
                            item {
                                DiscoveryHeader("Недавние контакты")
                            }
                            items(state.conversations) { username ->
                                UserItem(
                                    username = username,
                                    isOnline = username in state.onlineUsers,
                                    onClick = {
                                        viewModel.onUserSelected(username)
                                        viewModel.closeUserDiscovery()
                                    }
                                )
                            }
                        }

                        // Publicly Available Section
                        if (state.publicUsers.isNotEmpty()) {
                            item {
                                DiscoveryHeader("Доступные пользователи")
                            }
                            items(state.publicUsers) { publicUser ->
                                UserItem(
                                    username = publicUser.username,
                                    isOnline = publicUser.isOnline,
                                    onClick = {
                                        viewModel.onUserSelected(publicUser.username)
                                        viewModel.closeUserDiscovery()
                                    }
                                )
                            }
                        } else if (state.conversations.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Нет доступных пользователей.\nВключите «Публичный профиль» в настройках, чтобы вас видели!",
                                        textAlign = TextAlign.Center,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // Search Results
                        item {
                            DiscoveryHeader("Результаты поиска")
                        }
                        if (state.searchResults.isNotEmpty()) {
                            items(state.searchResults) { username ->
                                UserItem(
                                    username = username,
                                    isOnline = username in state.onlineUsers,
                                    onClick = {
                                        viewModel.onUserSelected(username)
                                        viewModel.closeUserDiscovery()
                                    }
                                )
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Не найдено пользователей по запросу \"$searchQuery\"")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isMobile) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Найти людей", fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(
                                onClick = viewModel::closeUserDiscovery,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                            }
                        }
                    )
                },
                containerColor = colors.surface,
                content = content
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Найти людей", fontWeight = FontWeight.SemiBold) },
                        actions = {
                            IconButton(onClick = viewModel::closeUserDiscovery) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface),
                    )
                },
                containerColor = colors.surface,
                content = content,
            )
        }
    }
}

@Composable
private fun DiscoveryHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
internal fun UserItem(
    username: String,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BadgedBox(
            badge = {
                if (isOnline) {
                    val onlineColor = if (colors.surface.luminance() > 0.5f) Color(0xFF2E7D32) else Color(0xFF4CAF50)
                    Badge(
                        containerColor = onlineColor,
                        modifier = Modifier.size(12.dp),
                    )
                }
            },
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = username,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (isOnline) "В сети" else "Не в сети",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
