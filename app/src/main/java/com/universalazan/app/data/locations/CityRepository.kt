package com.universalazan.app.data.locations

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Loads the bundled city list once, in the background, the first time it is needed. */
class CityRepository(private val assets: AssetManager) {
    private val mutex = Mutex()
    private var search: CitySearch? = null

    suspend fun citySearch(): CitySearch = mutex.withLock {
        search ?: withContext(Dispatchers.IO) {
            assets.open(CITIES_ASSET).bufferedReader(Charsets.UTF_8).useLines { lines ->
                CitySearch(lines.mapNotNull(::parseCityLine).toList())
            }
        }.also { search = it }
    }

    private companion object {
        const val CITIES_ASSET = "cities.tsv"
    }
}
