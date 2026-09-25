package com.evil0ctopus.octobuddy.data

/**
 * Level / evolution / rank helpers (PorkChop-style progression shell).
 *
 * Curve:
 *   level = 1 + floor(xp / XP_PER_LEVEL), capped at [MAX_LEVEL]
 *   Hatchling = 1–4 · Juvenile = 5–9 · Adult = 10+
 */
enum class PetStage(val displayName: String, val modelScale: Float, val idleSpeed: Float) {
    Hatchling("Hatchling", modelScale = 0.72f, idleSpeed = 0.80f),
    Juvenile("Juvenile", modelScale = 0.88f, idleSpeed = 1.00f),
    Adult("Adult", modelScale = 1.00f, idleSpeed = 1.18f),
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

    /** PorkChop-style rank title by level (cyber-octopus voice). */
    fun rankTitle(level: Int): String = when {
        level >= 30 -> "Dread Octopus"
        level >= 25 -> "Abyss Ace"
        level >= 20 -> "Circuit Kraken"
        level >= 15 -> "Hex Admiral"
        level >= 10 -> "Cyan Captain"
        level >= 7 -> "Tide Raider"
        level >= 5 -> "Ring Runner"
        level >= 3 -> "Ink Scout"
        else -> "Inkling"
    }

    fun xpIntoLevel(xp: Long): Long {
        val level = levelForXp(xp)
        if (level >= MAX_LEVEL) return XP_PER_LEVEL
        val floorXp = (level - 1) * XP_PER_LEVEL
        return (xp.coerceAtLeast(0L) - floorXp).coerceIn(0L, XP_PER_LEVEL)
    }

    fun xpProgress(xp: Long): Float {
        if (levelForXp(xp) >= MAX_LEVEL) return 1f
        return xpIntoLevel(xp).toFloat() / XP_PER_LEVEL.toFloat()
    }

    fun xpToNext(xp: Long): Long {
        if (levelForXp(xp) >= MAX_LEVEL) return 0L
        return XP_PER_LEVEL - xpIntoLevel(xp)
    }
}
