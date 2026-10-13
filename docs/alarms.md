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
  need "Alarms and reminders" permission on Android 12+, which the user grants in system settings
  (Settings → Prayer alarms → Allow exact alarms). Without it, inexact alarms are used, which may be
  late, and Android may refuse to start the Azan from the background.
- **Playback** runs in a short foreground service (`AzanPlaybackService`) with a notification
  ("Azan: Dhuhr", Stop button), so it works with the app closed and the screen locked. The service
  stops itself when the Azan ends.
- **Missed Azans are never played late.** If an alarm arrives more than 5 minutes after its time (for
  example the phone was off or asleep in a way Android delayed), the Azan is skipped and the next
  alarms are planned. After a restart, only future times are scheduled. The same prayer time is never
  played twice (e.g. if the clock is moved back).
- Alarm times are fixed moments in real time, computed for the *chosen location*. Changing the
  phone's time zone does not move them.

Code: `app/src/main/java/com/universalazan/app/alarms/`.

## Permissions used

| Permission | Why |
|---|---|
| `SCHEDULE_EXACT_ALARM` | Start the Azan at the exact minute. User grants it in system settings. |
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
| Fresh install: screen shows exact alarms and notifications missing; "Allow exact alarms" opens Android's page; after granting, all 5 alarms become exact | Pass (manual, emulator) |

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
