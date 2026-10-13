# Prayer-time validation

Prayer times are calculated on the device with the
[Adhan library](https://github.com/batoulapps/adhan-kotlin) (`com.batoulapps.adhan:adhan2:0.0.7`).
This file records how the results were checked. It is not a guarantee that every time matches every
mosque; communities use different methods, adjustments and precautions (ihtiyat).

The automated checks are in
`app/src/test/java/com/universalazan/app/domain/prayer/ReferenceTimetableTest.kt`.
A difference of up to **2 minutes** is accepted, because published timetables round and add
small precautionary minutes.

Checked on 13 October 2026.

## Sources

| Source | Type | Used for |
|---|---|---|
| JAKIM e-solat (`e-solat.gov.my`), zone WLY01 | **Official** Malaysian timetable | Kuala Lumpur, 13 Oct and 21 Dec 2026 |
| AlAdhan API (`api.aladhan.com`) | Independent calculation (not an official timetable) | London, Makkah, New York, Karachi |
| NOAA solar-position equations | Independent astronomical calculation | Asr in London (see below) |

## Results

| Place, date | Method | Result |
|---|---|---|
| Kuala Lumpur, 13 Oct 2026 | Singapore (20°/18°) vs JAKIM | Sunrise, Dhuhr, Asr, Maghrib, Isha within 2 min. **Fajr 10 min earlier than JAKIM** (see below). |
| Kuala Lumpur, 21 Dec 2026 | Singapore vs JAKIM | Same as above: all within 2 min except **Fajr 10–11 min earlier**. |
| London, 13 Oct 2026 | MWL, Standard and Hanafi Asr | All within 2 min (Asr compared with NOAA). |
| London, 28 and 29 Mar 2026 (clocks go forward) | MWL | All within 2 min; clock times jump by one hour correctly. |
| London, 24 and 25 Oct 2026 (clocks go back) | MWL | All within 2 min; clock times drop by one hour correctly. |
| London, 21 Jun 2026 | MWL, one-seventh of the night | All within 2 min. |
| Makkah, 13 Oct 2026 | Umm al-Qura | All within 2 min. |
| New York, 13 Oct 2026 | ISNA | All within 2 min. |
| Karachi, 13 Oct 2026 | Karachi, Hanafi Asr | All within 2 min. |

## Differences found

1. **Malaysia (JAKIM) Fajr is 10 minutes later than the "Singapore" method.** Our Fajr equals
   JAKIM's published *imsak* time; JAKIM's Fajr is 10 minutes after that. All other JAKIM times
   agree. Users in Malaysia can add +10 minutes to Fajr once per-prayer adjustments are available
   in Settings (Stage 9). The method description in the app mentions this.
2. **AlAdhan's Asr in London was 2–3 minutes later than ours.** An independent calculation with the
   NOAA solar-position equations gave 15:35:42 on 13 Oct (ours 15:35, AlAdhan 15:38) and 15:17:47
   on 24 Oct (ours 15:17, AlAdhan 15:20). Our results agree with NOAA, so NOAA values are used as
   the Asr reference for London.
3. **High-latitude rule.** Above 48° latitude, the default "Automatic" rule uses one-seventh of the
   night. In London on 28 March this makes Fajr about 20 minutes later and Isha about 12 minutes
   earlier than the pure 18°/17° angles. This is how the rule is meant to work; users can choose
   "Twilight angle" or another rule in Settings.
4. **Adhan's built-in method adjustments** (for example Dhuhr +1 minute for Muslim World League)
   are not used by AlAdhan, so Dhuhr is often 1 minute later than AlAdhan. This is expected.

## Known limitations

- **Polar day and night:** where the sun does not rise or set on a date (for example Tromsø,
  Norway, in June and December), times cannot be calculated. The app says so instead of guessing.
- **Umm al-Qura in Ramadan:** the official Isha is 120 minutes after Maghrib during Ramadan. This
  is not applied automatically.
- **Turkey:** the "Turkey" method approximates Diyanet's official times; it was not checked
  against the official Diyanet timetable.
- **Places whose clocks run far from solar time** (for example Kiritimati, UTC+14): handled by
  shifting the solar day so that Dhuhr falls on the requested local date (covered by tests).
- Results depend on the accuracy of the chosen coordinates and time zone.
