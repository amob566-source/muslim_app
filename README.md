# Universal Azan & Qibla

An offline, privacy-friendly Android app for prayer times, Azan alarms and a Qibla compass.
No accounts, no ads, no analytics, no internet needed for normal use.

**Current status:** Stage 2. Screens and navigation are in place, and the user can choose a
location (city search, typed coordinates or GPS). The choice is saved on the device. No prayer
times are calculated yet, and the screen says so instead of showing invented times. Qibla, Azan
and alarms are not implemented yet.

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
| Permissions | None declared by the app (see below) |

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
  data/locations/          saved-location model, offline city list and search, GPS reader
  data/preferences/        saves settings on the device (DataStore)
  ui/navigation/           the three sections and the bottom bar / side rail
  ui/location/             Location screen and time zone picker
  ui/prayer/               Prayer Times screen and its states (loading, error, ready)
  ui/qibla/                Qibla screen
  ui/settings/             Settings screen
  ui/common/               shared screen layout
  ui/theme/                colours (green and gold, light and dark)
```

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

## Permissions

| Permission | Why |
|---|---|
| `ACCESS_FINE_LOCATION` | Only requested when you tap **Use current location**. Precise location is needed for GPS, the only location source that works without internet. |
| `ACCESS_COARSE_LOCATION` | Requested together with the one above. Android lets you choose "approximate" instead; the app then tries Android's other location sources. |
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

## Internet use

Normal use of the app needs no internet. Internet is only needed to install the app, to download
future updates, and (on the developer's PC) for Gradle to download libraries when building.
