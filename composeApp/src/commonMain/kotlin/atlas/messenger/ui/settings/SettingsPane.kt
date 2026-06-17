package atlas.messenger.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.*
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBase64
import io.github.ismoy.imagepickerkmp.domain.models.MimeType
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import atlas.messenger.ui.components.AvatarBox
import atlas.messenger.ui.components.displayNameFor
import atlas.messenger.viewmodel.AppTheme
import atlas.messenger.viewmodel.ChatViewModel
import atlas.messenger.viewmodel.ColorPreset
import kotlinx.coroutines.launch

private val MOBILE_BREAKPOINT = 600.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsGroupHeader(text: String, colors: ColorScheme) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        ),
        color = colors.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun atlasSwitchColors(colors: ColorScheme = MaterialTheme.colorScheme, darkTheme: Boolean = false): SwitchColors {
    val isDark = colors.surface.luminance() < 0.5f
    return SwitchDefaults.colors(
        checkedThumbColor = if (isDark) colors.onPrimary else Color.White,
        checkedTrackColor = colors.primary,
        checkedBorderColor = colors.primary,
        checkedIconColor = colors.primary,
        uncheckedThumbColor = colors.outline,
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
internal fun SettingsPane(viewModel: ChatViewModel, showHeader: Boolean = true) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    var showAvatarPicker by remember { mutableStateOf(false) }
    val settingsIconColors = listOf(
        Color(0xFF5B7CFA),
        Color(0xFF16A34A),
        Color(0xFF0EA5E9),
        Color(0xFF8B5CF6),
        Color(0xFFEF4444),
    )

    val presetColors = listOf(
        0xFF0066FF.toInt() to "Синий",
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
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                                            LoadingIndicator(
                                                modifier = Modifier.size(48.dp),
                                                color = colors.primary,
                                            )
                                        }
                                    } else {
                                        AvatarBox(state.username, state.avatars[state.username], 72)
                                    }
                                }
                                Icon(
                                    MaterialSymbols.RoundedFilled.Photo_camera,
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
                                    text = displayNameFor(state.displayNames, state.username),
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
                                    MaterialSymbols.RoundedFilled.Check,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }

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

                item {
                    SettingsGroupHeader("ТЕМА ОФОРМЛЕНИЯ", colors)
                    Surface(
                        color = colors.surfaceContainer,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Светлая, тёмная или как на устройстве",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val isMobile = maxWidth < MOBILE_BREAKPOINT
                                if (isMobile) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        AppTheme.entries.forEach { theme ->
                                            val isSelected = state.theme == theme
                                            ToggleButton(
                                                checked = isSelected,
                                                onCheckedChange = { viewModel.onThemeChanged(theme) },
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = when (theme) {
                                                            AppTheme.SYSTEM -> MaterialSymbols.RoundedFilled.Phone_android
                                                            AppTheme.LIGHT -> MaterialSymbols.RoundedFilled.Light_mode
                                                            AppTheme.DARK -> MaterialSymbols.RoundedFilled.Dark_mode
                                                        },
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                    Text(
                                                        text = when (theme) {
                                                            AppTheme.SYSTEM -> "Системная"
                                                            AppTheme.LIGHT -> "Светлая"
                                                            AppTheme.DARK -> "Тёмная"
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    ButtonGroup(modifier = Modifier.fillMaxWidth()) {
                                        AppTheme.entries.forEach { theme ->
                                            val isSelected = state.theme == theme
                                            ToggleButton(
                                                checked = isSelected,
                                                onCheckedChange = { viewModel.onThemeChanged(theme) },
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = when (theme) {
                                                            AppTheme.SYSTEM -> MaterialSymbols.RoundedFilled.Phone_android
                                                            AppTheme.LIGHT -> MaterialSymbols.RoundedFilled.Light_mode
                                                            AppTheme.DARK -> MaterialSymbols.RoundedFilled.Dark_mode
                                                        },
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                    Text(
                                                        text = when (theme) {
                                                            AppTheme.SYSTEM -> "Системная"
                                                            AppTheme.LIGHT -> "Светлая"
                                                            AppTheme.DARK -> "Тёмная"
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }

                item {
                    SettingsGroupHeader("ПРИВАТНОСТЬ", colors)
                    SettingsTile(
                        icon = MaterialSymbols.RoundedFilled.Public,
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
                        icon = MaterialSymbols.RoundedFilled.Mic,
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

                item {
                    SettingsGroupHeader("СЕРВЕР", colors)
                    SettingsTile(
                        icon = MaterialSymbols.RoundedFilled.Cloud,
                        iconContainerColor = settingsIconColors[2],
                        title = "Адрес сервера",
                        subtitle = state.serverUrl,
                        colors = colors,
                        shape = RoundedCornerShape(28.dp),
                        onClick = { viewModel.openServerUrlDialog() },
                        trailing = {
                            IconButton(
                                shapes = IconButtonDefaults.shapes(),
                                onClick = { viewModel.openServerUrlDialog() },
                            ) {
                                Icon(
                                    MaterialSymbols.RoundedFilled.Edit,
                                    contentDescription = "Изменить",
                                    tint = colors.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }

                item { Spacer(Modifier.height(20.dp)) }

                item {
                    SettingsGroupHeader("БЕЗОПАСНОСТЬ", colors)
                    SettingsTile(
                        icon = MaterialSymbols.RoundedFilled.Fingerprint,
                        iconContainerColor = settingsIconColors[3],
                        title = "Отпечаток ключа",
                        subtitle = state.publicKeyFingerprint,
                        colors = colors,
                        shape = RoundedCornerShape(28.dp),
                    )
                }

                item { Spacer(Modifier.height(20.dp)) }

                item {
                    SettingsGroupHeader("АККАУНТ", colors)
                    SettingsTile(
                        icon = MaterialSymbols.RoundedFilled.Qr_code_scanner,
                        iconContainerColor = settingsIconColors[0],
                        title = "Сканировать QR",
                        subtitle = "Подключить веб-версию",
                        colors = colors,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                        onClick = { viewModel.openQrScanner() },
                    )
                    Spacer(Modifier.height(3.dp))
                    SettingsTile(
                        icon = MaterialSymbols.RoundedFilled.Logout,
                        iconContainerColor = settingsIconColors[4],
                        title = "Выйти из аккаунта",
                        subtitle = "Отключиться и очистить локальные данные",
                        colors = colors,
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                        onClick = { viewModel.disconnect() },
                    )
                }

                item { Spacer(Modifier.height(32.dp)) }

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

    if (state.showQrScanner) {
        QrScannerDialog(
            tokenInput = state.qrLoginTokenInput,
            onTokenInputChanged = viewModel::onQrTokenInputChanged,
            onConfirm = viewModel::confirmQrLogin,
            onDismiss = viewModel::closeQrScanner,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrScannerDialog(
    tokenInput: String,
    onTokenInputChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Подключить веб-версию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Отсканируйте QR-код на экране веб-версии или введите токен вручную",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextField(
                    value = tokenInput,
                    onValueChange = onTokenInputChanged,
                    label = { Text("Токен") },
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
                onClick = onConfirm,
                enabled = tokenInput.isNotBlank(),
            ) {
                Text("Подключить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
}
