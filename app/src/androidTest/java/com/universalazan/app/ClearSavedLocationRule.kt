package com.universalazan.app

import androidx.test.platform.app.InstrumentationRegistry
import com.universalazan.app.data.preferences.LocationRepository
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

/** The app's real location storage on the test device. */
fun testLocationRepository(): LocationRepository =
    (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as UniversalAzanApp)
        .container.locationRepository

/** Removes any saved location before (and after) a test, so every test starts the same way. */
class ClearSavedLocationRule : ExternalResource() {
    override fun before() = runBlocking { testLocationRepository().clear() }
    override fun after() = runBlocking { testLocationRepository().clear() }
}
