package com.universalazan.app

import android.app.Application
import android.content.Context
import com.universalazan.app.data.locations.CityRepository
import com.universalazan.app.data.locations.DeviceLocationProvider
import com.universalazan.app.data.preferences.CalculationSettingsRepository
import com.universalazan.app.data.preferences.LocationRepository
import com.universalazan.app.data.preferences.settingsDataStore

class UniversalAzanApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Creates the app's shared objects once. Screens get them from here. */
class AppContainer(context: Context) {
    val locationRepository = LocationRepository(context.settingsDataStore)
    val calculationSettingsRepository = CalculationSettingsRepository(context.settingsDataStore)
    val cityRepository = CityRepository(context.assets)
    val deviceLocationProvider = DeviceLocationProvider(context)
}
