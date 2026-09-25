package com.evil0ctopus.octobuddy.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.evil0ctopus.octobuddy.data.Achievement
import com.evil0ctopus.octobuddy.data.CareActionType
import com.evil0ctopus.octobuddy.data.Cosmetic
import com.evil0ctopus.octobuddy.data.DailyChallengeEngine
import com.evil0ctopus.octobuddy.data.DailyState
import com.evil0ctopus.octobuddy.data.EquippedCosmetics
import com.evil0ctopus.octobuddy.data.OctoQuips
import com.evil0ctopus.octobuddy.data.PetPreferences
import com.evil0ctopus.octobuddy.data.PetProgress
import com.evil0ctopus.octobuddy.data.PetStage
import com.evil0ctopus.octobuddy.data.PetState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Local-only pet needs + XP / evolution + daily challenges + cosmetics.
 * Play Billing not implemented — stay free.
 */
class PetViewModel(
    private val preferences: PetPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetUiState())
    val uiState: StateFlow<PetUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null
    private var quipClearJob: Job? = null
    private var ambientQuipJob: Job? = null

    init {
        viewModelScope.launch {
            val stored = preferences.petState.first()
            val decayed = applyDecay(stored, System.currentTimeMillis())
            val withDaily = decayed.withDaily(
                DailyChallengeEngine.ensureToday(decayed.dailyState()),
            )
            val withAchievements = evaluateAchievements(withDaily)
            preferences.save(withAchievements)
            _uiState.value = PetUiState.from(withAchievements)
            startTicker()
            startAmbientQuips()
        }
    }

    fun onResume() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val current = _uiState.value.toPetState(now)
            val withDaily = current.withDaily(
                DailyChallengeEngine.ensureToday(current.dailyState(), DailyChallengeEngine.todayKey(now)),
            )
            val decayed = evaluateAchievements(applyDecay(withDaily, now))
            preferences.save(decayed)
            _uiState.value = PetUiState.from(decayed).withTransient(_uiState.value)
        }
    }

    fun tapPet() = careAction(PetAction.Tap, CareActionType.Tap, PetProgress.XP_TAP, OctoQuips.forTap()) { state ->
        state.copy(
            mood = min(100.0, state.mood + 8.0),
            xp = state.xp + PetProgress.XP_TAP,
            tapCount = state.tapCount + 1,
        )
    }

    fun feed() = careAction(PetAction.Feed, CareActionType.Feed, PetProgress.XP_FEED, OctoQuips.forFeed()) { state ->
        state.copy(
            hunger = min(100.0, state.hunger + 18.0),
            mood = min(100.0, state.mood + 4.0),
            xp = state.xp + PetProgress.XP_FEED,
            feedCount = state.feedCount + 1,
        )
    }

    fun play() = careAction(PetAction.Play, CareActionType.Play, PetProgress.XP_PLAY, OctoQuips.forPlay()) { state ->
        state.copy(
            mood = (state.mood + 12.0).coerceIn(0.0, 100.0),
            hunger = (state.hunger - 4.0).coerceIn(0.0, 100.0),
            energy = (state.energy - 6.0).coerceIn(0.0, 100.0),
            xp = state.xp + PetProgress.XP_PLAY,
            playCount = state.playCount + 1,
        )
    }

    fun rest() = careAction(PetAction.Rest, CareActionType.Rest, PetProgress.XP_REST, OctoQuips.forRest()) { state ->
        state.copy(
            energy = (state.energy + 20.0).coerceIn(0.0, 100.0),
            mood = (state.mood + 3.0).coerceIn(0.0, 100.0),
            xp = state.xp + PetProgress.XP_REST,
            restCount = state.restCount + 1,
        )
    }

    fun renamePet(rawName: String) {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return
        val clamped = trimmed.take(MAX_PET_NAME_LENGTH)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val prev = _uiState.value
            val next = evaluateAchievements(
                prev.toPetState(now).copy(petName = clamped, lastUpdatedMillis = now),
            )
            preferences.save(next)
            _uiState.value = PetUiState.from(next).withTransient(prev)
        }
    }

    fun completeOnboarding(rawName: String) {
        val trimmed = rawName.trim().ifEmpty { "OctoBuddy" }.take(MAX_PET_NAME_LENGTH)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val prev = _uiState.value
            val next = evaluateAchievements(
                prev.toPetState(now).copy(
                    petName = trimmed,
                    onboardingComplete = true,
                    lastUpdatedMillis = now,
                ),
            )
            preferences.save(next)
            showQuip("Ahoy, $trimmed!")
            _uiState.value = PetUiState.from(next).withTransient(prev).copy(
                speechText = "Ahoy, $trimmed!",
            )
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val prev = _uiState.value
            val next = prev.toPetState(now).copy(hapticsEnabled = enabled)
            preferences.save(next)
            _uiState.value = PetUiState.from(next).withTransient(prev)
        }
    }

    fun equipCosmetic(cosmetic: Cosmetic) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val prev = _uiState.value
            val state = prev.toPetState(now)
            if (!cosmetic.isUnlocked(state.achievementsMask, state.level, state.careStreak)) return@launch
            val equipped = state.equipped().withSlot(cosmetic.slot, cosmetic)
            val next = state.withEquipped(equipped).copy(lastUpdatedMillis = now)
            preferences.save(next)
            _uiState.value = PetUiState.from(next).withTransient(prev)
        }
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(
            showSettings = true, showAchievements = false, showUnlocks = false, showDaily = false,
        )
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(showSettings = false)
    }

    fun openAchievements() {
        _uiState.value = _uiState.value.copy(
            showAchievements = true, showSettings = false, showUnlocks = false, showDaily = false,
        )
    }

    fun closeAchievements() {
        _uiState.value = _uiState.value.copy(showAchievements = false)
    }

    fun openUnlocks() {
        _uiState.value = _uiState.value.copy(
            showUnlocks = true, showSettings = false, showAchievements = false, showDaily = false,
        )
    }

    fun closeUnlocks() {
        _uiState.value = _uiState.value.copy(showUnlocks = false)
    }

    fun openDaily() {
        _uiState.value = _uiState.value.copy(
            showDaily = true, showSettings = false, showAchievements = false, showUnlocks = false,
        )
    }

    fun closeDaily() {
        _uiState.value = _uiState.value.copy(showDaily = false)
    }

    fun dismissEvolve() {
        _uiState.value = _uiState.value.copy(evolvedToStage = null)
    }

    fun dismissLevelUp() {
        _uiState.value = _uiState.value.copy(leveledTo = null)
    }

    fun dismissAchievementToast() {
        _uiState.value = _uiState.value.copy(newAchievement = null)
    }

    fun resetPet() {
        viewModelScope.launch {
            preferences.resetPet()
            val fresh = preferences.petState.first()
            val withDaily = fresh.withDaily(DailyChallengeEngine.ensureToday(fresh.dailyState()))
            preferences.save(withDaily)
            _uiState.value = PetUiState.from(withDaily)
        }
    }

    private fun careAction(
        action: PetAction,
        careType: CareActionType,
        xpGain: Long,
        quip: String,
        block: (PetState) -> PetState,
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val prev = _uiState.value
            val prevStreak = prev.careStreak
            val prevDailyComplete = prev.daily.allComplete
            val rawNext = block(prev.toPetState(now)).copy(lastUpdatedMillis = now)
            val dailyNext = DailyChallengeEngine.applyCare(
                state = DailyChallengeEngine.ensureToday(rawNext.dailyState(), DailyChallengeEngine.todayKey(now)),
                action = careType,
                deltaXp = xpGain,
                hunger = rawNext.hunger,
                mood = rawNext.mood,
                nowMillis = now,
            )
            val withDaily = rawNext.withDaily(dailyNext)
            val nextState = evaluateAchievements(withDaily)
            preferences.save(nextState)

            val leveled = nextState.level > prev.level
            val evolved = nextState.stage != prev.stage
            val newAchievements = Achievement.all().filter { a ->
                Achievement.isUnlocked(nextState.achievementsMask, a) &&
                    !Achievement.isUnlocked(prev.achievementsMask, a)
            }
            val streakGrew = nextState.careStreak > prevStreak
            val dailyJustDone = nextState.dailyState().allComplete && !prevDailyComplete

            val speech = when {
                evolved -> OctoQuips.forEvolve()
                leveled -> OctoQuips.forLevelUp()
                dailyJustDone -> OctoQuips.forDailyComplete()
                streakGrew && nextState.careStreak >= 2 -> OctoQuips.forStreak()
                else -> quip
            }
            scheduleQuipClear()

            _uiState.value = PetUiState.from(nextState).copy(
                actionEpoch = prev.actionEpoch + 1,
                lastAction = action,
                evolveEpoch = if (evolved) prev.evolveEpoch + 1 else prev.evolveEpoch,
                evolvedToStage = if (evolved) nextState.stage else prev.evolvedToStage,
                levelUpEpoch = if (leveled) prev.levelUpEpoch + 1 else prev.levelUpEpoch,
                leveledTo = if (leveled) nextState.level else prev.leveledTo,
                speechText = speech,
                showSettings = prev.showSettings,
                showAchievements = prev.showAchievements,
                showUnlocks = prev.showUnlocks,
                showDaily = prev.showDaily,
                newAchievement = newAchievements.firstOrNull() ?: prev.newAchievement,
                careBurstEpoch = prev.careBurstEpoch + 1,
            )
        }
    }

    private fun showQuip(text: String) {
        _uiState.value = _uiState.value.copy(speechText = text)
        scheduleQuipClear()
    }

    private fun scheduleQuipClear() {
        quipClearJob?.cancel()
        quipClearJob = viewModelScope.launch {
            delay(2800L)
            _uiState.value = _uiState.value.copy(speechText = null)
        }
    }

    private fun startAmbientQuips() {
        ambientQuipJob?.cancel()
        ambientQuipJob = viewModelScope.launch {
            while (isActive) {
                delay(14_000L)
                val s = _uiState.value
                if (!s.onboardingComplete) continue
                if (s.speechText != null) continue
                if (s.evolvedToStage != null || s.leveledTo != null) continue
                showQuip(OctoQuips.ambient(s.hunger, s.energy, s.mood))
            }
        }
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                val now = System.currentTimeMillis()
                val prev = _uiState.value
                val withDaily = prev.toPetState(now).withDaily(
                    DailyChallengeEngine.ensureToday(prev.toPetState(now).dailyState(), DailyChallengeEngine.todayKey(now)),
                )
                val decayed = evaluateAchievements(applyDecay(withDaily, now))
                preferences.save(decayed)
                _uiState.value = PetUiState.from(decayed).withTransient(prev)
            }
        }
    }

    private fun applyDecay(state: PetState, now: Long): PetState {
        val elapsedMs = max(0L, now - state.lastUpdatedMillis)
        val minutes = elapsedMs / 60_000.0
        return state.copy(
            hunger = (state.hunger - minutes / 3.0).coerceIn(0.0, 100.0),
            energy = (state.energy - minutes / 4.0).coerceIn(0.0, 100.0),
            mood = (state.mood - minutes / 5.0).coerceIn(0.0, 100.0),
            lastUpdatedMillis = now,
        )
    }

    private fun evaluateAchievements(state: PetState): PetState {
        var mask = state.achievementsMask
        fun unlock(a: Achievement) {
            mask = mask or a.mask
        }
        if (state.tapCount >= 1) unlock(Achievement.FirstTap)
        if (state.feedCount >= 1) unlock(Achievement.FirstFeed)
        if (state.playCount >= 1) unlock(Achievement.FirstPlay)
        if (state.restCount >= 1) unlock(Achievement.FirstRest)
        if (state.tapCount >= 25) unlock(Achievement.Tap25)
        if (state.feedCount >= 10) unlock(Achievement.Feed10)
        if (state.playCount >= 10) unlock(Achievement.Play10)
        if (state.tapCount >= 100) unlock(Achievement.Tap100)
        if (state.feedCount >= 50) unlock(Achievement.Feed50)
        if (state.playCount >= 50) unlock(Achievement.Play50)
        if (state.restCount >= 25) unlock(Achievement.Rest25)
        if (state.stage == PetStage.Juvenile || state.stage == PetStage.Adult) {
            unlock(Achievement.ReachJuvenile)
        }
        if (state.stage == PetStage.Adult) unlock(Achievement.ReachAdult)
        if (state.level >= 5) unlock(Achievement.Level5)
        if (state.level >= 10) unlock(Achievement.Level10)
        if (state.level >= 15) unlock(Achievement.Level15)
        if (state.level >= 20) unlock(Achievement.Level20)
        if (state.level >= PetProgress.MAX_LEVEL) unlock(Achievement.MaxLevel)
        if (state.onboardingComplete && state.petName.isNotBlank()) {
            unlock(Achievement.NamedBuddy)
        }
        if (state.careStreak >= 3) unlock(Achievement.Streak3)
        if (state.careStreak >= 7) unlock(Achievement.Streak7)
        if (state.careStreak >= 14) unlock(Achievement.Streak14)
        if (state.dailyAllEverComplete || state.dailyState().allComplete) {
            unlock(Achievement.DailyAll)
        }
        if (state.totalCareDays >= 10) unlock(Achievement.CareDay10)
        return if (mask != state.achievementsMask) state.copy(achievementsMask = mask) else state
    }

    companion object {
        const val MAX_PET_NAME_LENGTH = 24
    }
}

enum class PetAction {
    Tap, Feed, Play, Rest,
}

data class PetUiState(
    val hunger: Float = 80f,
    val mood: Float = 80f,
    val energy: Float = 80f,
    val petName: String = "OctoBuddy",
    val statusText: String = "OctoBuddy is happy",
    val xp: Long = 0L,
    val level: Int = 1,
    val stage: PetStage = PetStage.Hatchling,
    val rankTitle: String = "Inkling",
    val xpProgress: Float = 0f,
    val xpToNext: Long = PetProgress.XP_PER_LEVEL,
    val onboardingComplete: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val achievementsMask: Long = 0L,
    val tapCount: Int = 0,
    val feedCount: Int = 0,
    val playCount: Int = 0,
    val restCount: Int = 0,
    val careStreak: Int = 0,
    val totalCareDays: Int = 0,
    val daily: DailyState = DailyState(),
    val equipped: EquippedCosmetics = EquippedCosmetics(),
    val actionEpoch: Int = 0,
    val lastAction: PetAction = PetAction.Tap,
    val evolveEpoch: Int = 0,
    val evolvedToStage: PetStage? = null,
    val levelUpEpoch: Int = 0,
    val leveledTo: Int? = null,
    val speechText: String? = null,
    val showSettings: Boolean = false,
    val showAchievements: Boolean = false,
    val showUnlocks: Boolean = false,
    val showDaily: Boolean = false,
    val newAchievement: Achievement? = null,
    val careBurstEpoch: Int = 0,
    val dailyAllEverComplete: Boolean = false,
    val ready: Boolean = false,
) {
    fun toPetState(now: Long = System.currentTimeMillis()) = PetState(
        hunger = hunger.toDouble(),
        mood = mood.toDouble(),
        energy = energy.toDouble(),
        lastUpdatedMillis = now,
        petName = petName,
        xp = xp,
        onboardingComplete = onboardingComplete,
        hapticsEnabled = hapticsEnabled,
        achievementsMask = achievementsMask,
        tapCount = tapCount,
        feedCount = feedCount,
        playCount = playCount,
        restCount = restCount,
        dailyDayKey = daily.dayKey,
        dailyEncoded = DailyChallengeEngine.encode(daily.challenges),
        careStreak = careStreak,
        lastCareDayKey = daily.lastCareDayKey,
        totalCareDays = totalCareDays,
        dailyAllEverComplete = dailyAllEverComplete || daily.allComplete,
        equippedFrameId = equipped.frameId,
        equippedEffectId = equipped.effectId,
        equippedHeadId = equipped.headId,
        equippedAccentId = equipped.accentId,
    )

    fun withTransient(prev: PetUiState) = copy(
        actionEpoch = prev.actionEpoch,
        lastAction = prev.lastAction,
        evolveEpoch = prev.evolveEpoch,
        evolvedToStage = prev.evolvedToStage,
        levelUpEpoch = prev.levelUpEpoch,
        leveledTo = prev.leveledTo,
        speechText = prev.speechText,
        showSettings = prev.showSettings,
        showAchievements = prev.showAchievements,
        showUnlocks = prev.showUnlocks,
        showDaily = prev.showDaily,
        newAchievement = prev.newAchievement,
        careBurstEpoch = prev.careBurstEpoch,
    )

    companion object {
        fun from(state: PetState): PetUiState {
            val h = state.hunger.toFloat()
            val m = state.mood.toFloat()
            val e = state.energy.toFloat()
            val name = state.petName.ifBlank { "OctoBuddy" }
            val daily = DailyChallengeEngine.ensureToday(state.dailyState())
            return PetUiState(
                hunger = h,
                mood = m,
                energy = e,
                petName = name,
                statusText = statusFor(name, h, m, e, state.stage),
                xp = state.xp,
                level = state.level,
                stage = state.stage,
                rankTitle = state.rankTitle,
                xpProgress = PetProgress.xpProgress(state.xp),
                xpToNext = PetProgress.xpToNext(state.xp),
                onboardingComplete = state.onboardingComplete,
                hapticsEnabled = state.hapticsEnabled,
                achievementsMask = state.achievementsMask,
                tapCount = state.tapCount,
                feedCount = state.feedCount,
                playCount = state.playCount,
                restCount = state.restCount,
                careStreak = state.careStreak,
                totalCareDays = state.totalCareDays,
                daily = daily,
                equipped = state.equipped(),
                dailyAllEverComplete = state.dailyAllEverComplete || daily.allComplete,
                ready = true,
            )
        }

        private fun statusFor(
            name: String,
            hunger: Float,
            mood: Float,
            energy: Float,
            stage: PetStage,
        ): String = when {
            hunger < 25f -> "$name is hungry"
            energy < 25f -> "$name is tired"
            mood < 25f -> "$name is sleepy"
            hunger < 50f && mood < 50f -> "$name could use a snack and a cuddle"
            hunger >= 70f && mood >= 70f && energy >= 70f ->
                "$name is a happy ${stage.displayName.lowercase()}"
            mood >= 60f -> "$name is content"
            else -> "$name is resting"
        }
    }
}

class PetViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PetViewModel::class.java)) {
            return PetViewModel(PetPreferences(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
