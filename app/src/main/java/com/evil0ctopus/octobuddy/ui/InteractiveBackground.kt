package com.evil0ctopus.octobuddy.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.evil0ctopus.octobuddy.ui.theme.Brand
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Bubble(
    var x: Float,
    var y: Float,
    val radius: Float,
    val speed: Float,
    val wobble: Float,
    val phase: Float,
    val alpha: Float,
)

private data class Ripple(
    val origin: Offset,
    var ageMs: Float,
    val maxRadius: Float,
    val lifeMs: Float = 900f,
)

private data class AmbientParticle(
    val xFrac: Float,
    val yFrac: Float,
    val size: Float,
    val speed: Float,
    val phase: Float,
)

/**
 * Interactive cyber-ocean background: deep navy gradient, soft caustics,
 * hex/circuit accents, rising bubbles, parallax on drag, ripples on tap.
 */
@Composable
fun InteractiveCyberOceanBackground(
    modifier: Modifier = Modifier,
    onBackgroundTap: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    var parallaxX by remember { mutableFloatStateOf(0f) }
    var parallaxY by remember { mutableFloatStateOf(0f) }
    var targetParallaxX by remember { mutableFloatStateOf(0f) }
    var targetParallaxY by remember { mutableFloatStateOf(0f) }

    val ripples = remember { mutableStateListOf<Ripple>() }
    var frameTick by remember { mutableFloatStateOf(0f) }
    val bubbles = remember {
        MutableList(28) {
            Bubble(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = 3f + Random.nextFloat() * 10f,
                speed = 0.015f + Random.nextFloat() * 0.04f,
                wobble = 0.008f + Random.nextFloat() * 0.02f,
                phase = Random.nextFloat() * (PI * 2).toFloat(),
                alpha = 0.15f + Random.nextFloat() * 0.35f,
            )
        }
    }
    val nodes = remember {
        List(18) {
            AmbientParticle(
                xFrac = Random.nextFloat(),
                yFrac = Random.nextFloat(),
                size = 1.5f + Random.nextFloat() * 2.5f,
                speed = 0.2f + Random.nextFloat() * 0.6f,
                phase = Random.nextFloat() * (PI * 2).toFloat(),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "ocean")
    val causticPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "caustic",
    )
    val circuitPulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "circuit",
    )
    val hexSpin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(90000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hex",
    )

    val maxParallaxPx = with(density) { 28.dp.toPx() }

    LaunchedEffect(Unit) {
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last == 0L) {
                    last = now
                    return@withFrameNanos
                }
                val dt = ((now - last) / 1_000_000f).coerceIn(0f, 48f)
                last = now

                // Ease parallax back toward target, then slowly toward rest.
                parallaxX += (targetParallaxX - parallaxX) * 0.12f
                parallaxY += (targetParallaxY - parallaxY) * 0.12f
                targetParallaxX *= 0.96f
                targetParallaxY *= 0.96f

                val iter = bubbles.listIterator()
                while (iter.hasNext()) {
                    val b = iter.next()
                    b.y -= b.speed * (dt / 16f) * 0.018f
                    b.x += sin(b.phase + b.y * 8f) * b.wobble * (dt / 16f) * 0.01f
                    if (b.y < -0.05f) {
                        b.y = 1.05f
                        b.x = Random.nextFloat()
                    }
                }

                val ripIter = ripples.listIterator()
                while (ripIter.hasNext()) {
                    val r = ripIter.next()
                    r.ageMs += dt
                    if (r.ageMs >= r.lifeMs) ripIter.remove()
                }
                frameTick = (frameTick + dt) % 1_000_000f
            }
        }
    }

    // Reading frameTick invalidates the Canvas each animation frame.
    val _tick = frameTick

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        targetParallaxX =
                            (targetParallaxX + dragAmount.x * 0.35f).coerceIn(-maxParallaxPx, maxParallaxPx)
                        targetParallaxY =
                            (targetParallaxY + dragAmount.y * 0.35f).coerceIn(-maxParallaxPx, maxParallaxPx)
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    ripples += Ripple(
                        origin = offset,
                        ageMs = 0f,
                        maxRadius = minOf(size.width, size.height).toFloat() * (0.18f + Random.nextFloat() * 0.12f),
                    )
                    // Spawn a few bubbles at tap
                    repeat(4) {
                        bubbles += Bubble(
                            x = (offset.x / size.width).coerceIn(0f, 1f) +
                                (Random.nextFloat() - 0.5f) * 0.06f,
                            y = (offset.y / size.height).coerceIn(0f, 1f),
                            radius = 4f + Random.nextFloat() * 8f,
                            speed = 0.04f + Random.nextFloat() * 0.05f,
                            wobble = 0.02f,
                            phase = Random.nextFloat() * 6.28f,
                            alpha = 0.45f,
                        )
                    }
                    while (bubbles.size > 48) bubbles.removeAt(0)
                    onBackgroundTap?.invoke()
                }
            },
    ) {
        val w = size.width
        val h = size.height
        val px = parallaxX
        val py = parallaxY
        // Keep ambient layers alive (read by composition).
        @Suppress("UNUSED_EXPRESSION")
        _tick

        // Deep gradient base
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Brand.NavyDeep,
                    Brand.Navy,
                    Color(0xFF0A2A38),
                    Brand.NavyDeep,
                ),
            ),
        )

        // Soft caustic bands (far layer — more parallax)
        val farX = px * 0.35f
        val farY = py * 0.35f
        for (i in 0 until 5) {
            val yBase = h * (0.15f + i * 0.16f) + sin(causticPhase + i) * 18f + farY
            val path = Path().apply {
                moveTo(-40f + farX, yBase)
                var x = -40f
                while (x < w + 40f) {
                    val yy = yBase +
                        sin(causticPhase * 1.3f + x * 0.012f + i) * 22f +
                        cos(causticPhase * 0.7f + x * 0.008f) * 10f
                    lineTo(x + farX, yy)
                    x += 28f
                }
            }
            drawPath(
                path = path,
                color = Brand.Cyan.copy(alpha = 0.04f + i * 0.01f),
                style = Stroke(width = 18f - i),
            )
        }

        // Hex lattice (mid layer)
        val midX = px * 0.65f
        val midY = py * 0.65f
        val hexR = 46f
        val hexH = (hexR * 1.732f)
        var row = 0
        var y = -hexH + midY
        while (y < h + hexH) {
            var x = -hexR + midX + if (row % 2 == 0) 0f else hexR * 0.75f
            while (x < w + hexR) {
                drawHex(
                    center = Offset(x, y),
                    radius = hexR,
                    color = Brand.Cyan.copy(alpha = 0.045f * circuitPulse),
                    stroke = 1.2f,
                )
                x += hexR * 1.5f
            }
            y += hexH * 0.5f
            row++
        }

        // Large soft brand hex behind pet zone
        rotate(degrees = hexSpin * 0.02f, pivot = Offset(w * 0.5f + midX * 0.3f, h * 0.38f + midY * 0.3f)) {
            drawHex(
                center = Offset(w * 0.5f + midX * 0.3f, h * 0.38f + midY * 0.3f),
                radius = w.coerceAtMost(h) * 0.42f,
                color = Brand.Cyan.copy(alpha = 0.12f * circuitPulse),
                stroke = 2.5f,
            )
            drawHex(
                center = Offset(w * 0.5f + midX * 0.3f, h * 0.38f + midY * 0.3f),
                radius = w.coerceAtMost(h) * 0.36f,
                color = Brand.Cyan.copy(alpha = 0.06f),
                stroke = 1.2f,
            )
        }

        // Circuit nodes + traces (near-ish)
        val nearX = px
        val nearY = py
        nodes.forEachIndexed { i, n ->
            val nx = n.xFrac * w + nearX * 0.4f + sin(causticPhase * n.speed + n.phase) * 6f
            val ny = n.yFrac * h + nearY * 0.4f + cos(causticPhase * n.speed + n.phase) * 4f
            val glow = 0.25f + 0.35f * ((sin(causticPhase * 2f + n.phase) + 1f) * 0.5f)
            drawCircle(
                color = Brand.Cyan.copy(alpha = 0.15f * glow * circuitPulse),
                radius = n.size * 3.5f,
                center = Offset(nx, ny),
            )
            drawCircle(
                color = Brand.CyanSoft.copy(alpha = 0.55f * glow),
                radius = n.size,
                center = Offset(nx, ny),
            )
            if (i + 1 < nodes.size) {
                val next = nodes[i + 1]
                val nx2 = next.xFrac * w + nearX * 0.4f
                val ny2 = next.yFrac * h + nearY * 0.4f
                val dist = Offset(nx2 - nx, ny2 - ny).getDistance()
                if (dist < w * 0.35f) {
                    drawLine(
                        color = Brand.Cyan.copy(alpha = 0.08f * circuitPulse),
                        start = Offset(nx, ny),
                        end = Offset(nx2, ny2),
                        strokeWidth = 1.2f,
                    )
                }
            }
        }

        // Rising bubbles
        bubbles.forEach { b ->
            val bx = b.x * w + nearX * 0.25f
            val by = b.y * h + nearY * 0.15f
            drawCircle(
                color = Brand.CyanSoft.copy(alpha = b.alpha * 0.35f),
                radius = b.radius * 1.6f,
                center = Offset(bx, by),
            )
            drawCircle(
                color = Brand.Foam.copy(alpha = b.alpha * 0.55f),
                radius = b.radius,
                center = Offset(bx, by),
                style = Stroke(width = 1.4f),
            )
            drawCircle(
                color = Brand.Foam.copy(alpha = b.alpha * 0.35f),
                radius = b.radius * 0.35f,
                center = Offset(bx - b.radius * 0.3f, by - b.radius * 0.3f),
            )
        }

        // Tap ripples
        ripples.forEach { r ->
            val t = (r.ageMs / r.lifeMs).coerceIn(0f, 1f)
            val radius = r.maxRadius * t
            val alpha = (1f - t) * 0.55f
            drawCircle(
                color = Brand.Cyan.copy(alpha = alpha),
                radius = radius,
                center = r.origin,
                style = Stroke(width = 3f * (1f - t * 0.7f)),
            )
            drawCircle(
                color = Brand.CyanSoft.copy(alpha = alpha * 0.45f),
                radius = radius * 0.72f,
                center = r.origin,
                style = Stroke(width = 1.5f),
            )
        }

        // Vignette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Brand.NavyDeep.copy(alpha = 0.55f),
                ),
                center = Offset(w * 0.5f, h * 0.4f),
                radius = w.coerceAtLeast(h) * 0.85f,
            ),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHex(
    center: Offset,
    radius: Float,
    color: Color,
    stroke: Float,
) {
    val path = Path()
    for (i in 0 until 6) {
        val angle = Math.toRadians((60.0 * i) - 30.0)
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color, style = Stroke(width = stroke))
}
