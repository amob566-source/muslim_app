# Offline test plan and results (Stage 11)

Run on 13 October 2026 on the Android Studio emulator (Android 37, "Medium Phone"), with
**airplane mode ON for the whole session**, starting from a fresh install.

**How each scenario was tested**

- **Automated:** covered by unit tests (run on the PC) or device tests (run on the emulator) that pass.
- **Emulator (manual):** done step by step on the emulator, with the evidence recorded below.
- **Not tested – needs real phone:** cannot be checked properly on the emulator.

A scenario is only marked as passed when it was actually run; compiling is not counted as testing.

## Network access

| Check | Result |
|---|---|
| Final merged manifest (app + all libraries) | **No `INTERNET` permission.** Only: location (fine/coarse), notifications, exact alarms, boot completed, foreground service (+ media playback), wake lock, and AndroidX's private receiver permission. |
| Libraries inside the app | AndroidX/Compose, DataStore, Lifecycle, Adhan (prayer times), Kotlin. **No networking, analytics, crash-reporting or advertising libraries** (searched 774 dependency lines). |
| On the device | `dumpsys package` shows no `INTERNET` permission for the app, so Android blocks it from opening network connections. |
| Whole test suite with airplane mode on | See "Automated tests in airplane mode" below. |

## Results

| # | Scenario | How tested | Result | Evidence |
|---|---|---|---|---|
| 1 | Fresh install, airplane mode on | Emulator (manual) | **Pass** | App opened in 1.8 s; "No location selected", `--:--` for every time (no invented times). |
| 2 | Manual coordinates, no internet | Emulator (manual) + automated | **Pass** | Typed 21.4225, 39.8262, chose Asia/Riyadh in the time-zone picker; times shown. `LocationFlowTest`. |
| 3 | Offline city | Emulator (manual) + automated | **Pass** | "Jakarta" found as Jakarta, Indonesia · Asia/Jakarta. `CityDatasetTest` checks all 34,160 cities. |
| 4 | Prayer times for today | Emulator (manual) + automated | **Pass** | Makkah, 13 Oct: Fajr 5:02, Dhuhr 12:08, Asr 3:28 (marked next), Maghrib 5:58, Isha 7:08. `ReferenceTimetableTest` compares with official/independent timetables. |
| 5 | Date change and recalculation | Emulator (manual) + automated | **Pass** | Clock set to 17:59 London: at 18:00 London (= midnight Jakarta) the screen changed to Wednesday 14 Oct. The day follows the *city*, not the phone. `PrayerTimesStateTest`. |
| 6 | Switching locations | Emulator (manual) + automated | **Pass** | Makkah → Jakarta: all six times and the time zone changed. `PrayerTimesFlowTest`. |
| 7 | Qibla bearing | Emulator (manual) + automated | **Pass** | Jakarta 295.2° (reference 295.15°), 7,920 km. `QiblaCalculatorTest` (JAKIM + 16 reference places + 1,000 grid points). |
| 8 | Live compass movement | Emulator (simulated sensors) | **Pass on emulator** | Magnetic north → "0° (N), Turn left 65°"; east → "90° (E), Turn left 155°". **Real phone: not tested.** |
| 9 | Device without compass sensors | Automated (simulated) | **Pass (simulated)** | `QiblaCompassScreenTest` shows "No compass sensor" and the bearing, no fake compass. The emulator cannot remove its sensors. **Real phone without a magnetometer: not tested.** |
| 10 | Full Azan playback | Emulator (manual) + automated | **Pass** | Fajr full Azan played offline on the alarm stream, 0 underruns; Stop ended it. `AzanPlayerTest`. |
| 11 | Short Azan playback | Emulator (manual) + automated | **Pass** | Short clip (48 kHz) played and finished by itself after ~21 s. |
| 12 | Silent prayer | Emulator (manual) + automated | **Pass** | Asr set to Silent: Test button greyed out, tapping it played nothing, Asr alarm removed. `PrayerAlarmTest`. |
| 13 | Screen locked during an alarm | Emulator (manual) + automated | **Pass on emulator** | Screen asleep; test alarm fired; Android allowed playback ("ALARM_MANAGER_WHILE_IDLE"); Azan played; service stopped itself at the end. **Real phone: not tested.** |
| 14 | App process killed | Emulator (manual) + automated | **Pass** | After `am kill`: alarms still registered; reopening restored the same screen and location. |
| 15 | Restart and alarm recovery | Emulator (manual) | **Pass** | Real restart in airplane mode; without opening the app, 3 prayer alarms (all exact) + daily check were back. |
| 16 | Time-zone and daylight-saving changes | Emulator (manual) + automated | **Pass** | Phone London → New York: the three alarm moments were identical (they follow the city). DST: `ReferenceTimetableTest`, `PrayerAlarmPlannerTest` (London clock changes). |
| 17 | Location permission denied | Emulator (manual) + automated | **Pass** | "Location permission was not given. You can still search for a city or enter coordinates"; manual entry still works. |
| 18 | Notification / exact-alarm permission revoked | Emulator (manual) | **Pass** | Notifications off: warning shown; test Azan still played (no notification). Exact alarms denied: Android stopped the app and removed all alarms; on reopening the app re-scheduled them (inexact) and showed the warning with a fix button; re-allowing made them exact again. |
| 19 | Incorrect coordinates | Emulator (manual) + automated | **Pass** | Latitude 95 → "Latitude must be between -90 and 90"; "abc" → "Enter a decimal number…"; nothing saved. `CoordinatesTest`. |
| 20 | Storage and battery | Emulator (measured) | **Pass (measured)** | App data on phone: 56 KB. Debug APK: 18.7 MB (release build will be smaller, Stage 12). Wake-ups: at most 5 prayer alarms + 1 daily check per day. App closed: 0 running services, 0 wake locks, 0 sensor listeners. Compass sensors registered only while the Qibla screen is open (sensor log: on at 12:58:29, off at 12:58:38). **Real battery drain over a day: not measured.** |

## Automated tests in airplane mode

The complete automated suite was run with **airplane mode on**:

| Suite | Result |
|---|---|
| Unit tests (on the PC) | **128 passed, 0 failed** |
| Device tests (on the emulator, airplane mode on) | **75 passed, 0 failed** (after the fixes below) |
| Lint | No issues |

## Not tested – needs a real phone

- Live compass accuracy against a known direction (e.g. a mosque).
- Locked-screen Azan, Do Not Disturb and silent mode on the actual phone.
- The phone maker's battery saver (Samsung, Xiaomi, etc.).
- A phone without a magnetometer.
- Real battery use over a full day.
- Android 8–11 (the emulator runs Android 37).

## Issues found during Stage 11

- **Compass redrew about 15 times a second for invisible sensor noise.** The combined rotation sensor
  reports tiny changes even when the phone is still, and each was treated as a new heading. The
  heading is now rounded to 0.5° (finer than the display or any phone compass), so the dial only
  redraws when the direction really changes. Found because a Qibla device test took 12.7 minutes
  (the test framework waits for the screen to settle); it now takes 7 seconds. Also saves battery
  while the Qibla screen is open. New unit test: `liveHeadingIsRoundedToHalfDegreesSoNoiseDoesNotRedraw`.
- **Intermittent device-test failures on a busy emulator** ("Failed to inject touch input",
  bottom-bar tab not found). Cause: on start-up and after rotation the app reads the saved theme
  before drawing (Stage 9, to avoid a flash of the wrong colours); tests could tap before the screen
  existed. Not an app fault (the delay is milliseconds), but the tests now wait for the app to be
  drawn (`AppReadyRule`, `waitForAppReady()`). Checked by repeated runs.
- The Test and Stop buttons on Azan sounds, and the minute-adjustment buttons, had their spoken
  label attached outside the button. It now sits on the text inside the button, so it merges into
  the button itself (recommended practice). The "enabled" state reported by the `uiautomator` dump
  tool turned out to be unreliable for Compose buttons in general, so this was not a confirmed
  screen-reader problem; it is a safe improvement.
- No app defects were found in the 20 scenarios.
