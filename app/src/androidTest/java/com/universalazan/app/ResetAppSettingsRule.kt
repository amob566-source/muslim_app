package com.universalazan.app

import androidx.test.platform.app.InstrumentationRegistry
import com.universalazan.app.audio.AzanPlayer
import com.universalazan.app.data.preferences.AzanSoundRepository
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

/** The app's real Azan sound settings on the test device. */
fun testAzanSoundRepository(): AzanSoundRepository = container.azanSoundRepository

/** The app's shared Azan player. */
fun testAzanPlayer(): AzanPlayer = container.azanPlayer

/**
 * Removes the saved location, resets calculation and sound settings and stops any sound,
 * before (and after) a test, so every test starts the same way.
 */
class ResetAppSettingsRule : ExternalResource() {
    override fun before() = reset()
    override fun after() = reset()

    private fun reset() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { testAzanPlayer().stop() }
        runBlocking {
            testLocationRepository().clear()
            testCalculationSettingsRepository().resetToDefaults()
            testAzanSoundRepository().resetToDefaults()
        }
    }
}
