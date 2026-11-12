package com.apexbytestudio.entrix.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Typed access to the small amount of app state that does not belong in the attendance table:
 * the tracking toggle and the pending (un-confirmed) geofence entry time.
 */
class EntrixPreferencesDataSource(
    private val dataStore: DataStore<Preferences>,
) {
    val trackingEnabled: Flow<Boolean> =
        dataStore.data.map { it[KEY_TRACKING_ENABLED] ?: false }

    suspend fun isTrackingEnabled(): Boolean = trackingEnabled.first()

    suspend fun setTrackingEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_TRACKING_ENABLED] = enabled }
    }

    suspend fun getPendingEnterMillis(officeId: Long): Long? =
        dataStore.data.map { it[pendingEnterKey(officeId)] }.first()

    suspend fun setPendingEnterMillis(officeId: Long, millis: Long?) {
        dataStore.edit { prefs ->
            val key = pendingEnterKey(officeId)
            if (millis == null) prefs.remove(key)
            else prefs[key] = millis
        }
    }

    private companion object {
        val KEY_TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")

        // The pending (un-confirmed) entry time is kept per office so a pending ENTER at one office
        // never blocks or is consumed by a DWELL at another.
        fun pendingEnterKey(officeId: Long) = longPreferencesKey("pending_enter_millis_$officeId")
    }
}
