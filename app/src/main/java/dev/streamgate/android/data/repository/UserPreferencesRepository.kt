package dev.streamgate.android.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val FRAME_RATE = intPreferencesKey("frame_rate")

    val frameRateFlow: Flow<Int> = dataStore.data
        .map { preferences -> preferences[FRAME_RATE] ?: 30 }

    suspend fun saveFrameRate(frameRate: Int) {
        dataStore.edit { preferences -> preferences[FRAME_RATE] = frameRate }
    }
}
