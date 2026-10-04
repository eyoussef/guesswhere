package com.guesswhere.app.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.guesswhere.app.R
import com.guesswhere.app.domain.Geo
import com.guesswhere.app.domain.Scoring
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private data class MapSignature(
    val pin: Geo? = null,
    val guess: Geo? = null,
    val answer: Geo? = null,
)

/**
 * OpenStreetMap based world map for guess placement and reveal.
 * - Guessing: tap (via [onMapTap]) places/moves the [pin].
 * - Reveal: [guess] vs [answer] markers plus connecting line, auto-fit.
 * Tile cache and User-Agent config happen once, in the Application class.
 */
@Composable
fun OsmMap(
    pin: Geo?,
    onMapTap: ((Geo) -> Unit)?,
    modifier: Modifier = Modifier,
    guess: Geo? = null,
    answer: Geo? = null,
    reveal: Boolean = false,
) {
    val context = LocalContext.current
    val tapHandler = rememberUpdatedState(onMapTap)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(1.6)
            controller.setCenter(GeoPoint(12.0, 12.0))
            overlays.add(
                0,
                MapEventsOverlay(
                    object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            tapHandler.value?.invoke(Geo(p.latitude, p.longitude))
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    },
                ),
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    // Imperative sync: osmdroid markers are not compose nodes. Applied after
    // composition, keyed on the signature so unchanged frames skip the work.
    LaunchedEffect(mapView, pin, guess, answer) {
        mapView.overlays.removeAll { it !is MapEventsOverlay }
        pin?.let { mapView.overlays += guessMarker(mapView, context, it) }
        if (guess != null && answer != null) {
            mapView.overlays += connectingLine(mapView, guess, answer)
        }
        answer?.let { mapView.overlays += targetMarker(mapView, context, it) }
        mapView.invalidate()
    }

    // Reveal: center between both markers and zoom to fit the distance.
    LaunchedEffect(mapView, reveal, guess, answer) {
        val a = answer ?: return@LaunchedEffect
        val g = guess ?: a
        mapView.controller.setZoom(zoomForDistance(Scoring.haversineKm(g, a)))
        mapView.controller.setCenter(
            GeoPoint((g.lat + a.lat) / 2.0, (g.lon + a.lon) / 2.0),
        )
    }

    AndroidView(factory = { mapView }, modifier = modifier, update = { })
}

private fun guessMarker(mapView: MapView, context: Context, geo: Geo): Marker =
    Marker(mapView).apply {
        position = GeoPoint(geo.lat, geo.lon)
        icon = markerIcon(context, R.drawable.pin_guess, 30)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        setOnMarkerClickListener { _, _ -> true } // swallow taps, no info window
    }

private fun targetMarker(mapView: MapView, context: Context, geo: Geo): Marker =
    Marker(mapView).apply {
        position = GeoPoint(geo.lat, geo.lon)
        icon = markerIcon(context, R.drawable.pin_target, 24)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        setOnMarkerClickListener { _, _ -> true }
    }

private fun connectingLine(mapView: MapView, guess: Geo, answer: Geo): Polyline {
    val line = Polyline(mapView)
    line.setPoints(
        listOf(GeoPoint(guess.lat, guess.lon), GeoPoint(answer.lat, answer.lon)),
    )
    line.outlinePaint.color = 0xFF54D5C5.toInt()
    line.outlinePaint.strokeWidth = 5.0f
    line.outlinePaint.isAntiAlias = true
    return line
}

private fun markerIcon(context: Context, @DrawableRes res: Int, sizeDp: Int): Drawable {
    val drawable: Drawable = checkNotNull(ContextCompat.getDrawable(context, res))
    val px = ceil(sizeDp * context.resources.displayMetrics.density).roundToInt()
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, px, px)
    drawable.draw(canvas)
    return BitmapDrawable(context.resources, bitmap)
}

/** Zoom level that frames a given distance across roughly the map's diagonal. */
internal fun zoomForDistance(km: Double): Double {
    val spanKm = max(km * 1.9, 120.0)
    return (ln(42000.0 / spanKm) / ln(2.0) - 1.0).coerceIn(1.5, 10.5)
}