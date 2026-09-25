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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.evil0ctopus.octobuddy.R
import com.evil0ctopus.octobuddy.data.PetStage
import com.evil0ctopus.octobuddy.ui.theme.Brand

/**
 * Brand-faithful 2.5D pet using the real Evil0ctopus mark PNG.
 * Idle bob / breathe / tilt, eye gleam pulse, stage scale + adult copper sheen,
 * and punch / spin bursts on care actions.
 */
@Composable
fun BrandPetView(
    stage: PetStage,
    actionEpoch: Int,
    lastAction: PetAction,
    evolveEpoch: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "brand_pet")
    val bobMs = (2200 / stage.idleSpeed).toInt().coerceIn(1400, 3200)

    val bob by transition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = bobMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )
    val breathe by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = bobMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    val tilt by transition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = bobMs + 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tilt",
    )
    val gleam by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gleam",
    )
    val punch = remember { Animatable(1f) }
    val spin = remember { Animatable(0f) }
    val evolveFlash = remember { Animatable(0f) }

    LaunchedEffect(actionEpoch) {
        if (actionEpoch == 0) return@LaunchedEffect
        val peak = when (lastAction) {
            PetAction.Play -> 1.18f
            PetAction.Feed -> 1.14f
            PetAction.Rest -> 1.10f
            PetAction.Tap -> 1.08f
        }
        val degrees = when (lastAction) {
            PetAction.Play -> 360f
            PetAction.Feed -> 160f
            PetAction.Rest -> 70f
            PetAction.Tap -> 28f
        }
        punch.snapTo(1f)
        spin.snapTo(0f)
        punch.animateTo(peak, animationSpec = tween(110, easing = FastOutSlowInEasing))
        punch.animateTo(1f, animationSpec = tween(240, easing = FastOutSlowInEasing))
        spin.animateTo(degrees, animationSpec = tween(650, easing = FastOutSlowInEasing))
        spin.snapTo(0f)
    }

    LaunchedEffect(evolveEpoch) {
        if (evolveEpoch == 0) return@LaunchedEffect
        evolveFlash.snapTo(0f)
        evolveFlash.animateTo(1f, animationSpec = tween(180))
        evolveFlash.animateTo(0f, animationSpec = tween(700))
    }

    val stageScale = stage.modelScale
    val colorFilter = when (stage) {
        PetStage.Hatchling -> null
        PetStage.Juvenile -> null
        PetStage.Adult -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1.05f, 0f, 0f, 0f, 8f,
                    0f, 1.02f, 0f, 0f, 4f,
                    0f, 0f, 0.95f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }

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
        // Soft cyan aura behind the mark
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .graphicsLayer {
                    scaleX = breathe * punch.value
                    scaleY = breathe * punch.value
                    alpha = 0.35f + gleam * 0.25f + evolveFlash.value * 0.45f
                }
                .drawWithContent {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Brand.Cyan.copy(alpha = 0.35f),
                                Brand.Cyan.copy(alpha = 0.08f),
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
                    translationY = bob
                    rotationZ = tilt + spin.value
                    scaleX = stageScale * breathe * punch.value
                    scaleY = stageScale * breathe * punch.value
                    alpha = 0.92f + gleam * 0.08f
                }
                .drawWithContent {
                    drawContent()
                    // Eye gleam / blink overlay — soft cyan wash over upper third
                    val gleamAlpha = gleam * 0.22f + evolveFlash.value * 0.35f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Brand.CyanSoft.copy(alpha = gleamAlpha),
                                Color.Transparent,
                            ),
                            startY = size.height * 0.18f,
                            endY = size.height * 0.48f,
                        ),
                    )
                    if (evolveFlash.value > 0.01f) {
                        drawCircle(
                            color = Brand.Cyan.copy(alpha = evolveFlash.value * 0.45f),
                            radius = size.minDimension * 0.55f * (0.6f + evolveFlash.value * 0.4f),
                            center = Offset(size.width / 2f, size.height / 2f),
                        )
                    }
                },
        )

        // Adult copper crown accent (small arc of dots above head)
        if (stage == PetStage.Adult) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 72.dp, height = 18.dp)
                    .graphicsLayer {
                        translationY = bob * 0.4f - 6f
                        alpha = 0.55f + gleam * 0.35f
                    }
                    .drawWithContent {
                        val cy = size.height * 0.6f
                        val xs = listOf(0.2f, 0.35f, 0.5f, 0.65f, 0.8f)
                        xs.forEachIndexed { i, xf ->
                            val x = size.width * xf
                            val y = cy - kotlin.math.abs(i - 2) * 2.5f
                            drawCircle(
                                color = Brand.CopperBright.copy(alpha = 0.85f),
                                radius = 3.2f,
                                center = Offset(x, y),
                            )
                            drawCircle(
                                color = Brand.CyanSoft.copy(alpha = 0.5f),
                                radius = 1.4f,
                                center = Offset(x - 0.6f, y - 0.6f),
                            )
                        }
                    },
            )
        }
    }
}
