package com.universalazan.app.ui.settings

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.universalazan.app.UniversalAzanApp
import com.universalazan.app.alarms.AlarmCoordinator
import com.universalazan.app.alarms.PlannedAlarm
import com.universalazan.app.alarms.PrayerAlarmScheduler
import com.universalazan.app.audio.AzanPlayer
import com.universalazan.app.data.locations.SavedLocation
import com.universalazan.app.data.preferences.AlarmSettingsRepository
import com.universalazan.app.data.preferences.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** Phone settings that decide whether an Azan can actually be heard. */
data class AlarmSystemStatus(
    val notificationsAllowed: Boolean = true,
    val exactAlarmsAllowed: Boolean = true,
    val batteryUnrestricted: Boolean = true,
    val alarmVolumeMuted: Boolean = false,
)

class AlarmsViewModel(
    private val application: Application,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    locationRepository: LocationRepository,
    private val coordinator: AlarmCoordinator,
    private val scheduler: PrayerAlarmScheduler,
    private val player: AzanPlayer,
) : ViewModel() {

    val enabled: StateFlow<Boolean> = alarmSettingsRepository.enabled
        .catch { emit(true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val location: StateFlow<SavedLocation?> = locationRepository.savedLocation
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val upcoming: StateFlow<List<PlannedAlarm>> = coordinator.upcoming

    private val _status = MutableStateFlow(AlarmSystemStatus())
    val status: StateFlow<AlarmSystemStatus> = _status.asStateFlow()

    private val _testAlarmAt = MutableStateFlow<Instant?>(null)

    /** When the last test alarm was set for, if any. */
    val testAlarmAt: StateFlow<Instant?> = _testAlarmAt.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads permissions and settings (call when the screen comes back into view). */
    fun refresh() {
        _status.value = AlarmSystemStatus(
            notificationsAllowed = notificationsAllowed(),
            exactAlarmsAllowed = scheduler.canScheduleExactAlarms(),
            batteryUnrestricted = application.getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(application.packageName) == true,
            alarmVolumeMuted = player.isAlarmVolumeMuted(),
        )
        // A permission may have just been granted: re-plan so exact alarms are used.
        viewModelScope.launch { coordinator.reschedule() }
    }

    fun setEnabled(value: Boolean) {
        viewModelScope.launch { alarmSettingsRepository.setEnabled(value) }
    }

    fun scheduleTestAlarm() {
        val now = System.currentTimeMillis()
        scheduler.scheduleTest(TEST_DELAY_MILLIS, now)
        _testAlarmAt.value = Instant.ofEpochMilli(now + TEST_DELAY_MILLIS)
    }

    private fun notificationsAllowed(): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(application, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(application).areNotificationsEnabled()
    }

    companion object {
        const val TEST_DELAY_MILLIS = 60_000L

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as UniversalAzanApp
                val container = app.container
                AlarmsViewModel(
                    app,
                    container.alarmSettingsRepository,
                    container.locationRepository,
                    container.alarmCoordinator,
                    container.alarmScheduler,
                    container.azanPlayer,
                )
            }
        }
    }
}
