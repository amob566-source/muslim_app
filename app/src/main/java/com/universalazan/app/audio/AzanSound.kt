package com.universalazan.app.audio

import androidx.annotation.RawRes
import com.universalazan.app.R
import com.universalazan.app.domain.prayer.Prayer

/** What to play at a prayer time. */
enum class AzanSound {
    /** The complete Azan. */
    Full,

    /** Only the opening "Allahu akbar". */
    Short,

    /** No sound at all. */
    Silent,
}

/** The sound chosen for each prayer. Defaults to the full Azan for every prayer. */
data class AzanSoundSettings(
    val sounds: Map<Prayer, AzanSound> = Prayer.entries.associateWith { AzanSound.Full },
) {
    fun soundFor(prayer: Prayer): AzanSound = sounds[prayer] ?: AzanSound.Full

    fun with(prayer: Prayer, sound: AzanSound) = copy(sounds = sounds + (prayer to sound))
}

/**
 * The bundled file to play, or null for Silent.
 *
 * The full Fajr Azan contains "As-salatu khayrun mina n-nawm" ("prayer is better than sleep"),
 * which is only said at Fajr, so the other prayers use a full version without that phrase.
 */
@RawRes
fun azanRawResource(prayer: Prayer, sound: AzanSound): Int? = when (sound) {
    AzanSound.Silent -> null
    AzanSound.Short -> R.raw.azan_short
    AzanSound.Full -> if (prayer == Prayer.Fajr) R.raw.azan_full_fajr else R.raw.azan_full
}
