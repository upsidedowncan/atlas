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
        // --- Premium "Physical String" Implementation ---
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
                
                // Advanced wave synthesis: 
                // Fundamental + Harmonics + Non-linear perturbation
                var dy = 0f
                val phase = time * 2f * kotlin.math.PI.toFloat()
                
                // Base vibration
                dy += kotlin.math.sin(t * kotlin.math.PI.toFloat() + phase) * 0.5f
                // Speech-driven harmonics
                dy += kotlin.math.sin(t * 3f * kotlin.math.PI.toFloat() - phase * 1.5f) * intensity * 0.4f
                dy += kotlin.math.sin(t * 7.2f * kotlin.math.PI.toFloat() + phase * 2.8f) * intensity * 0.2f
                
                // Add some "tension" and "noise" when loud
                if (intensity > 0.5f) {
                    dy += (kotlin.math.sin(t * 20f + phase * 10f)) * 0.05f * intensity
                }

                // Bell envelope (fixed ends)
                val envelope = kotlin.math.sin(t * kotlin.math.PI.toFloat())
                
                val amplitude = 12.dp.toPx() + (intensity * 220.dp.toPx())
                val y = centerY + (dy * amplitude * envelope)
                path.lineTo(x, y)
            }
            
            // Render with layers for "Glow"
            // Layer 1: Wide blur glow
            drawPath(
                path = path,
                color = colors.primary.copy(alpha = 0.15f * (0.3f + intensity)),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 24.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            // Layer 2: Medium glow
            drawPath(
                path = path,
                color = colors.primary.copy(alpha = 0.3f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 8.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            // Layer 3: Core string
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
            // Caller ID Card
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
                    text = "SECURE AUDIO CALL",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        // --- Official Material 3 Expressive Floating Toolbar ---
        // Positioned at bottom-center exactly per the M3 Expressive spec.
        val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
        HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                    onClick = viewModel::endCall,
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End Call")
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
                    contentDescription = "Mute",
                )
            }
            IconButton(onClick = { isSpeakerOn = !isSpeakerOn }) {
                Icon(
                    imageVector = if (isSpeakerOn)
                        Icons.AutoMirrored.Filled.VolumeUp
                    else
                        Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Speaker",
                )
            }
        }
    }
}
