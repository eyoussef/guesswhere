package com.guesswhere.app.domain

import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

/**
 * Scoring model, GeoGuessr-style: a round starts at [MAX_POINTS] and decays
 * exponentially with distance. Within [NEAR_MISS_KM] the full amount pays.
 */
object Scoring {
    const val MAX_POINTS = 5000
    const val NEAR_MISS_KM = 2.0
    private const val EARTH_RADIUS_KM = 6371.0088

    fun pointsFor(distanceKm: Double, decayKm: Double): Int {
        if (!distanceKm.isFinite() || distanceKm <= NEAR_MISS_KM) return MAX_POINTS
        val pts = MAX_POINTS * exp(-distanceKm / decayKm)
        return pts.roundToInt().coerceIn(0, MAX_POINTS)
    }

    fun haversineKm(a: Geo, b: Geo): Double {
        val lat1 = a.lat * PI / 180.0
        val lat2 = b.lat * PI / 180.0
        val dLat = (b.lat - a.lat) * PI / 180.0
        val dLon = (b.lon - a.lon) * PI / 180.0
        val h = sin(dLat / 2) * sin(dLat / 2) +
            kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun scoreGuess(guess: Geo?, answer: Geo, decayKm: Double): Pair<Double, Int> {
        if (guess == null) return 0.0 to 0
        val km = haversineKm(guess, answer)
        return km to pointsFor(km, decayKm)
    }
}