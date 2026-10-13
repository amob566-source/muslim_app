# Live Qibla compass: how it works and how it was tested

## How it works

- **Sensors** (chosen when the app starts, depending on what the phone has):
  1. Android's *rotation vector* sensor (accelerometer + magnetometer + gyroscope, fused). Best.
  2. Accelerometer + magnetometer directly. Works, a little more jittery.
  3. No magnetometer: no live compass. The screen says so and still shows the calculated bearing.
     A fake compass is never shown.
- **Heading:** combines the direction of the top of the screen (phone flat) and the back of the
  phone (phone upright), so it works however the phone is held. Screen rotation (portrait and both
  landscapes) is taken into account. Code: `app/src/main/java/com/universalazan/app/sensors/HeadingMath.kt`.
- **True north:** sensors measure *magnetic* north. Android's built-in World Magnetic Model
  (`android.hardware.GeomagneticField`) gives the correction (declination) for the saved location and
  today's date, offline. The screen shows the correction used.
- **Warnings:** sensor accuracy (high / medium / low / unreliable) with figure-of-eight calibration
  advice, and a magnetic-interference warning when the measured field is outside Earth's normal
  20-70 µT range (magnets, metal, speakers, magnetic cases).
- **Battery:** sensors run only while the Qibla screen is visible, and stop about one second after
  leaving it. No GPS or location permission is used.

## Tested on the emulator (13 October 2026)

Simulated sensor values were set with the emulator console (`adb emu sensor set ...`). Saved location:
London, Ontario (declination shown by the app: -8.9°).

| Test | Expected | Result |
|---|---|---|
| Phone flat, top towards magnetic east | true heading 90 - 8.9 = 81° | 81° (E) |
| Magnetic heading 53.3° | true heading 44.4° | 44° (NE), "Turn right 9°" |
| Phone pointing at the Qibla | "You are facing the Qibla" | Shown; gold marker under the pointer, ring turns green |
| Strong magnetic field (~150 µT) | interference warning | Shown |
| Screen rotated to landscape (phone not moved) | heading moves 90° (another edge is "top") | Qibla marker moved a quarter turn left |

Automated tests: `HeadingMathTest` (flat, upright, tilted, all four screen rotations, turn
direction, smoothing across 359°/0°, interference, magnetic to true) and `QiblaCompassScreenTest`
(turn left/right, facing, no sensor, calibration and interference messages).

## Not yet tested: needs a real phone

The emulator only simulates sensors. Please test on your phone:

1. On the phone: **Settings → About phone**, tap **Build number** seven times to enable Developer
   options. Then **Settings → System → Developer options → USB debugging: on**.
2. Connect the phone to the PC with a USB cable and accept the "Allow USB debugging?" prompt.
3. In Android Studio, choose your phone in the device list at the top and press **Run ▶**.
4. Choose your city, open **Qibla**, and check:
   - The dial turns smoothly as you turn around.
   - The Qibla direction matches a reliable reference (a local mosque, or a known landmark).
   - It works with the phone flat and upright, in portrait and landscape.
   - Holding it next to a magnet or laptop shows the interference warning.
   - Airplane mode on: it still works.

## Limitations

- Phone compasses can be disturbed by magnets, metal, phone cases, cars and electronics, and must
  sometimes be recalibrated. The app shows warnings but cannot detect every disturbance.
- Accuracy is typically a few degrees at best.
- The magnetic model built into Android is updated with Android releases; on old phones it may be
  slightly out of date, usually by well under a degree.
