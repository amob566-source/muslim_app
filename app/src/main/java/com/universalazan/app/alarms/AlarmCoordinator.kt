package com.universalazan.app.alarms

import com.universalazan.app.audio.AzanSoundSettings
import com.universalazan.app.data.preferences.AlarmSettingsRepository
import com.universalazan.app.data.preferences.AzanSoundRepository
import com.universalazan.app.data.preferences.CalculationSettingsRepository
import com.universalazan.app.data.preferences.LocationRepository
import com.universalazan.app.domain.prayer.CalculationSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Keeps the scheduled alarms in line with the saved settings. Re-plans whenever the location,
 * calculation settings, Azan sounds or the on/off switch change, and when asked to (app start,
 * an alarm firing, reboot, clock or time-zone change).
 */
class AlarmCoordinator(
    private val locationRepository: LocationRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val azanSoundRepository: AzanSoundRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val scheduler: PrayerAlarmScheduler,
    private val clock: () -> Instant = Instant::now,
) {
    private val mutex = Mutex()
    private val _upcoming = MutableStateFlow<List<PlannedAlarm>>(emptyList())

    /** The alarms currently scheduled, soonest first. */
    val upcoming: StateFlow<List<PlannedAlarm>> = _upcoming.asStateFlow()

    private val playedEvents = ArrayDeque<String>()

    /** Plans and schedules alarms from the current settings. Safe to call repeatedly. */
    suspend fun reschedule(): List<PlannedAlarm> = mutex.withLock {
        val enabled = alarmSettingsRepository.enabled.first()
        val planned = if (enabled) {
            PrayerAlarmPlanner.plan(
                location = locationRepository.savedLocation.first(),
                calculation = calculationSettingsRepository.settings.first(),
                sounds = azanSoundRepository.settings.first(),
                now = clock(),
            )
        } else {
            emptyList()
        }
        scheduler.apply(planned)
        _upcoming.value = planned
        planned
    }

    /** Re-plans automatically whenever a setting that affects alarms changes. */
    fun startObserving(scope: CoroutineScope) {
        scope.launch {
            combine(
                locationRepository.savedLocation,
                calculationSettingsRepository.settings.catch { emit(CalculationSettings()) },
                azanSoundRepository.settings.catch { emit(AzanSoundSettings()) },
                alarmSettingsRepository.enabled.catch { emit(true) },
            ) { location, calculation, sounds, enabled -> listOf(location, calculation, sounds, enabled) }
                .distinctUntilChanged()
                .catch { /* Settings unreadable: keep existing alarms. */ }
                .collectLatest { reschedule() }
        }
    }

    /**
     * Records that the Azan for [eventId] is being played. Returns false if it was already
     * played (e.g. the clock was moved back), so the same Azan is never played twice.
     */
    @Synchronized
    fun markPlayed(eventId: String): Boolean {
        if (eventId in playedEvents) return false
        playedEvents.addLast(eventId)
        while (playedEvents.size > 20) playedEvents.removeFirst()
        return true
    }
}
