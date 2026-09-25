package com.evil0ctopus.octobuddy.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.random.Random

/**
 * PorkChop-energy daily goals + care streak.
 * Day key is local calendar date (America/Chicago when device is set that way).
 */
enum class ChallengeKind(
    val id: String,
    val defaultTarget: Int,
    val titleTemplate: String,
    val descriptionTemplate: String,
) {
    FeedTimes("feed", 3, "Feed %dx", "Feed your buddy %d times today"),
    PlayOnce("play", 1, "Play %dx", "Play with your buddy %d time(s)"),
    RestOnce("rest", 1, "Rest %dx", "Let your buddy rest %d time(s)"),
    TapTimes("tap", 10, "Boop %dx", "Tap your buddy %d times"),
    EarnXp("xp", 25, "Earn %d XP", "Bank %d XP from care today"),
    KeepMood("mood", 50, "Mood ≥ %d", "Keep mood at or above %d after any care"),
    KeepHunger("hunger", 50, "Hunger ≥ %d", "Keep hunger at or above %d after any care"),
    ;

    fun title(target: Int): String = titleTemplate.format(target)
    fun description(target: Int): String = descriptionTemplate.format(target)
}

data class DailyChallenge(
    val kind: ChallengeKind,
    val target: Int,
    val progress: Int = 0,
) {
    val complete: Boolean get() = progress >= target
    val title: String get() = kind.title(target)
    val description: String get() = kind.description(target)
    val fraction: Float get() = if (target <= 0) 1f else (progress.toFloat() / target).coerceIn(0f, 1f)
}

data class DailyState(
    val dayKey: String = "",
    val challenges: List<DailyChallenge> = emptyList(),
    /** Consecutive local days with any care action. */
    val careStreak: Int = 0,
    /** Last local day key on which any care happened. */
    val lastCareDayKey: String = "",
    /** Total days ever cared (lifetime). */
    val totalCareDays: Int = 0,
) {
    val allComplete: Boolean get() = challenges.isNotEmpty() && challenges.all { it.complete }
    val completedCount: Int get() = challenges.count { it.complete }
}

object DailyChallengeEngine {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val dayFmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayKey(nowMillis: Long = System.currentTimeMillis()): String {
        val instant = java.time.Instant.ofEpochMilli(nowMillis)
        return LocalDate.ofInstant(instant, zone).format(dayFmt)
    }

    fun yesterdayKey(today: String): String {
        val d = LocalDate.parse(today, dayFmt)
        return d.minusDays(1).format(dayFmt)
    }

    /** Deterministic 1–3 challenges from the day key so everyone gets a stable set. */
    fun generateForDay(dayKey: String): List<DailyChallenge> {
        val seed = dayKey.hashCode().toLong()
        val rng = Random(seed)
        val pool = ChallengeKind.entries.toMutableList()
        // Always include at least one care-action challenge.
        val forced = listOf(ChallengeKind.FeedTimes, ChallengeKind.PlayOnce, ChallengeKind.RestOnce)
        val first = forced[rng.nextInt(forced.size)]
        pool.remove(first)
        val count = 1 + rng.nextInt(3) // 1..3
        val picked = mutableListOf(first)
        while (picked.size < count && pool.isNotEmpty()) {
            val idx = rng.nextInt(pool.size)
            picked += pool.removeAt(idx)
        }
        return picked.map { kind ->
            val target = when (kind) {
                ChallengeKind.FeedTimes -> 2 + rng.nextInt(3) // 2–4
                ChallengeKind.PlayOnce -> 1 + rng.nextInt(2) // 1–2
                ChallengeKind.RestOnce -> 1 + rng.nextInt(2)
                ChallengeKind.TapTimes -> 8 + rng.nextInt(13) // 8–20
                ChallengeKind.EarnXp -> listOf(20, 25, 30, 40)[rng.nextInt(4)]
                ChallengeKind.KeepMood -> listOf(50, 60, 70)[rng.nextInt(3)]
                ChallengeKind.KeepHunger -> listOf(50, 60, 70)[rng.nextInt(3)]
            }
            DailyChallenge(kind = kind, target = target, progress = 0)
        }
    }

    /**
     * Roll day forward if needed, then apply a care event.
     * [deltaXp] is XP gained this action; needs are post-action values.
     */
    fun applyCare(
        state: DailyState,
        action: CareActionType,
        deltaXp: Long,
        hunger: Double,
        mood: Double,
        nowMillis: Long = System.currentTimeMillis(),
    ): DailyState {
        val today = todayKey(nowMillis)
        var next = ensureToday(state, today)

        // Streak: any care today continues or starts streak.
        next = updateStreak(next, today)

        val updated = next.challenges.map { ch ->
            if (ch.complete) return@map ch
            val add = when (ch.kind) {
                ChallengeKind.FeedTimes -> if (action == CareActionType.Feed) 1 else 0
                ChallengeKind.PlayOnce -> if (action == CareActionType.Play) 1 else 0
                ChallengeKind.RestOnce -> if (action == CareActionType.Rest) 1 else 0
                ChallengeKind.TapTimes -> if (action == CareActionType.Tap) 1 else 0
                ChallengeKind.EarnXp -> deltaXp.toInt().coerceAtLeast(0)
                ChallengeKind.KeepMood -> if (mood >= ch.target) ch.target else 0
                ChallengeKind.KeepHunger -> if (hunger >= ch.target) ch.target else 0
            }
            when (ch.kind) {
                ChallengeKind.KeepMood, ChallengeKind.KeepHunger ->
                    if (add >= ch.target) ch.copy(progress = ch.target) else ch
                else -> ch.copy(progress = min(ch.target, ch.progress + add))
            }
        }
        return next.copy(challenges = updated)
    }

    fun ensureToday(state: DailyState, today: String = todayKey()): DailyState {
        if (state.dayKey == today && state.challenges.isNotEmpty()) return state
        return state.copy(
            dayKey = today,
            challenges = generateForDay(today),
        )
    }

    private fun updateStreak(state: DailyState, today: String): DailyState {
        if (state.lastCareDayKey == today) return state
        val yesterday = yesterdayKey(today)
        val newStreak = when {
            state.lastCareDayKey.isEmpty() -> 1
            state.lastCareDayKey == yesterday -> state.careStreak + 1
            else -> 1 // broken streak
        }
        return state.copy(
            careStreak = newStreak,
            lastCareDayKey = today,
            totalCareDays = state.totalCareDays + 1,
        )
    }

    /** Encode / decode challenges for DataStore (kind:target:progress|…). */
    fun encode(challenges: List<DailyChallenge>): String =
        challenges.joinToString("|") { "${it.kind.id}:${it.target}:${it.progress}" }

    fun decode(raw: String): List<DailyChallenge> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { part ->
            val bits = part.split(":")
            if (bits.size < 3) return@mapNotNull null
            val kind = ChallengeKind.entries.find { it.id == bits[0] } ?: return@mapNotNull null
            val target = bits[1].toIntOrNull() ?: return@mapNotNull null
            val progress = bits[2].toIntOrNull() ?: 0
            DailyChallenge(kind, target, progress.coerceIn(0, target))
        }
    }
}

enum class CareActionType { Tap, Feed, Play, Rest }
