package com.guesswhere.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.guesswhere.app.R
import com.guesswhere.app.domain.GameSummary
import com.guesswhere.app.domain.RoundResult
import com.guesswhere.app.ui.components.CountUpText
import com.guesswhere.app.ui.components.MorphingButton
import com.guesswhere.app.ui.components.formatInt
import com.guesswhere.app.ui.components.formatKm

/** End-of-game summary: total with count-up, per-round rows, share, rematch. */
@Composable
fun GameOverScreen(
    summary: GameSummary,
    onPlayAgain: () -> Unit,
    onBackHome: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.game_over_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CountUpText(
            target = summary.totalScore,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.tertiary,
        )
        if (summary.isPersonalBest) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_crown),
                        contentDescription = stringResource(R.string.crown_badge),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.personal_best),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        Text(
            text = summary.config.modeLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        summary.results.forEachIndexed { index, result -> RoundRow(index, result) }

        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = {
                runCatching {
                    val text = context.getString(
                        R.string.share_text,
                        formatInt(summary.totalScore),
                        summary.results.size,
                    )
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND)
                                .putExtra(Intent.EXTRA_TEXT, text)
                                .setType("text/plain"),
                            context.getString(R.string.share_score),
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.share_score)) }
        MorphingButton(
            text = stringResource(R.string.play_again),
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onBackHome) { Text(stringResource(R.string.go_home)) }
    }
}

@Composable
private fun RoundRow(index: Int, result: RoundResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        AsyncImage(
            model = result.image.thumbUrl,
            contentDescription = result.image.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = result.image.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = stringResource(
                    R.string.round_row_points,
                    formatInt(result.points),
                ) + (result.distanceKm?.let {
                    " · " + stringResource(R.string.round_row_distance, formatKm(it))
                } ?: " · " + stringResource(R.string.round_row_skipped)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}