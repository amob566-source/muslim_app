# Universal Azan & Qibla

An offline, privacy-friendly Android app for prayer times, Azan alarms and a Qibla compass.
No accounts, no ads, no analytics, no internet needed for normal use.

**Current status:** Stage 1. The basic interface is in place: Prayer Times, Qibla and Settings
screens with navigation between them. No prayer times are calculated yet, and the screen says so
instead of showing invented times. Location, Qibla, Azan and alarms are not implemented yet.

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
  ui/navigation/           the three sections and the bottom bar / side rail
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

The app's own manifest declares **no permissions**, not even internet.
The final merged manifest contains one entry added automatically by the AndroidX Core library:
`com.universalazan.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. It is private to this app.
It only stops other apps from sending messages to it, and gives no access to any data or hardware.

Auto-backup is turned off (`android:allowBackup="false"`), so saved settings, which will include
your location, are never uploaded to cloud backup.

## Internet use

Normal use of the app needs no internet. Internet is only needed to install the app, to download
future updates, and (on the developer's PC) for Gradle to download libraries when building.
