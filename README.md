# Universal Azan & Qibla

An offline, privacy-friendly Android app for prayer times, Azan alarms and a Qibla compass.
No accounts, no ads, no analytics, no internet needed for normal use.

**Current status:** version 1.0.0, all 13 stages complete (see [docs/release.md](docs/release.md)
for the release build, signing and installing on a phone). The user can choose a location (city search, typed coordinates or
GPS), and prayer times are calculated on the device for that location's own date and time zone.
Calculation method, Asr (Standard or Hanafi) and the high-latitude rule can be changed in
Settings. The Qibla direction is calculated offline from the saved location, with a live compass
on phones that have a magnetic sensor. Each prayer can be set to the full Azan, a short Azan or
silent, and the Azan plays automatically at prayer times (also with the screen locked). Alarms
update themselves after a new day, restart, clock or time-zone change and setting changes, without
re-setting alarms that are still correct. Settings include per-prayer minute adjustments, 12/24-hour
time, light/dark theme, and an About and privacy page.

## Project facts

| Item | Value |
|---|---|
| Package name | `com.universalazan.app` (change `namespace` + `applicationId` in `app/build.gradle.kts`) |
| App name | `app/src/main/res/values/strings.xml` → `app_name` |
| Minimum Android | 8.0 (API 26) |
| Target / compile SDK | 37 |
| Language / UI | Kotlin, Jetpack Compose (Material 3) |
| Build tools | Android Gradle Plugin 9.4.1, Gradle 9.6.0, Kotlin 2.4.21 |
| Library versions | `gradle/libs.versions.toml` |
| Version | 1.0.0 (`versionName`), `versionCode` 1, in `app/build.gradle.kts` |
| Permissions | See [Permissions](#permissions) below; no internet |

## This PC's setup (nothing on C:)

- Android Studio: `G:\android_studio_installation_folder`
- Android SDK: `G:\android_sdk_location` (recorded in `local.properties`, not committed)
- Java: Android Studio's built-in JDK, `G:\android_studio_installation_folder\jbr`
- Gradle downloads/cache: `G:\gradle_user_home` (Windows user environment variable `GRADLE_USER_HOME`)
- Gradle temp files: `G:\gradle_tmp`
- Emulators: `G:\android_avd` (Windows user environment variable `ANDROID_AVD_HOME`)

## Code layout

```
app/src/main/java/com/universalazan/app/
  MainActivity.kt          starts the app
  UniversalAzanApp.kt      creates shared objects (AppContainer)
  domain/location/         coordinate checking and time zone helpers (pure Kotlin)
  domain/prayer/           prayer-time calculation with the Adhan library (pure Kotlin)
  domain/qibla/            Qibla bearing and distance to the Kaaba (pure Kotlin)
  sensors/                 compass sensors, heading maths, magnetic-to-true-north correction
  audio/                   Azan sound choices and the shared Azan player
  alarms/                  prayer alarm planning, scheduling, receivers and playback service
  data/locations/          saved-location model, offline city list and search, GPS reader
  data/preferences/        saves settings on the device (DataStore)
  ui/navigation/           the three sections and the bottom bar / side rail
  ui/location/             Location screen and time zone picker
  ui/prayer/               Prayer Times screen and its states (loading, error, ready)
  ui/qibla/                Qibla screen, bearing and live compass dial
  ui/settings/             Settings, Prayer calculation (incl. minute adjustments), Azan sounds,
                           Prayer alarms, About and privacy
  ui/common/TimeFormat.kt  12/24-hour formatting used on every screen
  ui/common/               shared screen layout
  ui/theme/                colours (green and gold, light and dark)
```

**App icon:** made from `logo/gemini-svg.svg`. After changing the logo, run
`G:\Python312\python.exe -I tools\build_launcher_icon.py`, which rewrites the three icon layers in
`app/src/main/res/drawable/` (`ic_launcher_background`, `ic_launcher_foreground`, and
`ic_launcher_monochrome` for Android 13+ themed icons).

## Build and run

### Easiest: Android Studio
1. **File → Open** → choose `F:\muslim app`.
2. Wait for "Gradle sync finished" at the bottom.
3. Pick an emulator or your phone in the device dropdown at the top, then press the green **Run ▶** button.

### Command line (PowerShell, in `F:\muslim app`)

Set up the terminal once per window:

```powershell
$env:JAVA_HOME = 'G:\android_studio_installation_folder\jbr'
$env:GRADLE_USER_HOME = 'G:\gradle_user_home'
$env:TEMP = 'G:\gradle_tmp'; $env:TMP = 'G:\gradle_tmp'
```

Then:

```powershell
.\gradlew.bat assembleDebug               # build the debug APK
.\gradlew.bat testDebugUnitTest           # run unit tests on the PC
.\gradlew.bat connectedDebugAndroidTest   # run tests on a running emulator/phone
.\gradlew.bat installDebug                # install on a running emulator/phone
```

The debug APK is created at `app\build\outputs\apk\debug\app-debug.apk`.

Release build (smaller, optimised, signed with the key on F:): `.\gradlew.bat assembleRelease`
→ `app\build\outputs\apk\release\app-release.apk`. Installing on a phone, the signing key and
backing it up are explained in [docs/release.md](docs/release.md).

## Permissions

| Permission | Why |
|---|---|
| `ACCESS_FINE_LOCATION` | Only requested when you tap **Use current location**. Precise location is needed for GPS, the only location source that works without internet. |
| `ACCESS_COARSE_LOCATION` | Requested together with the one above. Android lets you choose "approximate" instead; the app then tries Android's other location sources. |
| `USE_EXACT_ALARM` (Android 13+) | Starts the Azan at the exact minute. Granted automatically at install, because playing the Azan at prayer times is this app's core alarm function. Without an exact alarm, Android would also refuse to start the Azan in the background. |
| `SCHEDULE_EXACT_ALARM` (Android 12 only) | The same for Android 12, where it is on by default. If you turn it off, the app warns you (Settings → Prayer alarms → Allow exact alarms). Android 8–11 need no permission. |
| `POST_NOTIFICATIONS` | Shows the "Azan playing" notification with a Stop button. Asked for in the app. |
| `RECEIVE_BOOT_COMPLETED` | Schedules the next Azans again after the phone restarts. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keeps the Azan playing while the app is closed or the screen is locked. |
| `WAKE_LOCK` | Keeps playback going with the screen off. |
| `com.universalazan.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Added automatically by the AndroidX Core library. Private to this app; gives no access to any data or hardware. |

There is **no internet permission** and **no background location**. Refusing location
permission does not stop the app working: you can search for a city or type coordinates.

Your location is never sent anywhere. Backup is turned off (`allowBackup="false"` plus explicit
backup and data-transfer rules), so saved settings are not uploaded to cloud backup or copied
to a new phone.

## Locations and time zones

- **City search:** about 34,000 cities with more than 15,000 people, bundled in
  `app/src/main/assets/cities.tsv`. Each city includes its time zone. Smaller places must be
  entered as coordinates. Common English spellings are included for larger cities (for example,
  "Mecca" finds Makkah and "Medina" finds Madinah).
- **Typed coordinates:** decimal degrees, south and west negative. You must confirm a time zone;
  the phone's time zone is offered first.
- **GPS:** uses the phone's time zone, shown clearly with a button to change it, because the app
  cannot work out a time zone from coordinates offline.

### City data source and licence

City data is from [GeoNames](https://www.geonames.org), licensed under
[Creative Commons Attribution 4.0](https://creativecommons.org/licenses/by/4.0/).
The app shows this credit on the Location screen. To rebuild the file:

1. Download `cities15000.zip` and `admin1CodesASCII.txt` from https://download.geonames.org/export/dump/
   into a folder (for example `F:\tmp\geonames`) and unzip `cities15000.zip` there.
2. From `F:\muslim app`, run: `G:\Python312\python.exe tools\build_cities.py F:\tmp\geonames`

## Prayer times

- Calculated on the phone with the [Adhan library](https://github.com/batoulapps/adhan-kotlin)
  (`com.batoulapps.adhan:adhan2`, MIT licence). No internet is used.
- Default method: Muslim World League, Standard Asr, automatic high-latitude rule. No single method
  is right for every community; users should choose the one their local mosque uses.
- "Today" is the date at the selected location. Times are converted with the location's time zone,
  so daylight saving is handled by Android's time-zone rules.
- Where the sun does not rise or set on a date (polar day or night), the app explains that times
  cannot be calculated.
- How the results were checked against official and independent timetables, and every difference
  found, is in [docs/prayer-time-validation.md](docs/prayer-time-validation.md).

## Qibla direction

- Calculated on the phone from the saved coordinates: no GPS or internet needed.
- Shown in degrees clockwise from **true** north, with a compass point (e.g. 119.0° ESE) and the
  distance to the Kaaba.
- Checked against JAKIM's official bearing and independent calculations; see
  [docs/qibla-validation.md](docs/qibla-validation.md).
- Live compass: uses the phone's sensors (no permission needed), corrected from magnetic to true
  north, with calibration and interference warnings. Phones without a magnetic sensor get an
  explanation instead of a compass. How it works, what was tested, and how to test on a real
  phone: [docs/compass-testing.md](docs/compass-testing.md).

## Azan audio

- Recordings are bundled in the app and played offline at the alarm volume.
- **Personal recordings are never pushed to GitHub.** They live in `app/src/personalAudio/`
  (ignored by Git). Without that folder, the app is built with clearly labelled test tones from
  `app/src/testAudio/`.
- The full Fajr Azan is only used for Fajr; the other prayers use a version without
  "aṣ-ṣalātu khayrun mina n-nawm".
- Details, file sources and how to replace the recordings: [docs/azan-audio.md](docs/azan-audio.md).

## Prayer alarms

- One alarm per prayer, always for its next time, re-planned when settings change, after a restart,
  clock or time-zone change. Missed Azans (e.g. phone was off) are never played late.
- Alarms that are still correct are not re-set; a once-a-day check (about 03:30) restores anything
  that went missing.
- Plays with the screen locked, with a notification and Stop button.
- Settings → Prayer alarms shows which phone settings need attention and has a
  "Test alarm in 1 minute" button.
- How it works, what was tested and its limits: [docs/alarms.md](docs/alarms.md).

## Settings

| Group | Settings |
|---|---|
| Location and prayer times | Location (city, coordinates, GPS, time zone); calculation method, Asr, high-latitude rule, **minute adjustments per prayer (±60 min)**, reset |
| Azan | Full / short / silent per prayer with test playback; Prayer alarms on/off, permission status and test alarm |
| General | Time format (phone setting, 12-hour, 24-hour); theme (phone setting, light, dark) |
| About | Version, privacy statement, permissions explained, data sources and licences, limitations |

All settings are saved on the phone (DataStore) and survive restarts.

## Quality checks

Accessibility (automated checks on every screen), large text, small and large screens, rotation,
error messages and alarm-restriction tips were reviewed in Stage 10: [docs/quality-review.md](docs/quality-review.md).

Offline operation (20 scenarios with airplane mode on, plus the full automated suite offline) was
verified in Stage 11: [docs/offline-test-plan.md](docs/offline-test-plan.md). The app has no internet
permission and no networking, analytics or advertising libraries.

## Internet use

Normal use of the app needs no internet. Internet is only needed to install the app, to download
future updates, and (on the developer's PC) for Gradle to download libraries when building.
