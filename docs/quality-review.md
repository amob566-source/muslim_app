# Reliability and user-experience review (Stage 10)

Done on 13 October 2026 on the emulator (Android 37). What was checked, what was found, and what changed.

## Accessibility

- **Automated checks on every screen.** `AccessibilityTest` runs Google's Accessibility Test Framework
  (the checks behind Android's Accessibility Scanner: missing labels, touch targets under 48dp, low
  text contrast, and more) on Prayer Times, Qibla, Settings, Location, time-zone picker, Prayer
  calculation, Azan sounds, Prayer alarms, and About. **All 9 pass.**
- `AccessibilityCheckerSanityTest` proves the checks really run: a deliberately bad 16dp unlabelled
  button must fail, and does.
- **Found and fixed:** the Location screen had two buttons both called "Change time zone" (one for
  the saved location, one in the coordinates form). Screen-reader users could not tell them apart.
  The coordinates one is now "Choose time zone for these coordinates".
- Already in place: headings marked for screen readers, labels on icon buttons, prayer rows read as
  one item ("Fajr, 5:30 AM, next prayer"), "--:--" read as "not calculated yet", compass dial
  described in words ("Compass. Turn right 29°").

## Large text

- Checked at the largest font size (200%). All screens wrap text and scroll; nothing is cut off.
- **Found and fixed:** the "Prayer Times" label in the bottom bar wrapped onto two lines and was
  pushed against the left edge. Navigation labels are now centred (up to two lines).

## Screen sizes

- Small phone (360 × 640 dp): all content fits; Qibla bearing and compass readable.
- Tablet (900 × 1280 dp): side navigation rail; content kept to a readable width.
- Landscape: side rail (checked in Stage 1 and Stage 5).

## Rotation and app state

`RotationStateTest` (Android rebuilds the screen on rotation):

| What was in progress | Kept after rotation |
|---|---|
| Coordinates typed but not saved | Yes |
| Time-zone picker open, with search text | Yes |
| Prayer calculation screen open | Yes |
| Azan playing (test sound) | Yes, keeps playing |

Earlier stages already test: selected section, saved location, Azan sound choices, theme.

## Errors and missing hardware (reviewed)

| Situation | Message shown |
|---|---|
| Settings cannot be read | Prayer Times error card with "Try again" |
| City list cannot be loaded | "You can still enter coordinates" |
| Invalid coordinates | Field-specific messages |
| GPS: permission refused, refused permanently, location off, no fix | Explanation plus a button to fix it where possible |
| Sun does not rise or set (polar) | Explanation instead of times |
| No compass sensor | "No compass sensor" with the calculated bearing; no fake compass |
| Compass uncalibrated or disturbed | Calibration and interference messages |
| Azan file missing or damaged, or audio in use | Message on the Azan sounds screen |
| Alarm permissions missing, alarm volume zero, battery restrictions | Prayer alarms screen, with buttons to fix |

## Alarm restrictions

- **New:** the Prayer alarms screen shows a tip for the phone's maker (Samsung, Xiaomi/Redmi/POCO,
  Huawei/Honor, OPPO/realme/OnePlus, vivo/iQOO, or a general tip), because their own battery
  savers are the most common reason alarms fail. The exact menu names vary between models.

## Other

- The Azan notification now uses the app's 12/24-hour setting.

## Test totals after Stage 10

127 unit tests, 75 device tests, lint: no issues.

## Not yet checked (needs a real phone)

- TalkBack (screen reader) used by a person, end to end.
- Phone-maker battery menus: the tips follow the usual menu names but were not checked on each brand.
