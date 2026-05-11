package atlas.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import atlas.messenger.data.PublicUserInfo
import atlas.messenger.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDiscoveryScreen(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }

    BoxWithConstraints {
        val isMobile = maxWidth < 600.dp

        val content: @Composable (PaddingValues) -> Unit = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(colors.background)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.onSearchQueryChanged(it)
                        if (it.isNotBlank()) viewModel.submitSearch()
                    },
                    placeholder = { Text("Поиск по имени пользователя...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = colors.outlineVariant.copy(alpha = 0.5f)
                    )
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
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                        title = { Text("Найти людей", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(
                                onClick = viewModel::closeUserDiscovery,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                            }
                        }
                    )
                },
                content = content
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colors.background,
                tonalElevation = 1.dp
            ) {
                Column {
                    CenterAlignedTopAppBar(
                        title = { Text("Найти людей", fontWeight = FontWeight.Bold) },
                        actions = {
                            IconButton(onClick = viewModel::closeUserDiscovery) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        }
                    )
                    content(PaddingValues(0.dp))
                }
            }
        }
    }
}

@Composable
private fun DiscoveryHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
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
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isOnline) {
                val onlineColor = if (colors.surface.luminance() > 0.5f) Color(0xFF2E7D32) else Color(0xFF4CAF50)
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(onlineColor)
                    )
                }
            }
        }

        Column {
            Text(
                text = username,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Text(
                text = if (isOnline) "В сети" else "Не в сети",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOnline) colors.primary else colors.onSurfaceVariant
            )
        }
    }
}
