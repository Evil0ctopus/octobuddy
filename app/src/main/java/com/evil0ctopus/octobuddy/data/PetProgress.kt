package com.evil0ctopus.octobuddy.data

/**
 * Level / evolution helpers for OctoBuddy.
 *
 * Curve (documented in README):
 *   level = 1 + floor(xp / XP_PER_LEVEL), capped at [MAX_LEVEL]
 *   Hatchling = levels 1–4, Juvenile = 5–9, Adult = 10+
 */
enum class PetStage(val displayName: String, val modelScale: Float, val idleSpeed: Float) {
    Hatchling("Hatchling", modelScale = 0.55f, idleSpeed = 0.75f),
    Juvenile("Juvenile", modelScale = 0.80f, idleSpeed = 1.00f),
    Adult("Adult", modelScale = 1.00f, idleSpeed = 1.25f),
}

object PetProgress {
    const val XP_PER_LEVEL = 40L
    const val MAX_LEVEL = 30

    const val XP_TAP = 1L
    const val XP_FEED = 5L
    const val XP_PLAY = 8L
    const val XP_REST = 4L

    fun levelForXp(xp: Long): Int {
        val safeXp = xp.coerceAtLeast(0L)
        val raw = 1 + (safeXp / XP_PER_LEVEL).toInt()
        return raw.coerceIn(1, MAX_LEVEL)
    }

    fun stageForLevel(level: Int): PetStage = when {
        level < 5 -> PetStage.Hatchling
        level < 10 -> PetStage.Juvenile
        else -> PetStage.Adult
    }

    fun stageForXp(xp: Long): PetStage = stageForLevel(levelForXp(xp))

    /** XP already earned within the current level band (0 until [XP_PER_LEVEL]). */
    fun xpIntoLevel(xp: Long): Long {
        val level = levelForXp(xp)
        if (level >= MAX_LEVEL) return XP_PER_LEVEL
        val floorXp = (level - 1) * XP_PER_LEVEL
        return (xp.coerceAtLeast(0L) - floorXp).coerceIn(0L, XP_PER_LEVEL)
    }

    /** Progress 0f–1f toward the next level (1f when maxed). */
    fun xpProgress(xp: Long): Float {
        if (levelForXp(xp) >= MAX_LEVEL) return 1f
        return xpIntoLevel(xp).toFloat() / XP_PER_LEVEL.toFloat()
    }

    /** XP still needed to reach the next level (0 when maxed). */
    fun xpToNext(xp: Long): Long {
        if (levelForXp(xp) >= MAX_LEVEL) return 0L
        return XP_PER_LEVEL - xpIntoLevel(xp)
    }
}
