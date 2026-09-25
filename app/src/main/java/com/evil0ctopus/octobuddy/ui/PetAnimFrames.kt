package com.evil0ctopus.octobuddy.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * Blender-rendered brand-pet frame loops under assets/pet_anim/{clip}/frame_XX.png.
 * Same Evil0ctopus PNG pixels warped in Blender — not a different character mesh.
 */
enum class PetAnimClip(val folder: String) {
    Idle("idle"),
    Feed("feed"),
    Play("play"),
    Rest("rest"),
    Tap("tap"),
}

fun PetAction.toAnimClip(): PetAnimClip = when (this) {
    PetAction.Feed -> PetAnimClip.Feed
    PetAction.Play -> PetAnimClip.Play
    PetAction.Rest -> PetAnimClip.Rest
    PetAction.Tap -> PetAnimClip.Tap
}

object PetAnimFrames {
    private const val ASSET_ROOT = "pet_anim"
    private const val BASE_FPS = 24f

    fun frameDurationMs(mood: Float, energy: Float, stageIdleSpeed: Float): Long {
        val needs = ((mood + energy) / 2f).coerceIn(0f, 100f)
        val moodScale = when {
            needs < 35f -> 1.35f
            needs > 75f -> 0.85f
            else -> 1f
        }
        val speed = stageIdleSpeed.coerceIn(0.5f, 1.5f) / moodScale
        return (1000f / (BASE_FPS * speed)).toLong().coerceIn(28L, 90L)
    }

    fun loadClip(context: Context, clip: PetAnimClip): List<ImageBitmap> {
        val am = context.assets
        val dir = "$ASSET_ROOT/${clip.folder}"
        val names = try {
            am.list(dir)
                ?.filter { it.startsWith("frame_") && it.endsWith(".png") }
                ?.sorted()
                .orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        if (names.isEmpty()) return emptyList()
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        return names.mapNotNull { name ->
            try {
                am.open("$dir/$name").use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}

@Composable
fun rememberPetAnimClips(): Map<PetAnimClip, List<ImageBitmap>> {
    val context = LocalContext.current
    return remember(context) {
        PetAnimClip.entries.associateWith { PetAnimFrames.loadClip(context, it) }
    }
}
