package com.universalazan.app.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.universalazan.app.data.locations.LocationSource
import com.universalazan.app.data.locations.SavedLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocationRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val london = SavedLocation("London", 51.5085, -0.1257, "Europe/London", LocationSource.City)

    private fun TestScope.newDataStore(file: File = File(tmp.root, "settings.preferences_pb")) =
        PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })

    @Test
    fun nothingSavedInitially() = runTest {
        assertNull(LocationRepository(newDataStore()).savedLocation.first())
    }

    @Test
    fun saveAndReadBack() = runTest {
        val repository = LocationRepository(newDataStore())
        repository.save(london)
        assertEquals(london, repository.savedLocation.first())
    }

    @Test
    fun survivesReopeningTheFile() = runBlocking {
        val file = File(tmp.root, "persist.preferences_pb")
        val firstScope = CoroutineScope(Dispatchers.IO + Job())
        LocationRepository(PreferenceDataStoreFactory.create(scope = firstScope) { file }).save(london)
        firstScope.coroutineContext.job.cancelAndJoin()

        // A fresh DataStore on the same file stands in for an app restart.
        val secondScope = CoroutineScope(Dispatchers.IO + Job())
        val reopened = LocationRepository(PreferenceDataStoreFactory.create(scope = secondScope) { file })
            .savedLocation.first()
        secondScope.coroutineContext.job.cancelAndJoin()
        assertEquals(london, reopened)
    }

    @Test
    fun updateTimeZoneOnlyChangesTheZone() = runTest {
        val repository = LocationRepository(newDataStore())
        repository.save(london.copy(source = LocationSource.Device))
        repository.updateTimeZone("Europe/Paris")
        assertEquals(
            london.copy(source = LocationSource.Device, timeZoneId = "Europe/Paris"),
            repository.savedLocation.first(),
        )
    }

    @Test
    fun updateTimeZoneWithoutLocationDoesNothing() = runTest {
        val repository = LocationRepository(newDataStore())
        repository.updateTimeZone("Europe/Paris")
        assertNull(repository.savedLocation.first())
    }

    @Test
    fun clearRemovesLocation() = runTest {
        val repository = LocationRepository(newDataStore())
        repository.save(london)
        repository.clear()
        assertNull(repository.savedLocation.first())
    }

    @Test
    fun corruptCoordinatesAreIgnored() = runTest {
        val dataStore = newDataStore()
        dataStore.edit {
            it[stringPreferencesKey("location_name")] = "Bad"
            it[doublePreferencesKey("location_latitude")] = 123.0
            it[doublePreferencesKey("location_longitude")] = 0.0
            it[stringPreferencesKey("location_time_zone")] = "UTC"
        }
        assertNull(LocationRepository(dataStore).savedLocation.first())
    }

    @Test
    fun unknownTimeZoneIsKeptButFlagged() = runTest {
        val repository = LocationRepository(newDataStore())
        repository.save(london.copy(timeZoneId = "Removed/Zone"))
        val loaded = repository.savedLocation.first()!!
        assertEquals("Removed/Zone", loaded.timeZoneId)
        assertNull(loaded.zoneId)
    }
}
