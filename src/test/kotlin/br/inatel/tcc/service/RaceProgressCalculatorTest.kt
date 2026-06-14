package br.inatel.tcc.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RaceProgressCalculatorTest {
    @Test
    fun shouldCalculateRaceProgressPercentForNormalDistances() {
        assertEquals(0.0, calculateRaceProgressPercent(0.0, 10.0))
        assertEquals(20.0, calculateRaceProgressPercent(2.0, 10.0))
        assertEquals(100.0, calculateRaceProgressPercent(10.0, 10.0))
    }

    @Test
    fun shouldClampRaceProgressPercentToValidRange() {
        assertEquals(100.0, calculateRaceProgressPercent(12.0, 10.0))
        assertEquals(0.0, calculateRaceProgressPercent(-1.0, 10.0))
    }

    @Test
    fun shouldReturnZeroForInvalidRaceProgressInputs() {
        assertEquals(0.0, calculateRaceProgressPercent(1.0, 0.0))
        assertEquals(0.0, calculateRaceProgressPercent(1.0, -10.0))
        assertEquals(0.0, calculateRaceProgressPercent(Double.NaN, 10.0))
        assertEquals(0.0, calculateRaceProgressPercent(Double.POSITIVE_INFINITY, 10.0))
        assertEquals(0.0, calculateRaceProgressPercent(1.0, Double.NaN))
        assertEquals(0.0, calculateRaceProgressPercent(1.0, Double.POSITIVE_INFINITY))
    }
}
