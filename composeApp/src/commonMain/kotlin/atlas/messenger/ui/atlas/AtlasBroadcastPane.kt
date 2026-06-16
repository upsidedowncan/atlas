package atlas.messenger.ui.atlas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import atlas.messenger.ui.components.AtlasAppBarColor
import atlas.messenger.util.formatTime
import atlas.messenger.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AtlasBroadcastPane(viewModel: ChatViewModel, showHeader: Boolean = false) {
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AtlasAppBarColor()),
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
