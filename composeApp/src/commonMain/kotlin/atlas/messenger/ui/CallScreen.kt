package atlas.messenger.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import atlas.messenger.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CallScreen(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()
    val peer = state.activeCallPeer ?: return
    val colors = MaterialTheme.colorScheme
    
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    
    val intensity by animateFloatAsState(
        targetValue = state.callAudioLevel,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
    )

    Box(modifier = Modifier.fillMaxSize().background(colors.surfaceContainerLowest)) {
        // the call gets a living waveform instead of a cold empty screen
        val infiniteTransition = rememberInfiniteTransition(label = "call-string")
        val time by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "time"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            
            val path = androidx.compose.ui.graphics.Path()
            val points = 200
            
            path.moveTo(0f, centerY)
            
            for (i in 0..points) {
                val t = i.toFloat() / points
                val x = t * width
                
                // mix in harmonics so voices feel alive, not robotic
                var dy = 0f
                val phase = time * 2f * kotlin.math.PI.toFloat()
                
                // the calm base motion
                dy += kotlin.math.sin(t * kotlin.math.PI.toFloat() + phase) * 0.5f
                // extra motion pulled from the current voice level
                dy += kotlin.math.sin(t * 3f * kotlin.math.PI.toFloat() - phase * 1.5f) * intensity * 0.4f
                dy += kotlin.math.sin(t * 7.2f * kotlin.math.PI.toFloat() + phase * 2.8f) * intensity * 0.2f
                
                // louder moments get a little nervous energy
                if (intensity > 0.5f) {
                    dy += (kotlin.math.sin(t * 20f + phase * 10f)) * 0.05f * intensity
                }

                // pin the ends so the wave feels held in place
                val envelope = kotlin.math.sin(t * kotlin.math.PI.toFloat())
                
                val amplitude = 12.dp.toPx() + (intensity * 220.dp.toPx())
                val y = centerY + (dy * amplitude * envelope)
                path.lineTo(x, y)
            }
            
            // the soft glow gives the line some warmth
            drawPath(
                path = path,
                color = colors.primary.copy(alpha = 0.15f * (0.3f + intensity)),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 24.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            // the middle glow keeps the bright edge from feeling harsh
            drawPath(
                path = path,
                color = colors.primary.copy(alpha = 0.3f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 8.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            // the sharp core is what the eye actually follows
            drawPath(
                path = path,
                color = colors.primary,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // keep the caller centered and calm
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 96.dp)
            ) {
                Surface(
                    modifier = Modifier.size(112.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = colors.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = peer.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.displayMedium,
                            color = colors.onPrimaryContainer,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    text = peer,
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )

                Text(
                    text = "ЗАЩИЩЁННЫЙ ЗВОНОК",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        // the controls stay close to the thumb, where calls need them
        val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
        HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                    onClick = viewModel::endCall,
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Завершить звонок")
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -FloatingToolbarDefaults.ScreenOffset)
                .zIndex(1f),
            colors = vibrantColors,
        ) {
            IconButton(onClick = { isMuted = !isMuted }) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Отключить микрофон",
                )
            }
            IconButton(onClick = { isSpeakerOn = !isSpeakerOn }) {
                Icon(
                    imageVector = if (isSpeakerOn)
                        Icons.AutoMirrored.Filled.VolumeUp
                    else
                        Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Динамик",
                )
            }
        }
    }
}
