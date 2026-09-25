package com.evil0ctopus.octobuddy.data

/**
 * Light PorkChop-style achievement / challenge layer.
 * IDs are stable bit flags persisted as a Long mask.
 */
enum class Achievement(
    val bit: Int,
    val title: String,
    val description: String,
) {
    FirstTap(0, "First Boop", "Tap your buddy once"),
    FirstFeed(1, "Snack Protocol", "Feed your buddy"),
    FirstPlay(2, "Ink Jig", "Play with your buddy"),
    FirstRest(3, "Dock & Dream", "Let your buddy rest"),
    Tap25(4, "Boop Cadet", "Tap 25 times"),
    Feed10(5, "Krill Courier", "Feed 10 times"),
    Play10(6, "Play Captain", "Play 10 times"),
    ReachJuvenile(7, "Ring Runner", "Evolve to Juvenile"),
    ReachAdult(8, "Cyan Captain", "Evolve to Adult"),
    Level10(9, "Double Digits", "Reach level 10"),
    Level20(10, "Deep Decoder", "Reach level 20"),
    MaxLevel(11, "Dread Octopus", "Hit max level 30"),
    NamedBuddy(12, "Callsign Locked", "Name your OctoBuddy"),
    ;

    val mask: Long get() = 1L shl bit

    companion object {
        fun all(): List<Achievement> = entries
        fun unlocked(mask: Long): List<Achievement> = entries.filter { mask and it.mask != 0L }
        fun isUnlocked(mask: Long, a: Achievement): Boolean = mask and a.mask != 0L
    }
}

data class CareCounts(
    val taps: Int = 0,
    val feeds: Int = 0,
    val plays: Int = 0,
    val rests: Int = 0,
)
