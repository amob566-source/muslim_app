package com.universalazan.app

import android.app.Application
import android.content.Context
import com.universalazan.app.alarms.AlarmCoordinator
import com.universalazan.app.alarms.AzanPlaybackService
import com.universalazan.app.alarms.PrayerAlarmScheduler
import com.universalazan.app.audio.AzanPlayer
import com.universalazan.app.data.locations.CityRepository
import com.universalazan.app.data.locations.DeviceLocationProvider
import com.universalazan.app.data.preferences.AlarmSettingsRepository
import com.universalazan.app.data.preferences.AzanSoundRepository
import com.universalazan.app.data.preferences.CalculationSettingsRepository
import com.universalazan.app.data.preferences.GeneralSettingsRepository
import com.universalazan.app.data.preferences.LocationRepository
import com.universalazan.app.data.preferences.settingsDataStore
import com.universalazan.app.sensors.CompassSensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class UniversalAzanApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        AzanPlaybackService.createChannel(this)
        // Keep alarms in line with settings for as long as the app process runs.
        container.alarmCoordinator.startObserving(container.appScope)
    }
}

/** Creates the app's shared objects once. Screens get them from here. */
class AppContainer(context: Context) {
    /** Lives as long as the app process; used for background work such as re-planning alarms. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val locationRepository = LocationRepository(context.settingsDataStore)
    val calculationSettingsRepository = CalculationSettingsRepository(context.settingsDataStore)
    val cityRepository = CityRepository(context.assets)
    val deviceLocationProvider = DeviceLocationProvider(context)
    val compassSensor = CompassSensor(context)
    val azanSoundRepository = AzanSoundRepository(context.settingsDataStore)
    val azanPlayer = AzanPlayer(context.applicationContext)
    val alarmSettingsRepository = AlarmSettingsRepository(context.settingsDataStore)
    val generalSettingsRepository = GeneralSettingsRepository(context.settingsDataStore)
    val alarmScheduler = PrayerAlarmScheduler(context.applicationContext)
    val alarmCoordinator = AlarmCoordinator(
        locationRepository,
        calculationSettingsRepository,
        azanSoundRepository,
        alarmSettingsRepository,
        alarmScheduler,
    )
}
