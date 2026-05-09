package atlas.messenger.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import atlas.messenger.ui.shapes.MorphingBackground
import atlas.messenger.ui.shapes.MorphingLoadingIndicator
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
        contentAlignment = Alignment.Center,
    ) {
        MorphingBackground(accentColor = colors.primary)

        if (state.isConnecting && currentStep == AuthStep.WELCOME) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MorphingLoadingIndicator(modifier = Modifier.size(64.dp), color = colors.primary)
                Spacer(Modifier.height(24.dp))
                Text("Connecting to Atlas...", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
        } else {
            // Settings Button (Server URL)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                IconButton(
                    onClick = { viewModel.openServerUrlDialog() },
                    modifier = Modifier.align(Alignment.TopEnd)
                        
                        .clip(CircleShape)
                        .background(colors.surfaceVariant.copy(alpha = 0.5f))
                        .blur(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = colors.onSurface,
                    )
                }
                
                if (currentStep != AuthStep.WELCOME) {
                    IconButton(
                        onClick = { 
                            currentStep = when(currentStep) {
                                AuthStep.PASSWORD -> AuthStep.USERNAME
                                AuthStep.USERNAME -> AuthStep.WELCOME
                                else -> AuthStep.WELCOME
                            }
                        },
                        modifier = Modifier.align(Alignment.TopStart)
                            
                            .clip(CircleShape)
                            .background(colors.surfaceVariant.copy(alpha = 0.5f))
                            .blur(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onSurface,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(32.dp),
                color = colors.surface.copy(alpha = 0.7f),
                tonalElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.3f)),
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }
                    }
                ) { step ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (step) {
                            AuthStep.WELCOME -> {
                                WelcomeStep(
                                    onGetStarted = { currentStep = AuthStep.USERNAME },
                                    colors = colors
                                )
                            }
                            AuthStep.USERNAME -> {
                                UsernameStep(
                                    username = state.username,
                                    onUsernameChanged = viewModel::onUsernameChanged,
                                    onNext = { if (state.username.length >= 2) currentStep = AuthStep.PASSWORD },
                                    colors = colors
                                )
                            }
                            AuthStep.PASSWORD -> {
                                PasswordStep(
                                    state = state,
                                    viewModel = viewModel,
                                    colors = colors
                                )
                            }
                        }
                    }
                }
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
private fun WelcomeStep(onGetStarted: () -> Unit, colors: ColorScheme) {
    Text(
        text = "Atlas",
        style = MaterialTheme.typography.headlineLarge,
        color = colors.onSurface,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1.5).sp,
        fontSize = 48.sp
    )

    Spacer(Modifier.height(12.dp))

    Text(
        text = "Experience the next level of private messaging. Fast, secure, and beautiful.",
        style = MaterialTheme.typography.bodyLarge,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 24.sp
    )

    Spacer(Modifier.height(48.dp))

    Button(
        onClick = onGetStarted,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Text("Get Started", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
    }
}

@Composable
private fun UsernameStep(
    username: String,
    onUsernameChanged: (String) -> Unit,
    onNext: () -> Unit,
    colors: ColorScheme
) {
    Text(
        text = "What's your name?",
        style = MaterialTheme.typography.headlineSmall,
        color = colors.onSurface,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = "Pick a unique username for your Atlas account.",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(32.dp))

    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChanged,
        placeholder = { Text("Username") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outlineVariant,
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)
    )

    Spacer(Modifier.height(32.dp))

    Button(
        onClick = onNext,
        enabled = username.length >= 2,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text("Continue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordStep(
    state: atlas.messenger.viewmodel.ChatUiState,
    viewModel: ChatViewModel,
    colors: ColorScheme
) {
    Text(
        text = if (state.isRegistering) "Create a password" else "Welcome back!",
        style = MaterialTheme.typography.headlineSmall,
        color = colors.onSurface,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = if (state.isRegistering) 
            "Make sure it's strong. We use this to encrypt your local keys." 
            else "Enter your password to continue as @${state.username}",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(24.dp))
    
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SegmentedButton(
                        selected = !state.isRegistering,
                        onClick = { viewModel.setAuthMode(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier,
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = colors.primary,
                            activeContentColor = colors.onPrimary,
                        )
                    ) {
                        Text("Sign In", style = MaterialTheme.typography.labelLarge)
                    }
                    SegmentedButton(
                        selected = state.isRegistering,
                        onClick = { viewModel.setAuthMode(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier,
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = colors.primary,
                            activeContentColor = colors.onPrimary,
                        )
                    ) {
                        Text("Register", style = MaterialTheme.typography.labelLarge)
                    }
                }

    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = state.passwordInput,
        onValueChange = viewModel::onPasswordChanged,
        placeholder = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { viewModel.connect() }),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outlineVariant,
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)
    )

    AnimatedContent(targetState = state.errorMessage) { error ->
        if (error != null) {
            Text(
                text = error,
                color = colors.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        }
    }

    Spacer(Modifier.height(32.dp))

    Button(
        onClick = { viewModel.connect() },
        enabled = !state.isConnecting && state.passwordInput.length >= 4,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        if (state.isConnecting) {
            MorphingLoadingIndicator(
                modifier = Modifier.size(24.dp),
                color = colors.onPrimary,
            )
        } else {
            Text(
                if (state.isRegistering) "Create Account" else "Sign In",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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
