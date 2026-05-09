package atlas.messenger.ui.shapes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star

// Built-in toPath returns android.graphics.Path — use forEachCubic for commonMain
private fun Morph.toComposePath(progress: Float, scale: Float, path: Path): Path {
    var first = true
    path.rewind()
    forEachCubic(progress) { bezier ->
        if (first) {
            path.moveTo(bezier.anchor0X * scale, bezier.anchor0Y * scale)
            first = false
        }
        path.cubicTo(
            bezier.control0X * scale, bezier.control0Y * scale,
            bezier.control1X * scale, bezier.control1Y * scale,
            bezier.anchor1X * scale, bezier.anchor1Y * scale,
        )
    }
    path.close()
    return path
}

// ─── Preset shape pairs ───────────────────────────────────────────────

private val circlePolygon = RoundedPolygon.circle(numVertices = 12)
private val starPolygon = RoundedPolygon.star(
    numVerticesPerRadius = 12,
    innerRadius = 2f / 3f,
    rounding = CornerRounding(1f / 6f),
)
private val hexagonPolygon = RoundedPolygon(
    numVertices = 6,
    rounding = CornerRounding(0.15f),
)
private val diamondPolygon = RoundedPolygon(
    numVertices = 4,
    rounding = CornerRounding(0.12f),
)

private val morphCircleStar = Morph(circlePolygon, starPolygon)
private val morphCircleHexagon = Morph(circlePolygon, hexagonPolygon)
private val morphCircleDiamond = Morph(circlePolygon, diamondPolygon)

// ─── MorphingBackground ───────────────────────────────────────────────

@Composable
fun MorphingBackground(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF3CBCEB),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg-transition")

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bg-progress",
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bg-rotation",
    )

    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bg-rotation2",
    )

    val morphPath1 = remember { Path() }
    val morphPath2 = remember { Path() }
    val morphPath3 = remember { Path() }

    val accentBrush = Brush.sweepGradient(
        colors = listOf(
            accentColor,
            accentColor.copy(alpha = 0.3f),
            accentColor.copy(alpha = 0.1f),
            accentColor,
        ),
        center = Offset(0.5f, 0.5f),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val s = size.minDimension

                // Shape 1: circle↔star (large, centered, subtle)
                morphCircleStar.toComposePath(progress, s * 0.35f, morphPath1)

                morphCircleHexagon.toComposePath(1f - progress, s * 0.2f, morphPath2)

                morphCircleDiamond.toComposePath((progress + 0.5f) % 1f, s * 0.12f, morphPath3)

                // Shape 3: circle↔diamond (small, offset other direction)
                morphCircleDiamond.toComposePath(
                    progress = (progress + 0.5f) % 1f,
                    scale = s * 0.12f,
                    path = morphPath3,
                )

                onDrawBehind {
                    // Shape 1 — large centered
                    rotate(rotation) {
                        translate(size.width * 0.5f, size.height * 0.5f) {
                            drawPath(
                                path = morphPath1,
                                brush = accentBrush,
                                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                                alpha = 0.15f,
                            )
                        }
                    }

                    // Shape 2 — upper-right
                    rotate(rotation2) {
                        translate(size.width * 0.75f, size.height * 0.3f) {
                            drawPath(
                                path = morphPath2,
                                brush = accentBrush,
                                style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round),
                                alpha = 0.1f,
                            )
                        }
                    }

                    // Shape 3 — lower-left
                    rotate(rotation) {
                        translate(size.width * 0.25f, size.height * 0.7f) {
                            drawPath(
                                path = morphPath3,
                                brush = accentBrush,
                                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
                                alpha = 0.08f,
                            )
                        }
                    }
                }
            },
    )
}

// ─── AnimatedEmptyState ───────────────────────────────────────────────

/**
 * Centered morphing shape animation for empty states.
 * Replaces static placeholder icons with a living, breathing shape.
 */
@Composable
fun AnimatedEmptyState(
    modifier: Modifier = Modifier,
    shapeSize: Dp = 80.dp,
    color: Color = Color(0xFF5F96E7),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty-transition")

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "empty-progress",
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "empty-rotation",
    )

    val morphPath = remember { Path() }

    val brush = Brush.sweepGradient(
        colors = listOf(
            color,
            color.copy(alpha = 0.5f),
            color.copy(alpha = 0.2f),
            color.copy(alpha = 0.6f),
            color,
        ),
        center = Offset(0.5f, 0.5f),
    )

    Box(
        modifier = modifier
            .size(shapeSize)
            .drawWithCache {
                val s = shapeSize.toPx() * 0.4f
                morphCircleStar.toComposePath(progress, s, morphPath)

                onDrawBehind {
                    rotate(rotation) {
                        translate(size.width / 2f, size.height / 2f) {
                            drawPath(
                                path = morphPath,
                                brush = brush,
                                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                                alpha = 0.6f,
                            )
                        }
                    }
                }
            },
    )
}

// ─── MorphingLoadingIndicator ─────────────────────────────────────────

/**
 * Shape-based loading indicator: circle↔star morph with rotation.
 * Drop-in replacement for basic spinners.
 */
@Composable
fun MorphingLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Dp = 3.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading-transition")

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loading-progress",
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "loading-rotation",
    )

    val morphPath = remember { Path() }

    Box(
        modifier = modifier
            .drawWithCache {
                val s = size.minDimension * 0.4f
                morphCircleStar.toComposePath(progress, s, morphPath)

                onDrawBehind {
                    rotate(rotation) {
                        translate(size.width / 2f, size.height / 2f) {
                            drawPath(
                                path = morphPath,
                                color = color,
                                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round),
                            )
                        }
                    }
                }
            },
    )
}

// ─── MorphingSendButton ───────────────────────────────────────────────

private val sendCircle = RoundedPolygon.circle(numVertices = 12)
private val sendStar = RoundedPolygon.star(
    numVerticesPerRadius = 8,
    innerRadius = 0.55f,
    rounding = CornerRounding(0.15f),
)
private val sendMorph = Morph(sendCircle, sendStar)

@Composable
fun MorphingSendButton(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF3CBCEB),
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    var pressed by remember { mutableStateOf(false) }
    val morphProgress = remember { Animatable(0f) }
    val bounceScale = remember { Animatable(1f) }

    LaunchedEffect(pressed) {
        if (pressed) {
            morphProgress.animateTo(1f, tween(180))
            bounceScale.animateTo(0.7f, tween(60))
            bounceScale.animateTo(1.2f, tween(80))
            bounceScale.animateTo(0.95f, tween(60))
            bounceScale.animateTo(1f, tween(100))
            morphProgress.animateTo(0f, tween(350))
            pressed = false
        }
    }

    Box(
        modifier = modifier
            .clickable(enabled = enabled) { pressed = true; onClick() }
            .drawWithCache {
                val s = size.minDimension * 0.42f

                onDrawBehind {
                    val path = Path()
                    sendMorph.toComposePath(morphProgress.value, s, path)

                    scale(bounceScale.value) {
                        translate(size.width / 2f, size.height / 2f) {
                            drawPath(
                                path = path,
                                color = if (enabled) color else color.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val iconAlpha = 1f - morphProgress.value
        if (iconAlpha > 0.01f) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Отправить",
                tint = Color.White.copy(alpha = iconAlpha),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
