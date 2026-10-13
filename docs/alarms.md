# Prayer alarms

## How they work

- **One alarm per prayer.** Each of the five prayers has its own fixed alarm slot in Android's
  AlarmManager, always set to that prayer's *next* time. Re-planning replaces the slot, so
  duplicates cannot build up. Silent prayers get no alarm. Turning "Play the Azan at prayer times"
  off cancels all five.
- **Rolling.** When an alarm fires, the Azan plays and all alarms are planned again, which moves that
  prayer to its next day. No background job runs every minute.
- **Re-planned automatically** when the location, calculation settings, Azan sounds or the on/off
  switch change, when the app starts, and after: phone restart, clock change, time-zone change,
  app update, and exact-alarm permission change (`SystemEventsReceiver`).
- **Exact alarms** (`setExactAndAllowWhileIdle`) fire on time even in battery-saving idle mode. They
  need permission on Android 12+: `USE_EXACT_ALARM` on Android 13+ (granted at install; this is an
  alarm app) and `SCHEDULE_EXACT_ALARM` on Android 12 (on by default, the user can turn it off:
  Settings → Prayer alarms → Allow exact alarms). Without an exact alarm, inexact alarms are used,
  which may be late, **and Android refuses to start the Azan from the background, so it stays
  silent**. Stage 12 found that Android 13+ gives new installs no `SCHEDULE_EXACT_ALARM`, so the
  Azan was silent on a fresh install until the user found the setting; `USE_EXACT_ALARM` fixes this.
- **Playback** runs in a short foreground service (`AzanPlaybackService`) with a notification
  ("Azan: Dhuhr", Stop button), so it works with the app closed and the screen locked. The service
  stops itself when the Azan ends.
- **Missed Azans are never played late.** If an alarm arrives more than 5 minutes after its time (for
  example the phone was off or asleep in a way Android delayed), the Azan is skipped and the next
  alarms are planned. After a restart, only future times are scheduled. The same prayer time is never
  played twice (e.g. if the clock is moved back).
- Alarm times are fixed moments in real time, computed for the *chosen location*. Changing the
  phone's time zone does not move them.

- **Alarms that are still correct are left alone.** The scheduler remembers what it last gave
  Android and only re-sets a prayer whose time, sound or exactness changed. (The memory is lost when
  the app process ends, which is exactly when Android may have cleared alarms, so everything is then
  set again.)
- **Daily safety net.** While alarms are on, one extra battery-friendly (inexact) alarm at about 03:30
  each night re-plans everything, in case an earlier re-plan was interrupted. No service runs all the time.

Code: `app/src/main/java/com/universalazan/app/alarms/`.

## Automatic updates and recovery (Stage 8)

| Situation | What happens |
|---|---|
| New local day | Each fired alarm re-arms for the next day; the Prayer Times screen switches date at local midnight (checked every minute while visible). |
| App opened | Alarms re-planned (unchanged ones untouched). |
| Phone restarted | Alarms re-planned from saved settings, without opening the app. |
| Clock or time-zone change | Re-planned; alarm moments follow the chosen city, not the phone. |
| Location, method, Asr, high-latitude, adjustments or sounds changed | Re-planned automatically; only the affected alarms change. |
| App process killed by Android | Alarms stay registered with Android; settings are saved on the phone; reopening shows the same location and times. |
| A re-plan was interrupted | The 03:30 daily check restores the alarms. |
| Exact-alarm permission granted later | Android tells the app; alarms are re-set as exact. |

## Permissions used

| Permission | Why |
|---|---|
| `USE_EXACT_ALARM` (Android 13+) | Start the Azan at the exact minute. Granted at install. |
| `SCHEDULE_EXACT_ALARM` (Android 12 only) | The same on Android 12; on by default, the user can turn it off. |
| `POST_NOTIFICATIONS` | Show the "Azan playing / Stop" notification (Android 13+). The Azan still plays without it. |
| `RECEIVE_BOOT_COMPLETED` | Schedule alarms again after the phone restarts. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keep playing in the background with a visible notification. |
| `WAKE_LOCK` | Keep the processor awake while the Azan plays with the screen off. |

## Tested (emulator, Android 37, 13 October 2026)

| Test | Result |
|---|---|
| Exactly one alarm per prayer, 5 in total, no duplicates after repeated re-planning | Pass (automated) |
| Exact alarms used when permission granted | Pass (automated; `exactAllowReason=permission`) |
| Silent prayer: its alarm is removed | Pass (automated) |
| Alarms switched off: all cancelled | Pass (automated) |
| City change: alarms replaced with the new times | Pass (automated) |
| Setting change re-plans automatically | Pass (automated) |
| Alarm on time starts the Azan; Stop (same as notification button) ends it and the service | Pass (automated) |
| Alarm 10 minutes late: Azan not played | Pass (automated) |
| Same alarm delivered twice: played once | Pass (automated) |
| Test alarm through the real AlarmManager path | Pass (automated) |
| **Screen locked**: test alarm fired, Android allowed the background start, Azan played, notification shown, service stopped itself when the Azan ended | Pass (manual, emulator) |
| **Real reboot**: all 5 alarms back without opening the app, still exact | Pass (manual, emulator) |
| **Phone time-zone change** (London → Tokyo): still 5 alarms at exactly the same moments | Pass (manual, emulator) |
| Fresh install (Stage 11, before `USE_EXACT_ALARM`): screen shows exact alarms and notifications missing; "Allow exact alarms" opens Android's page; after granting, all 5 alarms become exact | Pass (manual, emulator) |
| **Fresh install, nothing granted by hand (Stage 12, release build):** before the fix, the Maghrib alarm fired but Android refused to start the Azan (`ForegroundServiceStartNotAllowedException`), so it was silent. After the fix: all alarms exact at install (`exactAllowReason=policy_permission`); Isha alarm fired at 12:52:00.012 and the Azan played in the background | Fixed, pass (manual, emulator) |
| Re-planning with nothing changed: no alarm re-set, Android keeps the very same alarms | Pass (automated) |
| Changing one prayer's sound: only that alarm is re-set | Pass (automated) |
| Daily check exists only while alarms are on; restores alarms after they are lost | Pass (automated) |
| **App process killed** (`am kill`): 5 alarms still registered; reopening shows London with the same times; still 5 alarms | Pass (manual, emulator) |
| **Midnight**: clock set to 23:59:20 with Prayer Times open; at 00:00 the screen showed the next day with new times, Fajr marked next; still 5 alarms | Pass (manual, emulator) |

## Not yet tested: needs a real phone

- Locked-screen playback on a real phone (see "Test alarm in 1 minute" below).
- Behaviour with the phone maker's battery saver (Samsung, Xiaomi, Huawei, etc. can stop background apps).
- Do Not Disturb and silent mode on your phone.
- Android 8-11 (the emulator runs Android 37). A crash that would have happened there was found by
  lint and fixed, but these versions were not run.

### How to test on your phone

1. Install from Android Studio (Run ▶ with the phone connected, see docs/compass-testing.md).
2. Choose your city. Open Settings → Prayer alarms and fix anything shown in red.
3. Press **Test alarm in 1 minute**, lock the phone and wait. The short Azan should play and a
   notification with **Stop** should appear on the lock screen.
4. Also try: Do Not Disturb on, silent mode on, and after restarting the phone (check that
   "Next Azans" still lists times).

## Limitations

Android cannot guarantee an alarm in every situation. The Azan may not be heard if the alarm volume
is zero, Do Not Disturb blocks alarms, the phone is off, the permissions above are turned off, the
phone maker's battery saver closes the app, or a phone call is using the audio.
