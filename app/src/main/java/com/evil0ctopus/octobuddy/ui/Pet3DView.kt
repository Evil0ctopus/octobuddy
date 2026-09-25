package com.evil0ctopus.octobuddy.ui

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.evil0ctopus.octobuddy.R
import com.evil0ctopus.octobuddy.data.PetStage
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import kotlin.math.sin

private const val MODEL_ASSET = "models/octobuddy.glb"

/**
 * SceneView (Filament) pet viewer.
 * Loads the bundled stylized octopus GLB, scales by [PetStage],
 * idles with a gentle Y bob + spin, and punches on care actions.
 * Falls back to the brand PNG if the model fails to load.
 */
@Composable
fun Pet3DView(
    stage: PetStage,
    actionEpoch: Int,
    lastAction: PetAction,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var useFallback by remember { mutableStateOf(false) }

    if (useFallback) {
        FallbackIdlePet(onTap = onTap, modifier = modifier)
        return
    }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val centerNode = rememberNode(engine)

    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0.0f, y = 0.15f, z = 2.4f)
        lookAt(centerNode)
    }

    val modelNode: ModelNode? = remember(engine) {
        try {
            ModelNode(
                modelInstance = modelLoader.createModelInstance(MODEL_ASSET),
                scaleToUnits = 1.0f,
                centerOrigin = Position(x = 0.0f, y = 0.0f, z = 0.0f),
            ).also { centerNode.addChildNode(it) }
        } catch (_: Throwable) {
            null
        }
    }

    LaunchedEffect(modelNode) {
        if (modelNode == null) useFallback = true
    }

    val punch = remember { Animatable(1f) }
    LaunchedEffect(actionEpoch) {
        if (actionEpoch == 0) return@LaunchedEffect
        val peak = when (lastAction) {
            PetAction.Play -> 1.22f
            PetAction.Feed -> 1.16f
            PetAction.Rest -> 1.12f
            PetAction.Tap -> 1.10f
        }
        punch.snapTo(1f)
        punch.animateTo(peak, animationSpec = tween(120))
        punch.animateTo(1f, animationSpec = tween(220))
    }

    val spinBurst = remember { Animatable(0f) }
    LaunchedEffect(actionEpoch) {
        if (actionEpoch == 0) return@LaunchedEffect
        val degrees = when (lastAction) {
            PetAction.Play -> 360f
            PetAction.Feed -> 180f
            PetAction.Rest -> 90f
            PetAction.Tap -> 45f
        }
        spinBurst.snapTo(0f)
        spinBurst.animateTo(degrees, animationSpec = tween(durationMillis = 700))
    }

    LaunchedEffect(stage, modelNode, punch.value) {
        val node = modelNode ?: return@LaunchedEffect
        val s = stage.modelScale * punch.value
        node.scale = Scale(s)
    }

    if (modelNode == null) {
        FallbackIdlePet(onTap = onTap, modifier = modifier)
        return
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
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            isOpaque = false,
            cameraNode = cameraNode,
            cameraManipulator = null,
            childNodes = listOf(centerNode),
            onFrame = {
                val t = System.nanoTime() / 1_000_000_000.0
                val bob = (sin(t * stage.idleSpeed * 2.2) * 0.06).toFloat()
                val spin = ((t * 18.0 * stage.idleSpeed) % 360.0).toFloat() + spinBurst.value
                centerNode.position = Position(x = 0.0f, y = bob, z = 0.0f)
                centerNode.rotation = Rotation(x = 0.0f, y = spin, z = 0.0f)
                cameraNode.lookAt(centerNode)
            },
        )
    }
}

@Composable
private fun FallbackIdlePet(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "octo_idle_fallback")
    val bob by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Image(
        painter = painterResource(R.drawable.octobuddy_pet),
        contentDescription = "OctoBuddy pet",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .graphicsLayer { translationY = bob }
            .scale(pulse)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
    )
}
