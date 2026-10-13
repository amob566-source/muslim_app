# Usage: python tools/noaa_asr_check.py   (prints true Asr times for the reference cases in docs/prayer-time-validation.md)
# Independent Asr check using the NOAA solar position equations (accurate to well under a minute).
import math, datetime as dt

def sun(t):  # t: aware UTC datetime -> (declination deg, equation of time minutes)
    jd = t.timestamp() / 86400.0 + 2440587.5
    T = (jd - 2451545.0) / 36525.0
    L0 = (280.46646 + T * (36000.76983 + 0.0003032 * T)) % 360
    M = 357.52911 + T * (35999.05029 - 0.0001537 * T)
    e = 0.016708634 - T * (0.000042037 + 0.0000001267 * T)
    C = (math.sin(math.radians(M)) * (1.914602 - T * (0.004817 + 0.000014 * T))
         + math.sin(math.radians(2 * M)) * (0.019993 - 0.000101 * T) + math.sin(math.radians(3 * M)) * 0.000289)
    true_long = L0 + C
    omega = 125.04 - 1934.136 * T
    lam = true_long - 0.00569 - 0.00478 * math.sin(math.radians(omega))
    eps0 = 23 + (26 + (21.448 - T * (46.815 + T * (0.00059 - T * 0.001813))) / 60) / 60
    eps = eps0 + 0.00256 * math.cos(math.radians(omega))
    decl = math.degrees(math.asin(math.sin(math.radians(eps)) * math.sin(math.radians(lam))))
    y = math.tan(math.radians(eps / 2)) ** 2
    eot = 4 * math.degrees(y * math.sin(2 * math.radians(L0)) - 2 * e * math.sin(math.radians(M))
                           + 4 * e * y * math.sin(math.radians(M)) * math.cos(2 * math.radians(L0))
                           - 0.5 * y * y * math.sin(4 * math.radians(L0)) - 1.25 * e * e * math.sin(2 * math.radians(M)))
    return decl, eot

def altitude(t, lat, lon):
    decl, eot = sun(t)
    minutes = t.hour * 60 + t.minute + t.second / 60
    ha = (minutes + eot + 4 * lon) / 4 - 180
    lat_r, d_r, h_r = map(math.radians, (lat, decl, ha))
    return math.degrees(math.asin(math.sin(lat_r) * math.sin(d_r) + math.cos(lat_r) * math.cos(d_r) * math.cos(h_r)))

def asr(date, lat, lon, factor, utc_offset_h):
    # solar noon
    t = dt.datetime(date.year, date.month, date.day, 12, tzinfo=dt.timezone.utc) - dt.timedelta(minutes=4 * lon)
    for _ in range(3):
        _, eot = sun(t)
        t = dt.datetime(date.year, date.month, date.day, tzinfo=dt.timezone.utc) + dt.timedelta(minutes=720 - 4 * lon - eot)
    decl_noon, _ = sun(t)
    target = math.degrees(math.atan(1 / (factor + math.tan(math.radians(abs(lat - decl_noon))))))
    lo, hi = t, t + dt.timedelta(hours=8)
    for _ in range(60):  # bisection on altitude (decreasing in the afternoon)
        mid = lo + (hi - lo) / 2
        if altitude(mid, lat, lon) > target: lo = mid
        else: hi = mid
    return (t + dt.timedelta(hours=utc_offset_h)).strftime('%H:%M:%S'), (lo + dt.timedelta(hours=utc_offset_h)).strftime('%H:%M:%S')

CASES = [
    ("London 2026-10-13 standard", dt.date(2026, 10, 13), 51.5074, -0.1278, 1, 1),
    ("London 2026-10-13 hanafi", dt.date(2026, 10, 13), 51.5074, -0.1278, 2, 1),
    ("London 2026-10-24 standard", dt.date(2026, 10, 24), 51.5074, -0.1278, 1, 1),
    ("London 2026-03-29 standard", dt.date(2026, 3, 29), 51.5074, -0.1278, 1, 1),
    ("Makkah 2026-10-13 standard", dt.date(2026, 10, 13), 21.4225, 39.8262, 1, 3),
    ("KL 2026-10-13 standard", dt.date(2026, 10, 13), 3.1390, 101.6869, 1, 8),
]
import sys
if len(sys.argv) > 1:
    CASES = [("London 2026-10-25", dt.date(2026,10,25), 51.5074, -0.1278, 1, 0), ("London 2026-03-28", dt.date(2026,3,28), 51.5074, -0.1278, 1, 0), ("London 2026-06-21", dt.date(2026,6,21), 51.5074, -0.1278, 1, 1)]
for label, d, lat, lon, f, off in CASES:
    noon, a = asr(d, lat, lon, f, off)
    print(f"{label}: solar noon {noon}, Asr {a}")
