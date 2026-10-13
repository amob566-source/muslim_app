# Azan audio

## Which files the app plays

The app plays three bundled files (resource names `raw/azan_full_fajr`, `raw/azan_full`, `raw/azan_short`):

| Choice | Fajr | Dhuhr, Asr, Maghrib, Isha |
|---|---|---|
| Full Azan | `azan_full_fajr` (includes "aṣ-ṣalātu khayrun mina n-nawm") | `azan_full` (without that phrase, which is only said at Fajr) |
| Short | `azan_short` (the opening "Allāhu akbar, Allāhu akbar") | `azan_short` |
| Silent | nothing | nothing |

Each prayer has its own choice (Settings → Azan sounds). The default is Full Azan for every prayer.

## Where the files come from

The build chooses one of two folders (see `app/build.gradle.kts`):

| Folder | In Git? | Contents |
|---|---|---|
| `app/src/personalAudio/res/` | **No** (in `.gitignore`) | Your own recordings, for private use. Used when this folder exists. |
| `app/src/testAudio/res/` | Yes | Beep tones generated with ffmpeg. Used when there is no personal folder. The app then says clearly that it contains test tones, not an Azan. |

Each folder also has `values/bundled_audio.xml`, which describes the recording shown in the app.

### The personal recordings on this PC

- Source file: `audio_source/Fajr Azan - Muslim call to prayer in Mecca - Sheikh Ali Ahmed Mulla - Presheva Lee.mp3`
  (not in Git). The owner of the project confirmed it is for **personal use only**; it must not be
  redistributed, so it is never pushed to GitHub.
- Made with ffmpeg (`G:\ffmpeg\bin\ffmpeg.exe`), mono AAC 64 kbps:
  - `azan_full_fajr.m4a`: whole recording from 1.0 s, fade-out at the end. 4 min 12 s, about 2.0 MB.
  - `azan_full.m4a`: the same with 179.5 s to 220.8 s removed (the two "aṣ-ṣalātu khayrun mina n-nawm"
    phrases), joined with a 0.6 s crossfade. 3 min 30 s, about 1.7 MB.
  - `azan_short.m4a`: 1.0 s to 21.0 s (first "Allāhu akbar, Allāhu akbar"), 1.2 s fade-out. 20 s, 160 KB.
- The cut points were found from the quiet gaps between phrases (14 phrases, matching the Fajr Azan).
  They were not checked by ear by the developer tools; listen to confirm.

### Replacing the recordings

1. Put the new files in `app/src/personalAudio/res/raw/` with exactly these names:
   `azan_full_fajr.m4a`, `azan_full.m4a`, `azan_short.m4a` (.mp3 and .ogg also work, but keep one file per name).
2. Edit `app/src/personalAudio/res/values/bundled_audio.xml` to describe the recording.
3. Rebuild and reinstall the app.

To share the project publicly with a real Azan, use only recordings whose licence allows
redistribution, put them in `app/src/testAudio/res/raw/` instead, and record the source and
licence here.

## Playback behaviour

- Plays from the app's files (no streaming, no downloads), so it works in airplane mode.
- Streams from the file rather than loading it all into memory.
- Uses the **alarm** volume, so muting media does not silence it, but the alarm volume slider does.
- Asks other apps to pause their audio while the Azan plays. If a phone call (or similar) takes over
  the audio, the Azan stops and does not resume.
- Only one Azan can play at a time: starting one stops any other first.
- A missing or damaged file shows an error instead of crashing.

## Tests

- `AzanSoundTest`, `AzanSoundRepositoryTest` (unit): which file each prayer/choice plays; saving choices.
- `AzanPlayerTest` (device): real playback; checks Android's list of active players to confirm sound
  starts, that two never play at once, that stop works, and that every bundled file plays.
- `AzanSoundsFlowTest` (device): choosing Silent disables the test button and is saved; choices
  survive the screen being recreated; Test and Stop work.
- Manually checked on the emulator: Fajr test playback in airplane mode.
