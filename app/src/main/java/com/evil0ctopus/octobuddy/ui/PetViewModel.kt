package com.evil0ctopus.octobuddy.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.evil0ctopus.octobuddy.data.PetPreferences
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
 * Local-only pet needs. Future premium cosmetics / boosts can hook in here
 * (Play Billing not implemented in v1 — keep the app free for now).
 */
class PetViewModel(
    private val preferences: PetPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetUiState())
    val uiState: StateFlow<PetUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null

    init {
        viewModelScope.launch {
            val stored = preferences.petState.first()
            val decayed = applyDecay(stored, System.currentTimeMillis())
            preferences.save(decayed)
            _uiState.value = PetUiState.from(decayed)
            startTicker()
        }
    }

    fun onResume() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val current = _uiState.value.toPetState(now)
            val decayed = applyDecay(current, now)
            preferences.save(decayed)
            _uiState.value = PetUiState.from(decayed)
        }
    }

    fun tapPet() {
        mutate { state ->
            state.copy(mood = min(100.0, state.mood + 8.0))
        }
    }

    fun feed() {
        mutate { state ->
            state.copy(
                hunger = min(100.0, state.hunger + 18.0),
                mood = min(100.0, state.mood + 4.0),
            )
        }
    }

    fun play() {
        mutate { state ->
            state.copy(
                mood = (state.mood + 12.0).coerceIn(0.0, 100.0),
                hunger = (state.hunger - 4.0).coerceIn(0.0, 100.0),
                energy = (state.energy - 6.0).coerceIn(0.0, 100.0),
            )
        }
    }

    fun rest() {
        mutate { state ->
            state.copy(
                energy = (state.energy + 20.0).coerceIn(0.0, 100.0),
                mood = (state.mood + 3.0).coerceIn(0.0, 100.0),
            )
        }
    }

    /**
     * Renames the pet. Trims whitespace, clamps to 1–24 chars.
     * Blank (after trim) is rejected and the previous name is kept.
     */
    fun renamePet(rawName: String) {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return
        val clamped = trimmed.take(MAX_PET_NAME_LENGTH)
        mutate { state -> state.copy(petName = clamped) }
    }

    private fun mutate(block: (PetState) -> PetState) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val next = block(_uiState.value.toPetState(now)).copy(lastUpdatedMillis = now)
            preferences.save(next)
            _uiState.value = PetUiState.from(next)
        }
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                val now = System.currentTimeMillis()
                val decayed = applyDecay(_uiState.value.toPetState(now), now)
                preferences.save(decayed)
                _uiState.value = PetUiState.from(decayed)
            }
        }
    }

    /**
     * Hunger drains a bit faster than mood; energy sits between them.
     * Roughly: ~1 hunger / 3 min, ~1 energy / 4 min, ~1 mood / 5 min while away.
     */
    private fun applyDecay(state: PetState, now: Long): PetState {
        val elapsedMs = max(0L, now - state.lastUpdatedMillis)
        val minutes = elapsedMs / 60_000.0
        val hungerLoss = minutes / 3.0
        val energyLoss = minutes / 4.0
        val moodLoss = minutes / 5.0
        return state.copy(
            hunger = (state.hunger - hungerLoss).coerceIn(0.0, 100.0),
            energy = (state.energy - energyLoss).coerceIn(0.0, 100.0),
            mood = (state.mood - moodLoss).coerceIn(0.0, 100.0),
            lastUpdatedMillis = now,
        )
    }

    companion object {
        const val MAX_PET_NAME_LENGTH = 24
    }
}

data class PetUiState(
    val hunger: Float = 80f,
    val mood: Float = 80f,
    val energy: Float = 80f,
    val petName: String = "OctoBuddy",
    val statusText: String = "OctoBuddy is happy",
) {
    fun toPetState(now: Long = System.currentTimeMillis()) = PetState(
        hunger = hunger.toDouble(),
        mood = mood.toDouble(),
        energy = energy.toDouble(),
        lastUpdatedMillis = now,
        petName = petName,
    )

    companion object {
        fun from(state: PetState): PetUiState {
            val h = state.hunger.toFloat()
            val m = state.mood.toFloat()
            val e = state.energy.toFloat()
            val name = state.petName.ifBlank { "OctoBuddy" }
            return PetUiState(
                hunger = h,
                mood = m,
                energy = e,
                petName = name,
                statusText = statusFor(name, h, m, e),
            )
        }

        /** Priority: hunger first, then energy (tired), then mood. */
        private fun statusFor(name: String, hunger: Float, mood: Float, energy: Float): String = when {
            hunger < 25f -> "$name is hungry"
            energy < 25f -> "$name is tired"
            mood < 25f -> "$name is sleepy"
            hunger < 50f && mood < 50f -> "$name could use a snack and a cuddle"
            hunger >= 70f && mood >= 70f && energy >= 70f -> "$name is happy"
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
