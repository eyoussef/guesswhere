package com.guesswhere.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.guesswhere.app.domain.Difficulty
import com.guesswhere.app.domain.GamePrefs
import com.guesswhere.app.domain.HighScore
import com.guesswhere.app.domain.Pack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.guessWhereStore by preferencesDataStore(name = "guesswhere")

/**
 * Single-device persistence over DataStore Preferences: leaderboard,
 * already-played image ids, and last-used game settings. No accounts, no sync.
 */
class LocalStore(context: Context, private val json: Json) {

    private val store = context.guessWhereStore

    private object Keys {
        val LEADERBOARD = stringPreferencesKey("leaderboard_json")
        val SEEN = stringSetPreferencesKey("seen_ids")
        val PACK = stringPreferencesKey("pack_id")
        val DIFFICULTY = stringPreferencesKey("difficulty_id")
    }

    val topScores: Flow<List<HighScore>> = store.data.map { prefs ->
        prefs[Keys.LEADERBOARD]
            ?.let { s -> runCatching { json.decodeFromString<List<HighScore>>(s) }.getOrNull() }
            ?: emptyList()
    }

    val prefs: Flow<GamePrefs> = store.data.map { p ->
        GamePrefs(
            packId = p[Keys.PACK] ?: Pack.WORLD.id,
            difficultyId = p[Keys.DIFFICULTY] ?: Difficulty.NORMAL.id,
        )
    }

    /** Returns true when [entry] beats the previous best total. */
    suspend fun recordScore(entry: HighScore): Boolean {
        val current = topScores.first()
        val isBest = entry.total > (current.maxOfOrNull { it.total } ?: -1)
        val merged = (current + entry).sortedByDescending { it.total }.take(LEADERBOARD_MAX)
        store.edit { it[Keys.LEADERBOARD] = json.encodeToString(merged) }
        return isBest
    }

    suspend fun clearScores() {
        store.edit { it.remove(Keys.LEADERBOARD) }
    }

    suspend fun seenIds(): Set<Long> =
        (store.data.first()[Keys.SEEN] ?: emptySet())
            .mapNotNull(String::toLongOrNull)
            .toSet()

    suspend fun rememberSeen(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        store.edit { prefs ->
            val existing = (prefs[Keys.SEEN] ?: emptySet()).toMutableSet()
            existing += ids.map(Long::toString)
            prefs[Keys.SEEN] = if (existing.size > SEEN_MAX) existing.take(SEEN_MAX).toSet() else existing
        }
    }

    suspend fun savePrefs(prefsValue: GamePrefs) {
        store.edit {
            it[Keys.PACK] = prefsValue.packId
            it[Keys.DIFFICULTY] = prefsValue.difficultyId
        }
    }

    private companion object {
        const val LEADERBOARD_MAX = 25
        const val SEEN_MAX = 4000
    }
}