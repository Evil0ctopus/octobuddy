package com.evil0ctopus.octobuddy.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.evil0ctopus.octobuddy.R
import com.evil0ctopus.octobuddy.data.Cosmetic
import com.evil0ctopus.octobuddy.data.EquippedCosmetics
import com.evil0ctopus.octobuddy.data.PetStage
import com.evil0ctopus.octobuddy.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Brand-faithful 2.5D pet using the Evil0ctopus mark PNG.
 * Distinct Feed / Play / Rest reactions, blink cycles, mood posture,
 * and equippable frame / glitter / glow / hats / accents.
 */
@Composable
fun BrandPetView(
    stage: PetStage,
    actionEpoch: Int,
    lastAction: PetAction,
    evolveEpoch: Int,
    hunger: Float,
    mood: Float,
    energy: Float,
    equipped: EquippedCosmetics,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "brand_pet")
    val needsAvg = (hunger + mood + energy) / 3f
    val sadFactor = ((50f - needsAvg) / 50f).coerceIn(0f, 1f)
    val happyFactor = ((needsAvg - 60f) / 40f).coerceIn(0f, 1f)
    val bobAmp = 10f - sadFactor * 5f + happyFactor * 3f
    val bobMs = ((2200 / stage.idleSpeed) * (1f + sadFactor * 0.45f)).toInt().coerceIn(1400, 3800)

    val bob by transition.animateFloat(
        initialValue = -bobAmp,
        targetValue = bobAmp,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = bobMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )
    val breathe by transition.animateFloat(
        initialValue = 0.97f - sadFactor * 0.02f,
        targetValue = 1.03f + happyFactor * 0.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = bobMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    val tilt by transition.animateFloat(
        initialValue = -3.5f + sadFactor * 2f,
        targetValue = 3.5f - sadFactor * 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = bobMs + 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tilt",
    )
    val gleam by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.55f + happyFactor * 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gleam",
    )
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbit",
    )
    val glitterPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glitter",
    )

    val punch = remember { Animatable(1f) }
    val spin = remember { Animatable(0f) }
    val squashY = remember { Animatable(1f) }
    val offsetY = remember { Animatable(0f) }
    val evolveFlash = remember { Animatable(0f) }
    val blink = remember { Animatable(0f) }

    // Blink cycle — quick lid close every few seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800L + (Math.random() * 3200).toLong())
            blink.snapTo(0f)
            blink.animateTo(1f, animationSpec = tween(70))
            blink.animateTo(0f, animationSpec = tween(110))
            if (Math.random() < 0.28) {
                delay(120)
                blink.animateTo(1f, animationSpec = tween(60))
                blink.animateTo(0f, animationSpec = tween(100))
            }
        }
    }

    // Distinct care reactions
    LaunchedEffect(actionEpoch) {
        if (actionEpoch == 0) return@LaunchedEffect
        punch.snapTo(1f)
        spin.snapTo(0f)
        squashY.snapTo(1f)
        offsetY.snapTo(0f)
        when (lastAction) {
            PetAction.Feed -> {
                // Happy nom: squash + bounce up
                squashY.animateTo(0.88f, tween(90, easing = FastOutSlowInEasing))
                punch.animateTo(1.16f, tween(100, easing = FastOutSlowInEasing))
                offsetY.animateTo(-14f, tween(120, easing = FastOutSlowInEasing))
                squashY.animateTo(1.06f, tween(120))
                punch.animateTo(1f, tween(200))
                offsetY.animateTo(0f, tween(180))
                squashY.animateTo(1f, tween(140))
                spin.animateTo(12f, tween(100))
                spin.animateTo(-8f, tween(120))
                spin.animateTo(0f, tween(100))
            }
            PetAction.Play -> {
                // Full spin jig
                punch.animateTo(1.20f, tween(100, easing = FastOutSlowInEasing))
                spin.animateTo(360f, tween(620, easing = FastOutSlowInEasing))
                punch.animateTo(1f, tween(220))
                spin.snapTo(0f)
                offsetY.animateTo(-18f, tween(80))
                offsetY.animateTo(0f, tween(160))
            }
            PetAction.Rest -> {
                // Slow settle / sleepy droop
                offsetY.animateTo(16f, tween(280, easing = FastOutSlowInEasing))
                squashY.animateTo(0.92f, tween(280))
                punch.animateTo(0.96f, tween(280))
                delay(220)
                offsetY.animateTo(0f, tween(360))
                squashY.animateTo(1f, tween(360))
                punch.animateTo(1f, tween(360))
            }
            PetAction.Tap -> {
                punch.animateTo(1.10f, tween(90, easing = FastOutSlowInEasing))
                spin.animateTo(22f, tween(140))
                punch.animateTo(1f, tween(180))
                spin.animateTo(0f, tween(160))
            }
        }
    }

    LaunchedEffect(evolveEpoch) {
        if (evolveEpoch == 0) return@LaunchedEffect
        evolveFlash.snapTo(0f)
        evolveFlash.animateTo(1f, animationSpec = tween(180))
        evolveFlash.animateTo(0f, animationSpec = tween(700))
    }

    val stageScale = stage.modelScale
    val postureDroop = sadFactor * 10f
    val colorFilter = when {
        sadFactor > 0.45f -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.85f, 0f, 0f, 0f, 0f,
                    0f, 0.88f, 0f, 0f, 0f,
                    0f, 0f, 1.05f, 0f, 6f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
        stage == PetStage.Adult -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1.05f, 0f, 0f, 0f, 8f,
                    0f, 1.02f, 0f, 0f, 4f,
                    0f, 0f, 0.95f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
        else -> null
    }

    val frame = equipped.frame()
    val effect = equipped.effect()
    val head = equipped.head()
    val accent = equipped.accent()
    val showCrownDots = stage == PetStage.Adult ||
        head == Cosmetic.HeadCrown ||
        head == Cosmetic.HeadAdmiral

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Frame ring
        Box(
            modifier = Modifier
                .fillMaxSize(0.98f)
                .graphicsLayer {
                    scaleX = breathe * punch.value
                    scaleY = breathe * punch.value * squashY.value
                    alpha = 0.9f
                }
                .drawWithContent {
                    val strokeColor = when (frame) {
                        Cosmetic.FrameCopper -> Brand.CopperBright
                        Cosmetic.FrameAbyss -> Brand.CyanSoft
                        Cosmetic.FrameGold -> Color(0xFFFFD700)
                        else -> Brand.Cyan
                    }
                    val r = min(size.width, size.height) * 0.48f
                    drawCircle(
                        color = strokeColor.copy(alpha = 0.35f + gleam * 0.25f),
                        radius = r,
                        style = Stroke(width = 3.5f),
                    )
                    if (frame == Cosmetic.FrameAbyss || frame == Cosmetic.FrameGold) {
                        drawCircle(
                            color = Brand.Copper.copy(alpha = 0.25f),
                            radius = r * 0.92f,
                            style = Stroke(width = 1.5f),
                        )
                    }
                },
        )

        // Soft aura (stronger with tentacle glow)
        val glowBoost = if (effect == Cosmetic.EffectTentacleGlow || effect == Cosmetic.EffectStreakFire) 0.35f else 0f
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .graphicsLayer {
                    scaleX = breathe * punch.value
                    scaleY = breathe * punch.value * squashY.value
                    alpha = 0.35f + gleam * 0.25f + evolveFlash.value * 0.45f + glowBoost
                }
                .drawWithContent {
                    val c1 = when (effect) {
                        Cosmetic.EffectStreakFire -> Brand.CopperBright
                        else -> Brand.Cyan
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                c1.copy(alpha = 0.35f + glowBoost),
                                c1.copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                        ),
                    )
                },
        )

        Image(
            painter = painterResource(R.drawable.octobuddy_pet),
            contentDescription = stringResource(R.string.content_desc_pet),
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter,
            modifier = Modifier
                .fillMaxSize(0.92f)
                .graphicsLayer {
                    translationY = bob + offsetY.value + postureDroop
                    rotationZ = tilt + spin.value
                    scaleX = stageScale * breathe * punch.value
                    scaleY = stageScale * breathe * punch.value * squashY.value
                    alpha = 0.92f + gleam * 0.08f
                }
                .drawWithContent {
                    drawContent()
                    val gleamAlpha = gleam * 0.22f + evolveFlash.value * 0.35f
                    // Eye gleam
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Brand.CyanSoft.copy(alpha = gleamAlpha * (1f - blink.value)),
                                Color.Transparent,
                            ),
                            startY = size.height * 0.18f,
                            endY = size.height * 0.48f,
                        ),
                    )
                    // Blink lids
                    if (blink.value > 0.01f) {
                        val lidH = size.height * 0.14f * blink.value
                        drawRect(
                            color = Brand.NavyDeep.copy(alpha = 0.55f * blink.value),
                            topLeft = Offset(size.width * 0.22f, size.height * 0.28f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.56f, lidH),
                        )
                    }
                    if (evolveFlash.value > 0.01f) {
                        drawCircle(
                            color = Brand.Cyan.copy(alpha = evolveFlash.value * 0.45f),
                            radius = min(size.width, size.height) * 0.55f * (0.6f + evolveFlash.value * 0.4f),
                            center = Offset(size.width / 2f, size.height / 2f),
                        )
                    }
                    // Packet glitter
                    if (effect == Cosmetic.EffectGlitter || effect == Cosmetic.EffectStreakFire) {
                        val n = 10
                        for (i in 0 until n) {
                            val a = glitterPhase + i * (PI * 2 / n).toFloat()
                            val rr = min(size.width, size.height) * (0.28f + (i % 3) * 0.06f)
                            val cx = size.width / 2f + cos(a) * rr
                            val cy = size.height / 2f + sin(a * 1.3f) * rr * 0.85f
                            val spark = (sin(a * 3f) + 1f) * 0.5f
                            val col = if (effect == Cosmetic.EffectStreakFire) Brand.CopperBright else Brand.CyanSoft
                            drawCircle(
                                color = col.copy(alpha = 0.25f + spark * 0.55f),
                                radius = 1.6f + spark * 2.2f,
                                center = Offset(cx, cy),
                            )
                        }
                    }
                },
        )

        // Head cosmetics
        when (head) {
            Cosmetic.HeadMiniHat -> MiniPirateHat(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-4).dp)
                    .graphicsLayer {
                        translationY = bob * 0.4f + offsetY.value * 0.4f
                        rotationZ = tilt * 0.5f + spin.value * 0.15f
                    },
            )
            Cosmetic.HeadAdmiral -> AdmiralCrest(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-2).dp)
                    .graphicsLayer {
                        translationY = bob * 0.4f + offsetY.value * 0.4f
                        alpha = 0.85f + gleam * 0.15f
                    },
            )
            else -> {}
        }

        if (showCrownDots && head != Cosmetic.HeadMiniHat) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 72.dp, height = 18.dp)
                    .graphicsLayer {
                        translationY = bob * 0.4f - 6f + offsetY.value * 0.3f
                        alpha = 0.55f + gleam * 0.35f
                    }
                    .drawWithContent {
                        val cy = size.height * 0.6f
                        val xs = listOf(0.2f, 0.35f, 0.5f, 0.65f, 0.8f)
                        val col = if (head == Cosmetic.HeadAdmiral) Color(0xFFFFD700) else Brand.CopperBright
                        xs.forEachIndexed { i, xf ->
                            val x = size.width * xf
                            val y = cy - kotlin.math.abs(i - 2) * 2.5f
                            drawCircle(color = col.copy(alpha = 0.85f), radius = 3.2f, center = Offset(x, y))
                            drawCircle(
                                color = Brand.CyanSoft.copy(alpha = 0.5f),
                                radius = 1.4f,
                                center = Offset(x - 0.6f, y - 0.6f),
                            )
                        }
                    },
            )
        }

        // Accent overlays
        when (accent) {
            Cosmetic.AccentBubbles -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.7f }
                    .drawWithContent {
                        for (i in 0 until 6) {
                            val a = orbit * (PI / 180f).toFloat() + i * (PI / 3f).toFloat()
                            val rr = min(size.width, size.height) * 0.42f
                            val cx = size.width / 2f + cos(a) * rr
                            val cy = size.height / 2f + sin(a) * rr
                            drawCircle(
                                color = Brand.Foam.copy(alpha = 0.35f),
                                radius = 3f + (i % 3),
                                center = Offset(cx, cy),
                                style = Stroke(width = 1.2f),
                            )
                        }
                    },
            )
            Cosmetic.AccentHexSpark -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        for (i in 0 until 5) {
                            val a = -orbit * (PI / 180f).toFloat() + i * (PI * 2 / 5f).toFloat()
                            val rr = min(size.width, size.height) * 0.44f
                            val cx = size.width / 2f + cos(a) * rr
                            val cy = size.height / 2f + sin(a) * rr
                            drawHexSpark(Offset(cx, cy), 6f, Brand.Cyan.copy(alpha = 0.55f))
                        }
                    },
            )
            Cosmetic.AccentMoon -> Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
                    .offset(x = (-8).dp, y = 12.dp)
                    .graphicsLayer { alpha = 0.75f + gleam * 0.2f }
                    .drawWithContent {
                        drawCircle(color = Brand.Foam.copy(alpha = 0.55f), radius = min(size.width, size.height) * 0.35f)
                        drawCircle(
                            color = Brand.NavyDeep.copy(alpha = 0.55f),
                            radius = min(size.width, size.height) * 0.28f,
                            center = Offset(size.width * 0.62f, size.height * 0.38f),
                        )
                    },
            )
            else -> {}
        }
    }
}

@Composable
private fun MiniPirateHat(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 54.dp, height = 28.dp)
            .drawWithContent {
                val path = Path().apply {
                    moveTo(size.width * 0.1f, size.height * 0.65f)
                    lineTo(size.width * 0.5f, size.height * 0.1f)
                    lineTo(size.width * 0.9f, size.height * 0.65f)
                    close()
                }
                drawPath(path, Brand.Copper)
                drawPath(path, Brand.CopperBright, style = Stroke(width = 1.5f))
                drawRect(
                    color = Brand.NavyDeep,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.55f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.28f),
                )
                // Tiny cyan band
                drawRect(
                    color = Brand.Cyan.copy(alpha = 0.8f),
                    topLeft = Offset(size.width * 0.22f, size.height * 0.58f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.1f),
                )
            },
    )
}

@Composable
private fun AdmiralCrest(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 22.dp)
            .drawWithContent {
                val gold = Color(0xFFFFD700)
                drawCircle(gold, radius = 4f, center = Offset(size.width * 0.5f, size.height * 0.35f))
                drawCircle(Brand.CyanSoft, radius = 2f, center = Offset(size.width * 0.5f, size.height * 0.35f))
                drawLine(
                    gold,
                    Offset(size.width * 0.2f, size.height * 0.7f),
                    Offset(size.width * 0.8f, size.height * 0.7f),
                    strokeWidth = 2.5f,
                )
            },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHexSpark(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val path = Path()
    for (i in 0 until 6) {
        val angle = Math.toRadians((60.0 * i) - 30.0)
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color, style = Stroke(width = 1.4f))
}
