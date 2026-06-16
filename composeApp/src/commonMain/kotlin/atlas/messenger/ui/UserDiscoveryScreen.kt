package atlas.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Arrow_back
import com.composables.icons.materialsymbols.roundedfilled.Close
import com.composables.icons.materialsymbols.roundedfilled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                            leadingIcon = { Icon(MaterialSymbols.RoundedFilled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (textFieldState.text.isNotEmpty()) {
                                    IconButton(onClick = { textFieldState.setTextAndPlaceCursorAtEnd("") }) {
                                        Icon(MaterialSymbols.RoundedFilled.Close, contentDescription = "Очистить")
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
                        // familiar faces first, because that feels faster
                        if (state.conversations.isNotEmpty()) {
                            item {
                                DiscoveryHeader("Недавние контакты")
                            }
                            items(state.conversations) { username ->
                                UserItem(
                                    displayName = displayNameFor(state, username),
                                    avatarUrl = state.avatars[username],
                                    username = username,
                                    isOnline = username in state.onlineUsers,
                                    onClick = {
                                        viewModel.onUserSelected(username)
                                        viewModel.closeUserDiscovery()
                                    }
                                )
                            }
                        }

                        // people who are open to being found
                        if (state.publicUsers.isNotEmpty()) {
                            item {
                                DiscoveryHeader("Доступные пользователи")
                            }
                            items(state.publicUsers) { publicUser ->
                                UserItem(
                                    displayName = displayNameFor(state, publicUser.username),
                                    avatarUrl = state.avatars[publicUser.username],
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
                        // search takes over once the user starts typing
                        item {
                            DiscoveryHeader("Результаты поиска")
                        }
                        if (state.searchResults.isNotEmpty()) {
                            items(state.searchResults) { username ->
                                UserItem(
                                    displayName = displayNameFor(state, username),
                                    avatarUrl = state.avatars[username],
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
                                Icon(MaterialSymbols.RoundedFilled.Arrow_back, contentDescription = "Назад")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = atlasAppBarColor()),
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
                                Icon(MaterialSymbols.RoundedFilled.Close, contentDescription = "Закрыть")
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
    displayName: String,
    avatarUrl: String?,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    ChatTile(
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        supportingText = if (isOnline) "В сети" else "Не в сети",
        isOnline = isOnline,
        onClick = onClick,
    )
}
