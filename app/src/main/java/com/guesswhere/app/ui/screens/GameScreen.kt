package com.guesswhere.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.guesswhere.app.R
import com.guesswhere.app.domain.Geo
import com.guesswhere.app.domain.RoundResult
import com.guesswhere.app.game.FailureReason
import com.guesswhere.app.game.GameState
import com.guesswhere.app.game.GameViewModel
import com.guesswhere.app.ui.components.CountUpText
import com.guesswhere.app.ui.components.CreditChip
import com.guesswhere.app.ui.components.ErrorState
import com.guesswhere.app.ui.components.LoadingState
import com.guesswhere.app.ui.components.MapAttribution
import com.guesswhere.app.ui.components.MorphingButton
import com.guesswhere.app.ui.components.formatInt
import com.guesswhere.app.ui.components.formatKm
import com.guesswhere.app.ui.map.OsmMap
import com.guesswhere.app.ui.theme.ScrimReveal

private const val COLLAPSED_FRACTION = 0.34f
private const val EXPANDED_FRACTION = 0.64f
private const val DRAG_THRESHOLD_PX = 48f

/**
 * Full game flow inside one route: loading/failure, the round loop
 * (photo + map panel + confirm), the reveal overlay, and the game-over screen.
 */
@Composable
fun GameScreen(gameViewModel: GameViewModel, onExit: () -> Unit) {
    val state by gameViewModel.state.collectAsStateWithLifecycle()
    var showQuitDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = state !is GameState.Finished && state !is GameState.Idle) {
        showQuitDialog = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalHeight = maxHeight

        // Photo layer: crossfades per round, dims during the reveal.
        Crossfade(
            targetState = state.photoUrl(),
            label = "photo",
        ) { photoUrl ->
            Box(Modifier.fillMaxSize()) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                val revealScrim by animateFloatAsState(
                    targetValue = if (state is GameState.Feedback) 0.55f else 0f,
                    label = "revealScrim",
                )
                if (revealScrim > 0.01f) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(ScrimReveal.copy(alpha = revealScrim)),
                    )
                }
            }
        }

        // Panel sizing shared by the round and reveal states: collapses for a
        // fresh round, expands for the reveal; the handle toggles by drag.
        var fraction by remember { mutableFloatStateOf(COLLAPSED_FRACTION) }
        LaunchedEffect(state is GameState.Feedback, state.roundNumberKey()) {
            fraction = if (state is GameState.Feedback) EXPANDED_FRACTION else COLLAPSED_FRACTION
        }
        val handleDrag = remember {
            var accum = 0f
            return@remember { dy: Float ->
                accum += dy
                when {
                    accum <= -DRAG_THRESHOLD_PX -> {
                        fraction = EXPANDED_FRACTION
                        accum = 0f
                    }
                    accum >= DRAG_THRESHOLD_PX -> {
                        fraction = COLLAPSED_FRACTION
                        accum = 0f
                    }
                }
            }
        }

        when (val s = state) {
            is GameState.Playing -> {
                RoundChrome(
                    showSkip = true,
                    roundNumber = s.roundNumber,
                    totalRounds = s.totalRounds,
                    onSkip = gameViewModel::skip,
                )
                PlayingPanel(
                    modifier = Modifier.align(BottomCenter),
                    panelHeight = totalHeight * fraction,
                    roundScoreText = stringResource(R.string.round_score, formatInt(s.totalSoFar)),
                    pinned = s.pinned,
                    onHandleDrag = handleDrag,
                    onPinAt = gameViewModel::pinAt,
                    onSkip = gameViewModel::skip,
                    onConfirm = gameViewModel::submit,
                )
            }
            is GameState.Feedback -> {
                RoundChrome(
                    showSkip = false,
                    roundNumber = s.roundNumber,
                    totalRounds = s.totalRounds,
                    onSkip = {},
                )
                FeedbackPanel(
                    modifier = Modifier.align(BottomCenter),
                    panelHeight = totalHeight * fraction,
                    result = s.result,
                    onNext = gameViewModel::next,
                )
            }
            GameState.Idle -> LaunchedEffect(Unit) { onExit() }
            GameState.Loading -> LoadingState(
                stringResource(R.string.loading_round),
                modifier = Modifier.align(Center),
            )
            is GameState.Failure -> ErrorState(
                message = stringResource(
                    when (s.reason) {
                        FailureReason.NETWORK, FailureReason.EMPTY -> R.string.error_no_images
                        FailureReason.SERVER -> R.string.error_generic
                    },
                    s.message ?: "",
                ),
                onRetry = gameViewModel::restart,
                onBack = onExit,
                modifier = Modifier.align(Center),
            )
            is GameState.Finished -> GameOverScreen(
                summary = s.summary,
                onPlayAgain = gameViewModel::restart,
                onBackHome = onExit,
            )
        }
    }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text(stringResource(R.string.give_up)) },
            text = { Text(stringResource(R.string.quit_game_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showQuitDialog = false
                    onExit()
                }) { Text(stringResource(R.string.give_up)) }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

private fun GameState.photoUrl(): String? = when (this) {
    is GameState.Playing -> image.thumbUrl
    is GameState.Feedback -> result.image.thumbUrl
    else -> null
}

private fun GameState.roundNumberKey(): Int = when (this) {
    is GameState.Playing -> roundNumber
    is GameState.Feedback -> roundNumber
    else -> 0
}

/** Top HUD chip: round counter plus the skip affordance while a round is live. */
@Composable
private fun BoxScope.RoundChrome(
    showSkip: Boolean,
    roundNumber: Int,
    totalRounds: Int,
    onSkip: () -> Unit,
) {
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Text(
                text = stringResource(R.string.round_of, roundNumber, totalRounds),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        if (showSkip) {
            TextButton(onClick = onSkip) { Text(stringResource(R.string.skip_round)) }
        }
    }
}

@Composable
private fun GamePanelSurface(
    modifier: Modifier,
    panelHeight: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(panelHeight),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            content = content,
        )
    }
}

@Composable
private fun PanelHandle(onDrag: (Float) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 44.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dy -> onDrag(dy) }
                },
        )
    }
}

@Composable
private fun PlayingPanel(
    modifier: Modifier,
    panelHeight: Dp,
    roundScoreText: String,
    pinned: Geo?,
    onHandleDrag: (Float) -> Unit,
    onPinAt: (Geo) -> Unit,
    onSkip: () -> Unit,
    onConfirm: () -> Unit,
) {
    GamePanelSurface(modifier, panelHeight) {
        PanelHandle(onDrag = onHandleDrag)
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            OsmMap(
                pin = pinned,
                onMapTap = onPinAt,
                modifier = Modifier.fillMaxSize(),
            )
            MapAttribution(
                modifier = Modifier
                    .align(BottomCenter)
                    .padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = roundScoreText,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSkip) { Text(stringResource(R.string.skip_round)) }
        }
        MorphingButton(
            text = stringResource(
                if (pinned == null) R.string.place_pin_hint else R.string.confirm_guess,
            ),
            enabled = pinned != null,
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FeedbackPanel(
    modifier: Modifier,
    panelHeight: Dp,
    result: RoundResult,
    onNext: () -> Unit,
) {
    GamePanelSurface(modifier, panelHeight) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            OsmMap(
                pin = null,
                onMapTap = null,
                guess = result.guess,
                answer = result.image.geo,
                reveal = true,
                modifier = Modifier.fillMaxSize(),
            )
            MapAttribution(
                modifier = Modifier
                    .align(BottomCenter)
                    .padding(bottom = 6.dp),
            )
            CreditChip(
                image = result.image,
                modifier = Modifier
                    .align(TopStart)
                    .padding(8.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.result_reveal_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            CountUpText(
                target = result.points,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = result.distanceKm
                    ?.let { stringResource(R.string.result_distance_km, formatKm(it)) }
                    ?: stringResource(R.string.result_skipped),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = result.image.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
        )
        result.image.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Spacer(Modifier.height(10.dp))
        MorphingButton(
            text = stringResource(R.string.result_next),
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}