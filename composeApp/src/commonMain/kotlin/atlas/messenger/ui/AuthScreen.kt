package atlas.messenger.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.math.cos
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
        modifier = Modifier.fillMaxSize().background(colors.background),
    ) {
        ExpressiveAuthBackdrop(colors)

        if (state.isConnecting && currentStep == AuthStep.WELCOME) {
            ConnectingState(colors)
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isMobile = maxWidth < 720.dp

                if (isMobile) {
                    MobileAuthLayout(state, currentStep, { currentStep = it }, viewModel, colors)
                } else {
                    DesktopAuthLayout(state, currentStep, { currentStep = it }, viewModel, colors)
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 76.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ConversationPreview(
            modifier = Modifier.fillMaxWidth().weight(1f),
            compact = currentStep != AuthStep.WELCOME,
            colors = colors,
        )

        AuthStepContent(state, currentStep, onStepChanged, viewModel, colors, modifier = Modifier.fillMaxWidth())
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
            modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 56.dp, top = 96.dp, end = 32.dp, bottom = 56.dp),
            compact = false,
            colors = colors,
        )

        Column(
            modifier = Modifier
                .widthIn(min = 420.dp, max = 480.dp)
                .fillMaxHeight()
                .background(colors.surfaceContainerHigh.copy(alpha = 0.95f))
                .padding(horizontal = 40.dp, vertical = 92.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            AuthStepContent(state, currentStep, onStepChanged, viewModel, colors, modifier = Modifier.fillMaxWidth())
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
            val springSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
            if (targetState.ordinal > initialState.ordinal) {
                (slideInHorizontally(springSpec) { it / 3 } + fadeIn(springSpec))
                    .togetherWith(slideOutHorizontally(springSpec) { -it / 3 } + fadeOut(springSpec))
            } else {
                (slideInHorizontally(springSpec) { -it / 3 } + fadeIn(springSpec))
                    .togetherWith(slideOutHorizontally(springSpec) { it / 3 } + fadeOut(springSpec))
            }
        },
        modifier = modifier,
    ) { step ->
        Column(modifier = Modifier.fillMaxWidth()) {
            when (step) {
                AuthStep.WELCOME -> WelcomeStep(onGetStarted = { onStepChanged(AuthStep.USERNAME) }, colors = colors)
                AuthStep.USERNAME -> UsernameStep(
                    username = state.username,
                    onUsernameChanged = viewModel::onUsernameChanged,
                    onNext = { if (state.username.length >= 2) onStepChanged(AuthStep.PASSWORD) },
                    colors = colors,
                )
                AuthStep.PASSWORD -> PasswordStep(state = state, viewModel = viewModel, colors = colors)
            }
        }
    }
}

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit, colors: ColorScheme) {
    Text(
        text = "Atlas",
        style = MaterialTheme.typography.displayLarge,
        color = colors.onSurface,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1).sp,
    )

    Spacer(Modifier.height(6.dp))

    Text(
        text = "Private messenger",
        style = MaterialTheme.typography.titleLarge,
        color = colors.primary,
        fontWeight = FontWeight.SemiBold,
    )

    Spacer(Modifier.height(16.dp))

    Text(
        text = "Быстрый приватный мессенджер для разговоров, которые должны ощущаться спокойно.",
        style = MaterialTheme.typography.bodyLarge,
        color = colors.onSurfaceVariant,
        lineHeight = 24.sp,
    )

    Spacer(Modifier.height(40.dp))

    Button(
        onClick = onGetStarted,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
    ) {
        Text("Начать", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(12.dp))
        Icon(MaterialSymbols.RoundedFilled.Arrow_forward, contentDescription = null, modifier = Modifier.size(22.dp))
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
        style = MaterialTheme.typography.headlineLarge,
        color = colors.onSurface,
        fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = "Так друзья будут находить вас в Atlas.",
        style = MaterialTheme.typography.bodyLarge,
        color = colors.onSurfaceVariant,
    )

    Spacer(Modifier.height(32.dp))

    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChanged,
        placeholder = { Text("username") },
        prefix = { Text("@", color = colors.primary, fontWeight = FontWeight.Bold) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outlineVariant,
            focusedContainerColor = colors.surfaceContainerLow,
            unfocusedContainerColor = colors.surfaceContainerLow,
        ),
    )

    Spacer(Modifier.height(28.dp))

    val buttonScale by animateFloatAsState(
        targetValue = if (username.length >= 2) 1f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    Button(
        onClick = onNext,
        enabled = username.length >= 2,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
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
        style = MaterialTheme.typography.headlineLarge,
        color = colors.onSurface,
        fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = if (state.isRegistering) "Придумайте пароль для @${state.username}." else "Введите пароль, чтобы продолжить как @${state.username}.",
        style = MaterialTheme.typography.bodyLarge,
        color = colors.onSurfaceVariant,
    )

    Spacer(Modifier.height(28.dp))

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = !state.isRegistering,
            onClick = { viewModel.setAuthMode(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = colors.primaryContainer,
                activeContentColor = colors.onPrimaryContainer,
            ),
        ) {
            Text("Вход", style = MaterialTheme.typography.labelLarge)
        }
        SegmentedButton(
            selected = state.isRegistering,
            onClick = { viewModel.setAuthMode(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = colors.primaryContainer,
                activeContentColor = colors.onPrimaryContainer,
            ),
        ) {
            Text("Регистрация", style = MaterialTheme.typography.labelLarge)
        }
    }

    Spacer(Modifier.height(20.dp))

    OutlinedTextField(
        value = state.passwordInput,
        onValueChange = viewModel::onPasswordChanged,
        placeholder = { Text("Пароль") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { viewModel.connect() }),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outlineVariant,
            focusedContainerColor = colors.surfaceContainerLow,
            unfocusedContainerColor = colors.surfaceContainerLow,
        ),
    )

    AnimatedContent(targetState = state.errorMessage, label = "error") { error ->
        if (error != null) {
            Text(
                text = error,
                color = colors.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )
        }
    }

    Spacer(Modifier.height(28.dp))

    Button(
        onClick = { viewModel.connect() },
        enabled = !state.isConnecting && state.passwordInput.length >= 4,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
    ) {
        if (state.isConnecting) {
            LoadingIndicator(modifier = Modifier.size(28.dp), color = colors.onPrimary)
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
    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        if (currentStep != AuthStep.WELCOME) {
            IconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh),
            ) {
                Icon(MaterialSymbols.RoundedFilled.Arrow_back, contentDescription = "Назад", tint = colors.onSurface)
            }
        }

        IconButton(
            shapes = IconButtonDefaults.shapes(),
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.surfaceContainerHigh),
        ) {
            Icon(MaterialSymbols.RoundedFilled.Settings, contentDescription = "Настройки", tint = colors.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConnectingState(colors: ColorScheme) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingIndicator(modifier = Modifier.size(72.dp), color = colors.primary)
            Spacer(Modifier.height(28.dp))
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
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Text(
            text = "Ваши чаты, без лишнего шума",
            style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displayMedium,
            color = colors.onBackground,
            fontWeight = FontWeight.Black,
            lineHeight = if (compact) 36.sp else 56.sp,
        )
        Spacer(Modifier.height(if (compact) 20.dp else 36.dp))
        PreviewBubble(text = "Ты уже в Atlas?", own = false, colors = colors, modifier = Modifier.fillMaxWidth(0.72f))
        Spacer(Modifier.height(12.dp))
        PreviewBubble(text = "Да. Всё быстро, чисто и без огромных экранов с карточками.", own = true, colors = colors, modifier = Modifier.align(Alignment.End).fillMaxWidth(0.82f))
        if (!compact) {
            Spacer(Modifier.height(12.dp))
            PreviewBubble(text = "Вот так и должен ощущаться мессенджер.", own = false, colors = colors, modifier = Modifier.fillMaxWidth(0.68f))
        }
    }
}

@Composable
private fun PreviewBubble(
    text: String,
    own: Boolean,
    colors: ColorScheme,
    modifier: Modifier = Modifier,
) {
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
    val yOffset = (sin((drift + if (own) 0.35f else 0f) * 6.28318f) * 5f).dp
    val bubbleColor = if (own) colors.primaryContainer else colors.surfaceContainerHigh
    val textColor = if (own) colors.onPrimaryContainer else colors.onSurface

    Box(
        modifier = modifier
            .offset(y = yOffset)
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(
                RoundedCornerShape(
                    topStart = 24.dp, topEnd = 24.dp,
                    bottomStart = if (own) 24.dp else 8.dp,
                    bottomEnd = if (own) 8.dp else 24.dp,
                )
            )
            .background(bubbleColor)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(text = text, color = textColor, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)
    }
}

@Composable
private fun ExpressiveAuthBackdrop(colors: ColorScheme) {
    val infiniteTransition = rememberInfiniteTransition(label = "auth-bg-motion")
    val motion by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(20000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "auth-bg-motion-progress",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.primary.copy(alpha = 0.08f),
                    colors.background,
                    colors.tertiary.copy(alpha = 0.05f),
                ),
                center = Offset(w * 0.3f, h * 0.4f),
                radius = w * 0.8f,
            )
        )

        val shapeColor1 = colors.primary.copy(alpha = 0.06f)
        val shapeColor2 = colors.tertiary.copy(alpha = 0.05f)
        val shapeColor3 = colors.secondary.copy(alpha = 0.04f)

        for (i in 0..5) {
            val phase = motion + i * 0.17f
            val x = w * ((i * 0.18f + 0.05f + sin(phase * 3.14f) * 0.03f) % 1f)
            val y = h * ((i * 0.22f + 0.08f + cos(phase * 2.7f) * 0.02f) % 1f)
            val radius = w * (0.08f + (i % 3) * 0.04f)
            val color = when (i % 3) { 0 -> shapeColor1; 1 -> shapeColor2; else -> shapeColor3 }

            drawCircle(color = color, radius = radius, center = Offset(x, y))
        }

        for (i in 0..3) {
            val phase = motion + i * 0.25f
            val x = w * ((i * 0.28f + 0.1f + cos(phase * 2.2f) * 0.04f) % 1f)
            val y = h * ((i * 0.3f + 0.15f + sin(phase * 1.8f) * 0.03f) % 1f)
            val bw = w * (0.12f + (i % 2) * 0.06f)
            val bh = 48.dp.toPx() + (i % 3) * 16.dp.toPx()
            val color = when (i % 2) { 0 -> shapeColor2; else -> shapeColor1 }

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(32.dp.toPx()),
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
                Text("Введите адрес сервера для подключения", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("ws://адрес:порт") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onUrlChanged(urlText.trim()); onDismiss() }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
