# Release build, signing and installing (Stage 12)

Measured on 13 October 2026, version 1.0.0 (`versionCode` 1).

## The two APK files

| Build | File | Size | Use |
|---|---|---|---|
| Debug | `app\build\outputs\apk\debug\app-debug.apk` | 19,006,058 bytes (18.1 MB) | Testing from Android Studio. Not optimised. |
| Release | `app\build\outputs\apk\release\app-release.apk` | 7,266,511 bytes (6.9 MB) | **The one to install on your phone.** Optimised and signed with your key. |

What makes up the release APK (approximate, compressed):

| Part | Size | Notes |
|---|---|---|
| Azan recordings (full, Fajr, short) | 4.3 MB | Already compressed (AAC), stored as-is. Personal copies; not in Git. |
| Program code (`classes.dex`) | 1.4 MB | After R8 removed unused code from the app and every library. |
| City list (`cities.tsv`) | 1.0 MB | 34,160 cities (GeoNames, CC BY 4.0), needed for offline search. |
| Everything else | 0.5 MB | Resources, icons, small native helpers from AndroidX. |

Built from a fresh clone **without** the personal recordings, the app uses the short test tones
instead; that APK was not built in this stage, but it should be about 3 MB (7.3 MB minus the 4.3 MB of recordings plus 0.2 MB of test tones).

## Optimisation (R8)

The release build uses R8 (`optimization { enable = true }` in `app/build.gradle.kts`), which
removes unused code and resources and shortens names. Settings checked in this stage:

- **Scope widened** from AndroidX/Kotlin only to also include the app's own code and the Adhan
  library: `classes.dex` went from 3.6 MB to 2.9 MB uncompressed (APK 7.48 → 7.27 MB).
- **Saved settings stay readable after updates.** Settings (calculation method, theme, Azan
  sound…) and scheduled alarms store enum names such as `MuslimWorldLeague` or `Fajr`. A keep rule in
  `app/src/main/keepRules/rules.keep` stops R8 renaming them; checked in
  `app/build/outputs/mapping/release/mapping.txt` (`Prayer Fajr -> Fajr`).
- **Start-up crash found and fixed.** The first release build crashed immediately
  (`IllegalAccessError` in `kotlin.sequences`): R8 moved one Kotlin class out of its package while its
  parent class stayed behind. This also happened with the project template's original settings;
  the release build had simply never been run before. Fixed with `-keeppackagenames kotlin.**`.
- Unused resources are removed automatically. All libraries are needed (checked one by one:
  AndroidX core/activity/Compose/Material 3/Lifecycle, DataStore, Adhan). `ui-tooling` and
  `ui-test-manifest` are only in debug builds.
- `mapping.txt` (in `app\build\outputs\mapping\release\`) translates crash reports from the release
  build back to real names. Keep a copy with each APK you install if you want readable crash logs.

## Signing key

Android only installs an **update** over an existing app if both are signed with the same key. So
there is **one key for the whole life of the app**. Do not create a new one for later versions.

| What | Where |
|---|---|
| Key file | `F:\UniversalAzan-keys\universal-azan-release.jks` (PKCS12, RSA 4096, valid until 2054) |
| Password and key name | `F:\UniversalAzan-keys\signing.properties` |
| Pointer used by the build | `G:\gradle_user_home\gradle.properties`, line `universalAzan.signingProperties=F:/UniversalAzan-keys/signing.properties` |
| Certificate fingerprint (SHA-256) | `A6:5B:FA:5C:DD:18:EE:40:AB:51:22:4E:2D:16:CE:F7:1E:D2:51:D3:8F:A2:FB:99:9C:C3:AE:69:6A:F5:E4:1D` |

None of these are inside the project folder or on GitHub. `app/build.gradle.kts` only reads the
pointer; if the pointer or file is missing (for example on another PC), the release APK is
simply built unsigned (`app-release-unsigned.apk`) and the build still works.

### Back up the key (do this now)

1. Copy the whole folder `F:\UniversalAzan-keys` to **two** other places that are not this PC,
   for example a USB stick kept at home and a private, encrypted cloud folder or password manager.
2. Do not put it on GitHub, email it, or share it.
3. If the key or password is lost, you can still build the app, but your phone will refuse to
   install it as an update: you would have to uninstall the old app (losing its saved settings) and
   install the new one.

### Building a signed release

In Android Studio: **Build → Generate App Bundles or APKs → Generate APKs** with the `release`
variant selected (Build → Select Build Variant), or from PowerShell in `F:\muslim app`:

```powershell
$env:JAVA_HOME = 'G:\android_studio_installation_folder\jbr'
$env:GRADLE_USER_HOME = 'G:\gradle_user_home'
$env:TEMP = 'G:\gradle_tmp'; $env:TMP = 'G:\gradle_tmp'
.\gradlew.bat assembleRelease
```

The APK is `app\build\outputs\apk\release\app-release.apk`.

For each new version, increase `versionCode` by 1 (and update `versionName`, e.g. `1.0.1`) in
`app/build.gradle.kts`; Android will not install an update with the same or a lower `versionCode`.

## Installing on your phone

**Option A: USB cable (recommended for testing)**

1. On the phone: Settings → About phone → tap **Build number** 7 times (turns on Developer options).
2. Settings → System → Developer options → turn on **USB debugging**.
3. Connect the phone by USB and tap **Allow** on the "Allow USB debugging?" question.
4. In PowerShell:
   ```powershell
   G:\android_sdk_location\platform-tools\adb.exe install -r "F:\muslim app\app\build\outputs\apk\release\app-release.apk"
   ```
   (Or choose the phone in Android Studio's device list and press **Run ▶**, which installs the
   debug build instead.)

**Option B: copy the file**

1. Copy `app-release.apk` to the phone (USB file transfer, or a cable to its Downloads folder).
2. Open it in the phone's **Files** app. Android asks to allow **Install unknown apps** for the Files
   app; allow it, go back, and tap **Install**.
3. You can turn "Install unknown apps" off again afterwards.

**Debug and release cannot replace each other.** They are signed with different keys. If the
debug version is on the phone, uninstall it first (this clears its settings) and then install the
release APK. From then on, install only release APKs to keep your settings.

**After installing**, open the app, choose your location, then check Settings → Prayer alarms:
it lists anything Android or the phone maker is blocking (notifications, battery saver) with a button
to fix it. Then use Azan sounds → **Test** to hear the alarm volume.

## Release smoke test (emulator, Android 37)

The signed release APK was installed on the emulator from scratch, with no permissions granted by
hand, and checked:

| Check | Result |
|---|---|
| Signature | Verified with `apksigner` (APK Signature Scheme v2), certificate matches the key above |
| Start-up | First build: **crash** (fixed, see Optimisation). After the fix: opens, no crashes in the log |
| Start time (cold start, `am start -W`, after the fix) | 1.3 s; 2.0–2.3 s on the very first start after installing (debug build in Stage 11: 1.8 s) |
| City search and prayer times | Jakarta found; Fajr 4:22 AM … Isha 6:52 PM (same as the debug build) |
| Qibla | 295.2° WNW, 7,920 km, live compass running (same as the debug build) |
| Azan test button | Played on the alarm stream; Stop ended it |
| Alarms | 5 prayer alarms + daily check scheduled |
| Saved location after closing the app | Kept |
| **Real prayer alarm with the app closed** | First attempt: **alarm fired but no sound** (see below). After the fix: Isha alarm fired at 12:52:00.012, Azan played in the background with its notification |

### Bug found: silent Azan on a fresh install (Android 13+)

Android 13 and newer do not give a newly installed app the `SCHEDULE_EXACT_ALARM` permission. The
app then fell back to inexact alarms, and when such an alarm fired Android refused to start the
Azan player from the background (`ForegroundServiceStartNotAllowedException`), so **nothing played**
until the user found and turned on "Allow exact alarms" in Settings → Prayer alarms. Earlier tests
missed this because the automated tests switch that permission on before running.

Fix: the app now declares `USE_EXACT_ALARM`, which Android grants at install to apps whose core
function is alarms (an Azan app is one). `SCHEDULE_EXACT_ALARM` remains only for Android 12
(where it is on by default). Verified on a fresh install as shown above.

Note for a future Google Play release: Google Play reviews apps that declare `USE_EXACT_ALARM`
and only allows it for alarm-clock-type apps. This build is for installing directly.

## Not tested

- Installing and running on a real phone (none was connected): includes the items in
  [offline-test-plan.md](offline-test-plan.md) "Not tested – needs a real phone".
- The release build on Android 8–12 (the emulator runs Android 37).
