package com.evil0ctopus.octobuddy.data

/**
 * Brand-pet cosmetics — overlays on BrandPetView (not a new mesh).
 * Unlock via achievements / level / streak; equip one per slot.
 */
enum class CosmeticSlot {
    Frame,
    Effect,
    Head,
    Accent,
}

enum class Cosmetic(
    val id: String,
    val slot: CosmeticSlot,
    val title: String,
    val description: String,
    /** Achievement bit that unlocks, or -1 if level/streak gated. */
    val achievementBit: Int = -1,
    val minLevel: Int = 1,
    val minStreak: Int = 0,
) {
    // Frames
    FrameCyan(
        "frame_cyan", CosmeticSlot.Frame, "Cyan Ring",
        "Default brand hex ring", minLevel = 1,
    ),
    FrameCopper(
        "frame_copper", CosmeticSlot.Frame, "Copper Circlet",
        "Warm copper frame", achievementBit = Achievement.Feed10.bit,
    ),
    FrameAbyss(
        "frame_abyss", CosmeticSlot.Frame, "Abyss Halo",
        "Deep dual-tone halo", achievementBit = Achievement.ReachAdult.bit,
    ),
    FrameGold(
        "frame_gold", CosmeticSlot.Frame, "Dread Frame",
        "Max-rank gilt frame", achievementBit = Achievement.MaxLevel.bit,
    ),

    // Effects (glitter / glow)
    EffectNone(
        "effect_none", CosmeticSlot.Effect, "Clean Ink",
        "No particle sparkle", minLevel = 1,
    ),
    EffectGlitter(
        "effect_glitter", CosmeticSlot.Effect, "Packet Glitter",
        "Soft cyan sparkles", achievementBit = Achievement.Tap25.bit,
    ),
    EffectTentacleGlow(
        "effect_glow", CosmeticSlot.Effect, "Tentacle Glow",
        "Pulsing cyan aura", achievementBit = Achievement.Play10.bit,
    ),
    EffectStreakFire(
        "effect_streak", CosmeticSlot.Effect, "Streak Ember",
        "Copper ember for streak keepers", minStreak = 3,
    ),

    // Head (mini hats / crowns)
    HeadNone(
        "head_none", CosmeticSlot.Head, "Bare Dome",
        "No headgear", minLevel = 1,
    ),
    HeadMiniHat(
        "head_hat", CosmeticSlot.Head, "Pirate Cap",
        "Tiny copper pirate hat", achievementBit = Achievement.NamedBuddy.bit,
    ),
    HeadCrown(
        "head_crown", CosmeticSlot.Head, "Hex Crown",
        "Adult copper crown (always on Adult stage too)", achievementBit = Achievement.ReachJuvenile.bit,
    ),
    HeadAdmiral(
        "head_admiral", CosmeticSlot.Head, "Admiral Crest",
        "Level-15 crest", minLevel = 15,
    ),

    // Background accents (drawn near pet / ocean tint hint)
    AccentNone(
        "accent_none", CosmeticSlot.Accent, "Clear Water",
        "No extra accent", minLevel = 1,
    ),
    AccentBubbles(
        "accent_bubbles", CosmeticSlot.Accent, "Bubble Veil",
        "Extra bubble ring around buddy", achievementBit = Achievement.FirstPlay.bit,
    ),
    AccentHexSpark(
        "accent_hex", CosmeticSlot.Accent, "Hex Spark",
        "Orbiting hex sparks", achievementBit = Achievement.Level10.bit,
    ),
    AccentMoon(
        "accent_moon", CosmeticSlot.Accent, "Night Buoy",
        "Soft moon accent (pairs with night ocean)", achievementBit = Achievement.Streak7.bit,
    ),
    ;

    fun isUnlocked(
        achievementsMask: Long,
        level: Int,
        careStreak: Int,
    ): Boolean {
        if (level < minLevel) return false
        if (careStreak < minStreak) return false
        if (achievementBit >= 0) {
            val bitMask = 1L shl achievementBit
            if (achievementsMask and bitMask == 0L) return false
        }
        return true
    }

    companion object {
        fun defaults(): EquippedCosmetics = EquippedCosmetics(
            frameId = FrameCyan.id,
            effectId = EffectNone.id,
            headId = HeadNone.id,
            accentId = AccentNone.id,
        )

        fun byId(id: String): Cosmetic? = entries.find { it.id == id }

        fun forSlot(slot: CosmeticSlot): List<Cosmetic> = entries.filter { it.slot == slot }
    }
}

data class EquippedCosmetics(
    val frameId: String = Cosmetic.FrameCyan.id,
    val effectId: String = Cosmetic.EffectNone.id,
    val headId: String = Cosmetic.HeadNone.id,
    val accentId: String = Cosmetic.AccentNone.id,
) {
    fun frame(): Cosmetic = Cosmetic.byId(frameId) ?: Cosmetic.FrameCyan
    fun effect(): Cosmetic = Cosmetic.byId(effectId) ?: Cosmetic.EffectNone
    fun head(): Cosmetic = Cosmetic.byId(headId) ?: Cosmetic.HeadNone
    fun accent(): Cosmetic = Cosmetic.byId(accentId) ?: Cosmetic.AccentNone

    fun withSlot(slot: CosmeticSlot, cosmetic: Cosmetic): EquippedCosmetics {
        require(cosmetic.slot == slot)
        return when (slot) {
            CosmeticSlot.Frame -> copy(frameId = cosmetic.id)
            CosmeticSlot.Effect -> copy(effectId = cosmetic.id)
            CosmeticSlot.Head -> copy(headId = cosmetic.id)
            CosmeticSlot.Accent -> copy(accentId = cosmetic.id)
        }
    }
}
