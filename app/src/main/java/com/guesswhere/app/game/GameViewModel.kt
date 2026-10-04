package com.guesswhere.app.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guesswhere.app.data.CommonsRepository
import com.guesswhere.app.data.LocalStore
import com.guesswhere.app.data.MediaWikiApiError
import com.guesswhere.app.domain.CommonsImage
import com.guesswhere.app.domain.Difficulty
import com.guesswhere.app.domain.GameConfig
import com.guesswhere.app.domain.GameSummary
import com.guesswhere.app.domain.Geo
import com.guesswhere.app.domain.HighScore
import com.guesswhere.app.domain.RoundResult
import com.guesswhere.app.domain.Scoring
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Possible failure buckets; the UI maps them to friendly strings. */
enum class FailureReason { NETWORK, SERVER, EMPTY }

sealed interface GameState {
    data object Idle : GameState
    data object Loading : GameState

    /** A live round waiting for the player to drop a pin and confirm. */
    data class Playing(
        val image: CommonsImage,
        val roundNumber: Int,
        val totalRounds: Int,
        val totalSoFar: Int,
        val pinned: Geo? = null,
    ) : GameState

    /** Overlay after a confirmed or skipped guess; photo still visible behind. */
    data class Feedback(
        val result: RoundResult,
        val roundNumber: Int,
        val totalRounds: Int,
        val totalSoFar: Int,
    ) : GameState

    data class Finished(val summary: GameSummary) : GameState
    data class Failure(val reason: FailureReason, val message: String?) : GameState
}

/**
 * Game state machine: Idle -> start(config) -> Loading -> Playing <-> Feedback
 * -> Finished (or Failure). All rounds are fetched up front so the per-round
 * reveal is instant; only 1-2 network calls per game total.
 */
class GameViewModel(
    private val repository: CommonsRepository,
    private val store: LocalStore,
) : ViewModel() {

    private val _state = MutableStateFlow<GameState>(GameState.Idle)
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var config: GameConfig? = null
    private var generation = 0
    private var roundIndex = 0 // zero-based into [rounds]
    private val rounds = mutableListOf<CommonsImage>()
    private val results = mutableListOf<RoundResult>()

    fun start(config: GameConfig) {
        this.config = config
        val gen = ++generation
        viewModelScope.launch {
            _state.value = GameState.Loading
            runCatching { repository.fetchRounds(config.pack, config.rounds, config.seed) }
                .onSuccess { images ->
                    if (_generation(gen)) return@launch
                    if (images.isEmpty()) {
                        _state.value = GameState.Failure(FailureReason.EMPTY, null)
                    } else {
                        rounds.clear()
                        rounds += images
                        results.clear()
                        roundIndex = 0
                        _state.value = GameState.Playing(
                            image = images.first(),
                            roundNumber = 1,
                            totalRounds = images.size,
                            totalSoFar = 0,
                        )
                    }
                }
                .onFailure { e ->
                    if (_generation(gen)) return@launch
                    _state.value = GameState.Failure(reasonOf(e), e.message)
                }
        }
    }

    fun restart() {
        config?.let(::start)
    }

    fun pinAt(geo: Geo) {
        val current = _state.value as? GameState.Playing ?: return
        if (current.pinned == null) {
            _state.value = current.copy(pinned = geo)
        }
    }

    fun submit() {
        val current = _state.value as? GameState.Playing ?: return
        val pin = current.pinned ?: return
        val decay = (config?.difficulty ?: Difficulty.NORMAL).decayKm
        val (km, points) = Scoring.scoreGuess(pin, current.image.geo, decay)
        recordAndAdvance(RoundResult(current.image, pin, km, points))
    }

    fun skip() {
        val current = _state.value as? GameState.Playing ?: return
        recordAndAdvance(RoundResult(current.image, guess = null, distanceKm = null, points = 0))
    }

    fun next() {
        val current = _state.value as? GameState.Feedback ?: return
        _state.value = GameState.Playing(
            image = rounds[roundIndex],
            roundNumber = roundIndex + 1,
            totalRounds = rounds.size,
            totalSoFar = current.totalSoFar,
        )
    }

    fun reset() {
        generation++
        rounds.clear()
        results.clear()
        config = null
        _state.value = GameState.Idle
    }

    private fun recordAndAdvance(result: RoundResult) {
        results += result
        val totalSoFar = results.sumOf { it.points }
        roundIndex++
        if (roundIndex >= rounds.size) {
            viewModelScope.launch { finishGame(totalSoFar) }
        } else {
            _state.value = GameState.Feedback(
                result = result,
                roundNumber = roundIndex,
                totalRounds = rounds.size,
                totalSoFar = totalSoFar,
            )
        }
    }

    private suspend fun finishGame(totalSoFar: Int) {
        val cfg = config ?: return
        val entry = HighScore(
            playedAt = System.currentTimeMillis(),
            total = totalSoFar,
            rounds = rounds.size,
            modeLabel = cfg.modeLabel(),
        )
        val isBest = store.recordScore(entry)
        _state.value = GameState.Finished(
            GameSummary(
                config = cfg,
                totalScore = totalSoFar,
                results = results.toList(),
                isPersonalBest = isBest,
                playedAt = entry.playedAt,
            ),
        )
    }

    /** True when a newer start/reset superseded the running job. */
    private fun _generation(gen: Int): Boolean = gen != generation

    private fun reasonOf(e: Throwable): FailureReason = when (e) {
        is MediaWikiApiError, is retrofit2.HttpException -> FailureReason.SERVER
        is IOException, is kotlinx.coroutines.TimeoutCancellationException -> FailureReason.NETWORK
        else -> FailureReason.NETWORK
    }

    companion object {
        fun factory(
            repository: CommonsRepository,
            store: LocalStore,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel(repository, store) }
        }
    }
}