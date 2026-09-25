package com.evil0ctopus.octobuddy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.petDataStore: DataStore<Preferences> by preferencesDataStore(name = "octobuddy_pet")

data class PetState(
    val hunger: Double = 80.0,
    val mood: Double = 80.0,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val petName: String = "OctoBuddy",
)

class PetPreferences(private val context: Context) {
    private val hungerKey = doublePreferencesKey("hunger")
    private val moodKey = doublePreferencesKey("mood")
    private val lastUpdatedKey = longPreferencesKey("last_updated_millis")
    private val petNameKey = stringPreferencesKey("pet_name")

    val petState: Flow<PetState> = context.petDataStore.data.map { prefs ->
        PetState(
            hunger = prefs[hungerKey] ?: 80.0,
            mood = prefs[moodKey] ?: 80.0,
            lastUpdatedMillis = prefs[lastUpdatedKey] ?: System.currentTimeMillis(),
            petName = prefs[petNameKey] ?: "OctoBuddy",
        )
    }

    suspend fun save(state: PetState) {
        context.petDataStore.edit { prefs ->
            prefs[hungerKey] = state.hunger.coerceIn(0.0, 100.0)
            prefs[moodKey] = state.mood.coerceIn(0.0, 100.0)
            prefs[lastUpdatedKey] = state.lastUpdatedMillis
            prefs[petNameKey] = state.petName
        }
    }
}
