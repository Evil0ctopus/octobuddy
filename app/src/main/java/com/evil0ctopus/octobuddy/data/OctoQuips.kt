package com.evil0ctopus.octobuddy.data

/**
 * PorkChop-style speech-bubble quips — Evil0ctopus cyber-pirate voice.
 * Mood-weighted ambient picks prioritize low need lines.
 */
object OctoQuips {
    private val idleHappy = listOf(
        "Beep-arrr!",
        "Hex marks the spot",
        "Ink over IP",
        "Code. Craft. Conquer.",
        "Cyan rings online",
        "Compile me lucky",
        "Kraken.exe ready",
        "Glowing and knowing",
        "Copper belly, steel will",
        "Not all treasure be gold",
        "Steady as she code",
        "Tentacles > toolbars",
    )

    private val idleContent = listOf(
        "Packets taste like krill",
        "Deep packets call…",
        "Who needs Wi‑Fi?",
        "The abyss pings back",
        "Soft reboot, hard stare",
        "Mind the boom… buffer",
        "Idle loop humming",
        "Surface looks calm",
    )

    private val tap = listOf(
        "Hey!",
        "Boop the beak",
        "I see you",
        "Ink splash!",
        "Tickle protocols engaged",
        "Ow — affection packet",
    )

    private val feed = listOf(
        "Nom nom packets",
        "Delicious datagrams",
        "Fuel for the deep",
        "More krill, less lag",
        "Stomach buffer full-ish",
        "Yum — checksum OK",
    )

    private val play = listOf(
        "Yo ho ho!",
        "Jig on the hex grid",
        "Race you to root!",
        "Play mode: mischief",
        "Cannonball… of ink!",
        "Spin the cyan!",
    )

    private val rest = listOf(
        "Power nap.exe",
        "Idle loop engaged",
        "Dreaming of uptime",
        "Zzz… 200 OK",
        "Docked in the deep",
        "Low-power mode… ahh",
    )

    private val levelUp = listOf(
        "LEVEL UP!",
        "New rank unlocked!",
        "Stronger ink!",
        "Promotion: deep sea",
        "XP tastes like victory",
    )

    private val evolve = listOf(
        "I grew a ring!",
        "Evolution complete",
        "Bigger tentacles!",
        "Stage cleared — nice",
        "Behold: new form",
    )

    private val hungry = listOf(
        "Feed the kraken…",
        "Hunger.exe critical",
        "Got snacks?",
        "Stomach ping: empty",
        "Krill buffer underrun",
    )

    private val tired = listOf(
        "Battery low…",
        "Need a rest cycle",
        "Yawn.exe",
        "CPU thermal throttle…",
        "Dock me, captain",
    )

    private val sadMood = listOf(
        "Mood buffer empty",
        "Cheer me up?",
        "Feeling packet-lossy",
        "Play would help…",
        "Lonely in the LAN",
    )

    private val dailyDone = listOf(
        "Daily quests cleared!",
        "Challenge board: green",
        "Streak fuel secured",
    )

    private val streak = listOf(
        "Streak online!",
        "Another day docked",
        "Habit protocols strong",
    )

    fun randomIdle(): String = idleHappy.random()
    fun forTap(): String = tap.random()
    fun forFeed(): String = feed.random()
    fun forPlay(): String = play.random()
    fun forRest(): String = rest.random()
    fun forLevelUp(): String = levelUp.random()
    fun forEvolve(): String = evolve.random()
    fun forDailyComplete(): String = dailyDone.random()
    fun forStreak(): String = streak.random()

    /** Prefer need-specific lines when low; else mood-weighted idle. */
    fun forNeeds(hunger: Float, energy: Float, mood: Float): String? = when {
        hunger < 25f -> hungry.random()
        energy < 25f -> tired.random()
        mood < 25f -> sadMood.random()
        hunger < 40f && hunger <= energy && hunger <= mood -> hungry.random()
        energy < 40f && energy <= mood -> tired.random()
        mood < 40f -> sadMood.random()
        else -> null
    }

    fun ambient(hunger: Float, energy: Float, mood: Float): String {
        forNeeds(hunger, energy, mood)?.let { return it }
        val avg = (hunger + energy + mood) / 3f
        return when {
            avg >= 70f -> idleHappy.random()
            avg >= 45f -> idleContent.random()
            else -> listOf(hungry, tired, sadMood).random().random()
        }
    }
}
