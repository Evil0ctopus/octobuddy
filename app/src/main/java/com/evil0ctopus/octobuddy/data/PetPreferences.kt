package com.evil0ctopus.octobuddy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.petDataStore: DataStore<Preferences> by preferencesDataStore(name = "octobuddy_pet")

data class PetState(
    val hunger: Double = 80.0,
    val mood: Double = 80.0,
    val energy: Double = 80.0,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val petName: String = "OctoBuddy",
    val xp: Long = 0L,
    val onboardingComplete: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val achievementsMask: Long = 0L,
    val tapCount: Int = 0,
    val feedCount: Int = 0,
    val playCount: Int = 0,
    val restCount: Int = 0,
) {
    val level: Int get() = PetProgress.levelForXp(xp)
    val stage: PetStage get() = PetProgress.stageForXp(xp)
    val rankTitle: String get() = PetProgress.rankTitle(level)
    val careCounts: CareCounts
        get() = CareCounts(taps = tapCount, feeds = feedCount, plays = playCount, rests = restCount)
}

class PetPreferences(private val context: Context) {
    private val hungerKey = doublePreferencesKey("hunger")
    private val moodKey = doublePreferencesKey("mood")
    private val energyKey = doublePreferencesKey("energy")
    private val lastUpdatedKey = longPreferencesKey("last_updated_millis")
    private val petNameKey = stringPreferencesKey("pet_name")
    private val xpKey = longPreferencesKey("xp")
    private val onboardingKey = booleanPreferencesKey("onboarding_complete")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")
    private val achievementsKey = longPreferencesKey("achievements_mask")
    private val tapCountKey = intPreferencesKey("tap_count")
    private val feedCountKey = intPreferencesKey("feed_count")
    private val playCountKey = intPreferencesKey("play_count")
    private val restCountKey = intPreferencesKey("rest_count")

    val petState: Flow<PetState> = context.petDataStore.data.map { prefs ->
        PetState(
            hunger = prefs[hungerKey] ?: 80.0,
            mood = prefs[moodKey] ?: 80.0,
            energy = prefs[energyKey] ?: 80.0,
            lastUpdatedMillis = prefs[lastUpdatedKey] ?: System.currentTimeMillis(),
            petName = prefs[petNameKey] ?: "OctoBuddy",
            xp = prefs[xpKey] ?: 0L,
            onboardingComplete = prefs[onboardingKey] ?: false,
            hapticsEnabled = prefs[hapticsKey] ?: true,
            achievementsMask = prefs[achievementsKey] ?: 0L,
            tapCount = prefs[tapCountKey] ?: 0,
            feedCount = prefs[feedCountKey] ?: 0,
            playCount = prefs[playCountKey] ?: 0,
            restCount = prefs[restCountKey] ?: 0,
        )
    }

    suspend fun save(state: PetState) {
        context.petDataStore.edit { prefs ->
            prefs[hungerKey] = state.hunger.coerceIn(0.0, 100.0)
            prefs[moodKey] = state.mood.coerceIn(0.0, 100.0)
            prefs[energyKey] = state.energy.coerceIn(0.0, 100.0)
            prefs[lastUpdatedKey] = state.lastUpdatedMillis
            prefs[petNameKey] = state.petName
            prefs[xpKey] = state.xp.coerceAtLeast(0L)
            prefs[onboardingKey] = state.onboardingComplete
            prefs[hapticsKey] = state.hapticsEnabled
            prefs[achievementsKey] = state.achievementsMask
            prefs[tapCountKey] = state.tapCount.coerceAtLeast(0)
            prefs[feedCountKey] = state.feedCount.coerceAtLeast(0)
            prefs[playCountKey] = state.playCount.coerceAtLeast(0)
            prefs[restCountKey] = state.restCount.coerceAtLeast(0)
        }
    }

    suspend fun resetPet() {
        context.petDataStore.edit { prefs ->
            val keepHaptics = prefs[hapticsKey] ?: true
            prefs.clear()
            prefs[hungerKey] = 80.0
            prefs[moodKey] = 80.0
            prefs[energyKey] = 80.0
            prefs[lastUpdatedKey] = System.currentTimeMillis()
            prefs[petNameKey] = "OctoBuddy"
            prefs[xpKey] = 0L
            prefs[onboardingKey] = false
            prefs[hapticsKey] = keepHaptics
            prefs[achievementsKey] = 0L
            prefs[tapCountKey] = 0
            prefs[feedCountKey] = 0
            prefs[playCountKey] = 0
            prefs[restCountKey] = 0
        }
    }
}
