package com.evil0ctopus.octobuddy.data

/**
 * PorkChop-style speech-bubble quips — Evil0ctopus cyber-pirate voice.
 * Short lines fit a phone bubble (~32 chars ideal).
 */
object OctoQuips {
    private val idle = listOf(
        "Beep-arrr!",
        "Hex marks the spot",
        "Ink over IP",
        "Code. Craft. Conquer.",
        "Cyan rings online",
        "Packets taste like krill",
        "Compile me lucky",
        "Deep packets call…",
        "Who needs Wi‑Fi?",
        "Tentacles > toolbars",
        "Steady as she code",
        "The abyss pings back",
        "Kraken.exe ready",
        "Soft reboot, hard stare",
        "Glowing and knowing",
        "Copper belly, steel will",
        "Not all treasure be gold",
        "Mind the boom… buffer",
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
    )

    private val play = listOf(
        "Yo ho ho!",
        "Jig on the hex grid",
        "Race you to root!",
        "Play mode: mischief",
        "Cannonball… of ink!",
    )

    private val rest = listOf(
        "Power nap.exe",
        "Idle loop engaged",
        "Dreaming of uptime",
        "Zzz… 200 OK",
        "Docked in the deep",
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
    )

    private val tired = listOf(
        "Battery low…",
        "Need a rest cycle",
        "Yawn.exe",
    )

    private val sleepyMood = listOf(
        "Mood buffer empty",
        "Cheer me up?",
        "Feeling packet-lossy",
    )

    fun randomIdle(): String = idle.random()
    fun forTap(): String = tap.random()
    fun forFeed(): String = feed.random()
    fun forPlay(): String = play.random()
    fun forRest(): String = rest.random()
    fun forLevelUp(): String = levelUp.random()
    fun forEvolve(): String = evolve.random()

    fun forNeeds(hunger: Float, energy: Float, mood: Float): String? = when {
        hunger < 25f -> hungry.random()
        energy < 25f -> tired.random()
        mood < 25f -> sleepyMood.random()
        else -> null
    }
}
