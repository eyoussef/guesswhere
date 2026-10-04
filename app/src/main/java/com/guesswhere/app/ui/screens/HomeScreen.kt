package com.guesswhere.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guesswhere.app.R
import com.guesswhere.app.data.LocalStore
import com.guesswhere.app.domain.Difficulty
import com.guesswhere.app.domain.GameConfig
import com.guesswhere.app.domain.GamePrefs
import com.guesswhere.app.domain.Pack
import com.guesswhere.app.domain.Scoring
import com.guesswhere.app.ui.components.MorphingButton
import kotlinx.coroutines.launch

/**
 * Menu: pack + difficulty pickers, daily challenge card, leaderboard entry,
 * about dialog. Selections persist through [LocalStore].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    store: LocalStore,
    onPlay: (GameConfig) -> Unit,
    onOpenLeaderboard: () -> Unit,
) {
    val prefs by store.prefs.collectAsStateWithLifecycle(GamePrefs())
    val scope = rememberCoroutineScope()
    var pack by remember { mutableStateOf<Pack?>(null) }
    var difficulty by remember { mutableStateOf<Difficulty?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(prefs) {
        if (pack == null) pack = Pack.byId(prefs.packId)
        if (difficulty == null) difficulty = Difficulty.byId(prefs.difficultyId)
    }

    fun persist() {
        val p = pack ?: return
        val d = difficulty ?: return
        scope.launch { store.savePrefs(GamePrefs(p.id, d.id)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp).fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.daily_card_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = stringResource(R.string.daily_card_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(12.dp))
                MorphingButton(
                    text = stringResource(R.string.daily_play),
                    onClick = { onPlay(GameConfig.forToday()) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Text(
            text = stringResource(R.string.pick_pack),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Pack.entries.forEach { candidate ->
                FilterChip(
                    selected = candidate == pack,
                    onClick = {
                        pack = candidate
                        persist()
                    },
                    label = { Text(candidate.label) },
                )
            }
        }

        Text(
            text = stringResource(R.string.pick_difficulty),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val entries = Difficulty.entries
            entries.forEachIndexed { index, candidate ->
                SegmentedButton(
                    selected = candidate == difficulty,
                    onClick = {
                        difficulty = candidate
                        persist()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = entries.size),
                ) { Text(candidate.label) }
            }
        }

        Text(
            text = stringResource(R.string.rules_hint, Scoring.MAX_POINTS),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        MorphingButton(
            text = stringResource(R.string.play),
            enabled = pack != null && difficulty != null,
            onClick = {
                val p = pack ?: return@MorphingButton
                val d = difficulty ?: return@MorphingButton
                onPlay(GameConfig(p, d, daily = false))
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onOpenLeaderboard, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.leaderboard))
            }
            TextButton(onClick = { showAbout = true }) { Text(stringResource(R.string.about)) }
        }

        Text(
            text = stringResource(R.string.data_credit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }

    if (showAbout) {
        val selected = difficulty ?: Difficulty.NORMAL
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.about_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.about_body),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(R.string.about_scoring_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        stringResource(
                            R.string.about_scoring_body,
                            Scoring.MAX_POINTS,
                            selected.decayKm,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.ok)) }
            },
        )
    }
}