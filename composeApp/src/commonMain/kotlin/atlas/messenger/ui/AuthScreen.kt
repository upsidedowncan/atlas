package atlas.messenger.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Arrow_back
import com.composables.icons.materialsymbols.roundedfilled.Arrow_forward
import com.composables.icons.materialsymbols.roundedfilled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import kotlin.math.sin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import atlas.messenger.viewmodel.ChatUiState
import atlas.messenger.viewmodel.ChatViewModel

enum class AuthStep { WELCOME, USERNAME, PASSWORD }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthScreen(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    var currentStep by remember { mutableStateOf(AuthStep.WELCOME) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        MessengerAuthBackdrop(colors)

        if (state.isConnecting && currentStep == AuthStep.WELCOME) {
            ConnectingState(colors)
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isMobile = maxWidth < 720.dp

                if (isMobile) {
                    MobileAuthLayout(
                        state = state,
                        currentStep = currentStep,
                        onStepChanged = { currentStep = it },
                        viewModel = viewModel,
                        colors = colors,
                    )
                } else {
                    DesktopAuthLayout(
                        state = state,
                        currentStep = currentStep,
                        onStepChanged = { currentStep = it },
                        viewModel = viewModel,
                        colors = colors,
                    )
                }

                AuthTopControls(
                    currentStep = currentStep,
                    onBack = {
                        currentStep = when (currentStep) {
                            AuthStep.PASSWORD -> AuthStep.USERNAME
                            AuthStep.USERNAME -> AuthStep.WELCOME
                            AuthStep.WELCOME -> AuthStep.WELCOME
                        }
                    },
                    onSettings = viewModel::openServerUrlDialog,
                    colors = colors,
                )
            }
        }
    }

    if (state.showServerUrlDialog) {
        AuthServerUrlDialog(
            currentUrl = state.serverUrl,
            onUrlChanged = viewModel::onServerUrlChanged,
            onDismiss = viewModel::closeServerUrlDialog,
        )
    }
}

@Composable
private fun MobileAuthLayout(
    state: ChatUiState,
    currentStep: AuthStep,
    onStepChanged: (AuthStep) -> Unit,
    viewModel: ChatViewModel,
    colors: ColorScheme,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 76.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ConversationPreview(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            compact = currentStep != AuthStep.WELCOME,
            colors = colors,
        )

        AuthStepContent(
            state = state,
            currentStep = currentStep,
            onStepChanged = onStepChanged,
            viewModel = viewModel,
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DesktopAuthLayout(
    state: ChatUiState,
    currentStep: AuthStep,
    onStepChanged: (AuthStep) -> Unit,
    viewModel: ChatViewModel,
    colors: ColorScheme,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        ConversationPreview(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 56.dp, top = 96.dp, end = 32.dp, bottom = 56.dp),
            compact = false,
            colors = colors,
        )

        Column(
            modifier = Modifier
                .widthIn(min = 420.dp, max = 480.dp)
                .fillMaxHeight()
                .background(colors.surface.copy(alpha = 0.92f))
                .padding(horizontal = 40.dp, vertical = 92.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            AuthStepContent(
                state = state,
                currentStep = currentStep,
                onStepChanged = onStepChanged,
                viewModel = viewModel,
                colors = colors,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AuthStepContent(
    state: ChatUiState,
    currentStep: AuthStep,
    onStepChanged: (AuthStep) -> Unit,
    viewModel: ChatViewModel,
    colors: ColorScheme,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally { it / 2 } + fadeIn()).togetherWith(slideOutHorizontally { -it / 2 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 2 } + fadeIn()).togetherWith(slideOutHorizontally { it / 2 } + fadeOut())
            }
        },
        modifier = modifier,
    ) { step ->
        Column(modifier = Modifier.fillMaxWidth()) {
            when (step) {
                AuthStep.WELCOME -> WelcomeStep(
                    onGetStarted = { onStepChanged(AuthStep.USERNAME) },
                    colors = colors,
                )

                AuthStep.USERNAME -> UsernameStep(
                    username = state.username,
                    onUsernameChanged = viewModel::onUsernameChanged,
                    onNext = { if (state.username.length >= 2) onStepChanged(AuthStep.PASSWORD) },
                    colors = colors,
                )

                AuthStep.PASSWORD -> PasswordStep(
                    state = state,
                    viewModel = viewModel,
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit, colors: ColorScheme) {
    Text(
        text = "Atlas",
        style = MaterialTheme.typography.displaySmall,
        color = colors.onSurface,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
    )

    Spacer(Modifier.height(10.dp))

    Text(
        text = "Быстрый приватный мессенджер для разговоров, которые должны ощущаться спокойно.",
        style = MaterialTheme.typography.bodyLarge,
        color = colors.onSurfaceVariant,
        lineHeight = 24.sp,
    )

    Spacer(Modifier.height(32.dp))

    Button(
        onClick = onGetStarted,
        modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Text("Начать", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Icon(MaterialSymbols.RoundedFilled.Arrow_forward, contentDescription = null)
    }
}

@Composable
private fun UsernameStep(
    username: String,
    onUsernameChanged: (String) -> Unit,
    onNext: () -> Unit,
    colors: ColorScheme,
) {
    Text(
        text = "Выберите имя",
        style = MaterialTheme.typography.headlineMedium,
        color = colors.onSurface,
        fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = "Так друзья будут находить вас в Atlas.",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
    )

    Spacer(Modifier.height(28.dp))

    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChanged,
        placeholder = { Text("username") },
        prefix = { Text("@") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outlineVariant,
        ),
    )

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = onNext,
        enabled = username.length >= 2,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Text("Продолжить", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PasswordStep(
    state: ChatUiState,
    viewModel: ChatViewModel,
    colors: ColorScheme,
) {
    Text(
        text = if (state.isRegistering) "Создать аккаунт" else "С возвращением",
        style = MaterialTheme.typography.headlineMedium,
        color = colors.onSurface,
        fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = if (state.isRegistering) {
            "Придумайте пароль для @${state.username}."
        } else {
            "Введите пароль, чтобы продолжить как @${state.username}."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
    )

    Spacer(Modifier.height(24.dp))

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = !state.isRegistering,
            onClick = { viewModel.setAuthMode(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = colors.primary,
                activeContentColor = colors.onPrimary,
            ),
        ) {
            Text("Вход", style = MaterialTheme.typography.labelLarge)
        }
        SegmentedButton(
            selected = state.isRegistering,
            onClick = { viewModel.setAuthMode(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = colors.primary,
                activeContentColor = colors.onPrimary,
            ),
        ) {
            Text("Регистрация", style = MaterialTheme.typography.labelLarge)
        }
    }

    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = state.passwordInput,
        onValueChange = viewModel::onPasswordChanged,
        placeholder = { Text("Пароль") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { viewModel.connect() }),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outlineVariant,
        ),
    )

    AnimatedContent(targetState = state.errorMessage) { error ->
        if (error != null) {
            Text(
                text = error,
                color = colors.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = { viewModel.connect() },
        enabled = !state.isConnecting && state.passwordInput.length >= 4,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        if (state.isConnecting) {
            LoadingIndicator(
                modifier = Modifier.size(24.dp),
                color = colors.onPrimary,
            )
        } else {
            Text(
                if (state.isRegistering) "Создать аккаунт" else "Войти",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AuthTopControls(
    currentStep: AuthStep,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    colors: ColorScheme,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        if (currentStep != AuthStep.WELCOME) {
            IconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(colors.surface.copy(alpha = 0.72f)),
            ) {
                Icon(
                    imageVector = MaterialSymbols.RoundedFilled.Arrow_back,
                    contentDescription = "Назад",
                    tint = colors.onSurface,
                )
            }
        }

        IconButton(
            shapes = IconButtonDefaults.shapes(),
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(colors.surface.copy(alpha = 0.72f)),
        ) {
            Icon(
                imageVector = MaterialSymbols.RoundedFilled.Settings,
                contentDescription = "Настройки",
                tint = colors.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConnectingState(colors: ColorScheme) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingIndicator(
                modifier = Modifier.size(64.dp),
                color = colors.primary,
            )
            Spacer(Modifier.height(24.dp))
            Text("Подключение к Atlas...", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConversationPreview(
    modifier: Modifier = Modifier,
    compact: Boolean,
    colors: ColorScheme,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Ваши чаты, без лишнего шума",
            style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall,
            color = colors.onBackground,
            fontWeight = FontWeight.Black,
            lineHeight = if (compact) 34.sp else 54.sp,
        )
        Spacer(Modifier.height(if (compact) 18.dp else 32.dp))
        ChatBubble(
            text = "Ты уже в Atlas?",
            own = false,
            colors = colors,
            modifier = Modifier.fillMaxWidth(0.72f),
        )
        Spacer(Modifier.height(10.dp))
        ChatBubble(
            text = "Да. Всё быстро, чисто и без огромных экранов с карточками.",
            own = true,
            colors = colors,
            modifier = Modifier.align(Alignment.End).fillMaxWidth(0.82f),
        )
        if (!compact) {
            Spacer(Modifier.height(10.dp))
            ChatBubble(
                text = "Вот так и должен ощущаться мессенджер.",
                own = false,
                colors = colors,
                modifier = Modifier.fillMaxWidth(0.68f),
            )
        }
    }
}

@Composable
private fun ChatBubble(
    text: String,
    own: Boolean,
    colors: ColorScheme,
    modifier: Modifier = Modifier,
) {
    // the preview bubbles should feel alive, but never distract from signing in
    val floatLoop = rememberInfiniteTransition(label = "auth-bubble-float")
    val drift by floatLoop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (own) 3600 else 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "auth-bubble-drift",
    )
    val yOffset = (sin((drift + if (own) 0.35f else 0f) * 6.28318f) * 4f).dp
    val bubbleColor = if (own) colors.primary else colors.surface.copy(alpha = 0.86f)
    val textColor = if (own) colors.onPrimary else colors.onSurface
    Box(
        modifier = modifier
            .offset(y = yOffset)
            .clip(
                RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp,
                    bottomStart = if (own) 22.dp else 6.dp,
                    bottomEnd = if (own) 6.dp else 22.dp,
                )
            )
            .background(bubbleColor)
            .padding(horizontal = 18.dp, vertical = 13.dp),
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 23.sp,
        )
    }
}

@Composable
private fun MessengerAuthBackdrop(colors: ColorScheme) {
    // slow background drift keeps the screen warm without going back to flying shapes
    val infiniteTransition = rememberInfiniteTransition(label = "auth-bg-motion")
    val motion by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "auth-bg-motion-progress",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    colors.background,
                    colors.primary.copy(alpha = 0.12f),
                    colors.tertiary.copy(alpha = 0.10f),
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
        )

        val bubbleColor = colors.primary.copy(alpha = 0.055f)
        val lineColor = colors.onBackground.copy(alpha = 0.035f)
        for (i in 0..8) {
            val x = size.width * ((i * 0.137f + 0.08f + motion * 0.055f) % 1f)
            val y = size.height * ((i * 0.211f + 0.05f + sin((motion + i * 0.13f) * 6.28318f) * 0.018f) % 1f)
            val w = size.width * (0.16f + (i % 3) * 0.045f)
            val h = 36.dp.toPx() + (i % 4) * 10.dp.toPx()
            drawRoundRect(
                color = bubbleColor,
                topLeft = Offset(x, y),
                size = Size(w, h),
                cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
            )
        }
        for (i in 0..6) {
            val x = size.width * (i / 6f)
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x + size.width * 0.18f, size.height),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthServerUrlDialog(
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
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
}
