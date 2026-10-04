package com.guesswhere.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guesswhere.app.R
import com.guesswhere.app.domain.CommonsImage
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Standard CTA with the M3 Expressive "shape morph" feel: the corner radius
 * squashes while pressed, then springs back.
 */
@Composable
fun MorphingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner: Dp by animateDpAsState(
        targetValue = if (pressed) 10.dp else 32.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "corner",
    )
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(corner),
        colors = colors,
        interactionSource = interaction,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}

/** Numeric label that counts up from zero on first appearance. */
@Composable
fun CountUpText(
    target: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    durationMillis: Int = 900,
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(target) { started = true }
    val value by animateFloatAsState(
        targetValue = if (started) target.toFloat() else 0f,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "countUp",
    )
    Text(text = formatInt(value.roundToInt()), style = style, color = color, modifier = modifier)
}

/** Photo attribution chip: required credit, tap opens the Commons file page. */
@Composable
fun CreditChip(image: CommonsImage, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        onClick = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(image.pageUrl)))
            }
        },
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.photo_by, image.artist, image.license),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** OSM tile attribution, required by their terms; keep it on every map. */
@Composable
fun MapAttribution(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.map_credit),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun LoadingState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        androidx.compose.material3.CircularProgressIndicator()
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        MorphingButton(stringResource(R.string.retry), onRetry, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

fun formatInt(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value.toLong())

fun formatKm(km: Double): String = String.format(Locale.getDefault(), "%.1f", km)