package com.universalazan.app

import androidx.test.platform.app.InstrumentationRegistry
import com.universalazan.app.data.preferences.CalculationSettingsRepository
import com.universalazan.app.data.preferences.LocationRepository
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

private val container: AppContainer
    get() = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as UniversalAzanApp).container

/** The app's real location storage on the test device. */
fun testLocationRepository(): LocationRepository = container.locationRepository

/** The app's real calculation-settings storage on the test device. */
fun testCalculationSettingsRepository(): CalculationSettingsRepository = container.calculationSettingsRepository

/**
 * Removes the saved location and resets calculation settings before (and after) a test,
 * so every test starts the same way.
 */
class ResetAppSettingsRule : ExternalResource() {
    override fun before() = reset()
    override fun after() = reset()

    private fun reset() = runBlocking {
        testLocationRepository().clear()
        testCalculationSettingsRepository().resetToDefaults()
    }
}
