# Qibla validation

The Qibla direction is the initial great-circle bearing from the selected location to the Kaaba,
measured clockwise from **true** (geographic) north. It is calculated on the device from the saved
coordinates; no GPS or internet is needed.

- Code: `app/src/main/java/com/universalazan/app/domain/qibla/QiblaCalculator.kt`
- Tests: `app/src/test/java/com/universalazan/app/domain/qibla/QiblaCalculatorTest.kt`
- Kaaba coordinates used: 21.4225° N, 39.8262° E (approximate).
- Spherical formulas are used. They agree with ellipsoidal results to within a small fraction of
  a degree, which is far finer than a phone compass can show.

Checked on 13 October 2026.

## References

| Source | Type | Result |
|---|---|---|
| JAKIM (Malaysia), zone WLY01 | **Official** | Publishes 292° 31′ 16″ (292.521°). Ours for central Kuala Lumpur: 292.538°. Difference 0.02°; JAKIM's zone reference point is not published with the bearing. |
| AlAdhan Qibla API (`api.aladhan.com/v1/qibla`) | Independent calculation | 16 places on every continent, including Fiji and Samoa (either side of the date line), Anchorage, Reykjavik and Madinah. All within 0.01°. |
| Adhan library `Qibla` class | Independent formula (different equation, Kaaba 21.4225241, 39.8261818) | Over 1,000 points on a world grid. All within 0.01°. |

| Place | Reference (AlAdhan) | Shown in app |
|---|---|---|
| London | 118.99° | 119.0° ESE |
| New York | 58.48° | 58.5° ENE |
| Kuala Lumpur | 292.54° | 292.5° WNW |
| Jakarta | 295.15° | 295.2° WNW |
| Sydney | 277.50° | 277.5° W |
| Suva, Fiji | 281.89° | 281.9° WNW |
| Apia, Samoa | 288.70° | 288.7° WNW |

## Special cases

- At the Kaaba itself (within 50 m) there is no single direction; the app says so.
- At the exact opposite point of the Earth (in the Pacific Ocean), every direction is equally
  valid; the app says so.
- Invalid coordinates are rejected.
- The bearing is always in the range 0° (inclusive) to 360° (exclusive); a value that would
  display as 360.0° is shown as 0.0°.

## Limitations

- The bearing is relative to true north. A magnetic compass points to magnetic north, which can
  differ by several degrees. The live compass (Stage 5) must correct for this.
- Accuracy depends on the accuracy of the saved coordinates. A city entry uses the city centre.
