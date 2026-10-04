package com.guesswhere.app.domain

import kotlinx.serialization.Serializable

/** WGS84 coordinate, decimal degrees. */
data class Geo(val lat: Double, val lon: Double)

/** Photo pack: WORLD uses random files, others use Commons full-text search. */
enum class Pack(val id: String, val label: String, val search: String?) {
    WORLD("world", "Whole world", null),
    COASTS("coasts", "Coasts and oceans", "coast beach sea filetype:bitmap"),
    MOUNTAINS("mountains", "Mountains", "mountain landscape filetype:bitmap"),
    CITIES("cities", "Cities", "city skyline filetype:bitmap");

    companion object {
        fun byId(id: String?): Pack = entries.firstOrNull { it.id == id } ?: WORLD
    }
}

/** Tighter decay curve = harsher scoring. */
enum class Difficulty(val id: String, val label: String, val decayKm: Double) {
    EASY("easy", "Easy", 2600.0),
    NORMAL("normal", "Normal", 1400.0),
    HARD("hard", "Hard", 650.0);

    companion object {
        fun byId(id: String?): Difficulty = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

data class GameConfig(
    val pack: Pack = Pack.WORLD,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val daily: Boolean = false,
    /** Seed fixes the round ordering for the Daily Mix. */
    val seed: Long? = null,
    val rounds: Int = ROUNDS_PER_GAME,
) {
    fun modeLabel(): String = when {
        daily -> "Daily Mix"
        else -> "${pack.label} · ${difficulty.label}"
    }

    companion object {
        const val ROUNDS_PER_GAME = 5

        /** Deterministic-feel daily: pack cycles by day, ordering seeded by day. */
        fun forToday(epochDay: Long = java.time.LocalDate.now().toEpochDay()): GameConfig {
            val packs = listOf(Pack.COASTS, Pack.MOUNTAINS, Pack.CITIES)
            return GameConfig(
                pack = packs[(epochDay % packs.size).toInt().mod(packs.size)],
                difficulty = Difficulty.NORMAL,
                daily = true,
                seed = epochDay,
            )
        }
    }
}

/** One playable Commons photograph, pre-filtered to geo-tagged bitmaps. */
data class CommonsImage(
    val pageId: Long,
    val title: String,
    /** Commons-provided scaled-down URL for loading. */
    val thumbUrl: String,
    val pageUrl: String,
    val geo: Geo,
    /** Photographer name or credit, HTML already stripped. */
    val artist: String,
    /** Short license name, e.g. "CC BY-SA 4.0". */
    val license: String,
    /** Location/description text for the post-guess debrief, may be null. */
    val description: String?,
)

/** Result of one answered round. */
data class RoundResult(
    val image: CommonsImage,
    /** null when the round was skipped. */
    val guess: Geo?,
    val distanceKm: Double?,
    val points: Int,
)

data class GameSummary(
    val config: GameConfig,
    val totalScore: Int,
    val results: List<RoundResult>,
    val isPersonalBest: Boolean,
    val playedAt: Long,
)

@Serializable
data class HighScore(
    val playedAt: Long,
    val total: Int,
    val rounds: Int,
    val modeLabel: String,
)

@Serializable
data class GamePrefs(
    val packId: String = Pack.WORLD.id,
    val difficultyId: String = Difficulty.NORMAL.id,
)