package atlas.messenger.ui.components

import androidx.compose.animation.core.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.roundedfilled.Arrow_back
import com.composables.icons.materialsymbols.roundedfilled.Auto_awesome
import com.composables.icons.materialsymbols.roundedfilled.Bolt
import com.composables.icons.materialsymbols.roundedfilled.Chat
import com.composables.icons.materialsymbols.roundedfilled.Check_circle
import com.composables.icons.materialsymbols.roundedfilled.Lock
import com.composables.icons.materialsymbols.roundedfilled.Palette
import com.composables.icons.materialsymbols.roundedfilled.Speed
import com.composables.icons.materialsymbols.roundedfilled.Star
import com.composables.icons.materialsymbols.roundedfilled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import atlas.messenger.ui.PlatformBackHandler

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AtlasXScreen(
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
                        Icon(MaterialSymbols.RoundedFilled.Arrow_back, contentDescription = "Назад")
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
                            AtlasXFeatureCard("20 000 символов в сообщении", "Отправляйте большие тексты без разбиения.", MaterialSymbols.RoundedFilled.Chat)
                            AtlasXFeatureCard("Приоритет Mite", "Ускоренные ответы и расширенные AI-возможности.", MaterialSymbols.RoundedFilled.Bolt)
                            AtlasXFeatureCard("Эксклюзивные темы", "Новые стили интерфейса и расширенная персонализация.", MaterialSymbols.RoundedFilled.Palette)
                            AtlasXFeatureCard("Скоростной канал", "Более низкая задержка и приоритетная доставка.", MaterialSymbols.RoundedFilled.Speed)
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
                        AtlasXFeatureCard("20 000 символов в сообщении", "Отправляйте большие тексты без разбиения.", MaterialSymbols.RoundedFilled.Chat)
                        AtlasXFeatureCard("Приоритет Mite", "Ускоренные ответы и расширенные AI-возможности.", MaterialSymbols.RoundedFilled.Bolt)
                        AtlasXFeatureCard("Эксклюзивные темы", "Новые стили интерфейса и расширенная персонализация.", MaterialSymbols.RoundedFilled.Palette)
                        AtlasXFeatureCard("Скоростной канал", "Более низкая задержка и приоритетная доставка.", MaterialSymbols.RoundedFilled.Speed)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AtlasXPaymentScreen(
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
                        Icon(MaterialSymbols.RoundedFilled.Arrow_back, contentDescription = "Назад")
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
                                    Icon(MaterialSymbols.RoundedFilled.Lock, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        trailingContent = { Text("$4.99", fontWeight = FontWeight.Bold, color = primary) },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AtlasXActivatedScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlatformBackHandler(enabled = true, onBack = onDone)

    val primary = MaterialTheme.colorScheme.primary
    val unlockedFeatures = listOf(
        Triple<ImageVector, String, String>(
            MaterialSymbols.RoundedFilled.Auto_awesome, "20 000 символов", "Отправляйте большие тексты без ограничений"
        ),
        Triple<ImageVector, String, String>(
            MaterialSymbols.RoundedFilled.Bolt, "Приоритет Mite", "Ускоренные ответы и расширенные AI-возможности"
        ),
        Triple<ImageVector, String, String>(
            MaterialSymbols.RoundedFilled.Palette, "Эксклюзивные темы", "Новые стили интерфейса и персонализация"
        ),
        Triple<ImageVector, String, String>(
            MaterialSymbols.RoundedFilled.Speed, "Скоростной канал", "Низкая задержка и приоритетная доставка"
        ),
        Triple<ImageVector, String, String>(
            MaterialSymbols.RoundedFilled.Star, "Расширенные лимиты", "Увеличенные квоты на все операции"
        ),
        Triple<ImageVector, String, String>(
            MaterialSymbols.RoundedFilled.Verified, "X-бейдж", "Отметка подписчика в профиле"
        ),
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
                            MaterialSymbols.RoundedFilled.Check_circle,
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
                icon = { Icon(MaterialSymbols.RoundedFilled.Auto_awesome, contentDescription = null, tint = primary) },
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
                    center = Offset(58f, 72f),
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
