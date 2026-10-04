package com.guesswhere.app.domain

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure scoring math: known distances, monotonic decay, clamps, skips. */
class ScoringTest {

    private val lisbon = Geo(38.7223, -9.1393)
    private val london = Geo(51.5074, -0.1278)
    private val sydney = Geo(-33.8688, 151.2093)

    private val newyork = Geo(40.7128, -74.0060)

    @Test
    fun haversine_matches_known_distances() {
        // London to New York is roughly 5570 km.
        val km = Scoring.haversineKm(london, newyork)
        assertEquals(5570.0, km, 90.0)
    }

    @Test
    fun haversine_of_identical_points_is_zero() {
        assertEquals(0.0, Scoring.haversineKm(lisbon, lisbon.copy(lat = lisbon.lat + 1e-9)), 1e-6)
    }

    @Test
    fun antipodal_pair_is_bounded() {
        // Sydney vs Azores: farthest plausible pairing, must stay < half circumference.
        val km = Scoring.haversineKm(sydney, lisbon)
        assertTrue(km in 0.0..20015.1)
        assertTrue(km > 10000.0)
    }

    @Test
    fun close_guess_pays_full_points() {
        assertEquals(Scoring.MAX_POINTS, Scoring.pointsFor(1.0, DecayKm.NORMAL))
        assertEquals(Scoring.MAX_POINTS, Scoring.pointsFor(Scoring.NEAR_MISS_KM, DecayKm.HARD))
    }

    @Test
    fun score_decays_monotonically_and_clamps_to_zero() {
        val decay = DecayKm.NORMAL
        assertTrue(
            Scoring.pointsFor(10.0, decay) > Scoring.pointsFor(1000.0, decay),
        )
        assertTrue(
            Scoring.pointsFor(1000.0, decay) > Scoring.pointsFor(8000.0, decay),
        )
        assertEquals(0, Scoring.pointsFor(200_000.0, decay))
        assertTrue(Scoring.pointsFor(8000.0, decay) > 0)
    }

    @Test
    fun tighter_decay_pays_less_at_distance() {
        val at1000Normal = Scoring.pointsFor(1000.0, DecayKm.NORMAL)
        val at1000Hard = Scoring.pointsFor(1000.0, DecayKm.HARD)
        assertTrue(at1000Hard < at1000Normal)
        // Easy should pay more than normal at the same distance.
        assertTrue(at1000Normal < Scoring.pointsFor(1000.0, DecayKm.EASY))
    }

    @Test
    fun skipped_guess_scores_zero() {
        val (km, pts) = Scoring.scoreGuess(null, lisbon, DecayKm.NORMAL)
        assertEquals(0.0, km, 1e-9)
        assertEquals(0, pts)
    }

    @Test
    fun score_guess_pair_is_consistent_with_parts() {
        val guess = Geo(50.0, 8.0)
        val (km, pts) = Scoring.scoreGuess(guess, london, DecayKm.NORMAL)
        assertEquals(Scoring.haversineKm(guess, london), km, 1e-9)
        assertEquals(Scoring.pointsFor(km, DecayKm.NORMAL), pts)
    }

    @Test
    fun points_and_distance_are_stable_near_antimeridian() {
        val west = Geo(0.0, 179.5)
        val east = Geo(0.0, -179.5)
        val km = Scoring.haversineKm(west, east)
        // Crossing the antimeridian should be ~111 km, not ~40_000 km.
        assertTrue(abs(km - 111.2) < 2.0)
    }

    private object DecayKm {
        val NORMAL = Difficulty.NORMAL.decayKm
        val HARD = Difficulty.HARD.decayKm
        val EASY = Difficulty.EASY.decayKm
    }
}