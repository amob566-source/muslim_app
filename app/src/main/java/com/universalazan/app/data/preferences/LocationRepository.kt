package com.universalazan.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.domain.location.isValidLatitude
import com.universalazan.app.domain.location.isValidLongitude
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The app's settings file, stored privately on the device. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Saves and reads the user's chosen location. */
class LocationRepository(private val dataStore: DataStore<Preferences>) {

    /** The saved location, or null if none has been chosen (or the stored data is unusable). */
    val savedLocation: Flow<SavedLocation?> = dataStore.data.map { it.toSavedLocation() }

    suspend fun save(location: SavedLocation) {
        dataStore.edit { prefs ->
            prefs[Keys.Name] = location.name
            prefs[Keys.Latitude] = location.latitude
            prefs[Keys.Longitude] = location.longitude
            prefs[Keys.TimeZone] = location.timeZoneId
            prefs[Keys.Source] = location.source.name
        }
    }

    /** Changes only the time zone of the saved location. Does nothing if none is saved. */
    suspend fun updateTimeZone(timeZoneId: String) {
        dataStore.edit { prefs ->
            if (prefs.toSavedLocation() != null) prefs[Keys.TimeZone] = timeZoneId
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.Name)
            prefs.remove(Keys.Latitude)
            prefs.remove(Keys.Longitude)
            prefs.remove(Keys.TimeZone)
            prefs.remove(Keys.Source)
        }
    }

    private fun Preferences.toSavedLocation(): SavedLocation? {
        val name = this[Keys.Name]?.takeIf { it.isNotBlank() } ?: return null
        val latitude = this[Keys.Latitude]?.takeIf(::isValidLatitude) ?: return null
        val longitude = this[Keys.Longitude]?.takeIf(::isValidLongitude) ?: return null
        val timeZone = this[Keys.TimeZone] ?: return null
        val source = LocationSource.entries.firstOrNull { it.name == this[Keys.Source] }
            ?: LocationSource.Manual
        return SavedLocation(name, latitude, longitude, timeZone, source)
    }

    private object Keys {
        val Name = stringPreferencesKey("location_name")
        val Latitude = doublePreferencesKey("location_latitude")
        val Longitude = doublePreferencesKey("location_longitude")
        val TimeZone = stringPreferencesKey("location_time_zone")
        val Source = stringPreferencesKey("location_source")
    }
}
