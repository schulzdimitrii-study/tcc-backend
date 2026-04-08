package br.inatel.tcc.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HordePositionServiceTest {

    private lateinit var service: HordePositionService

    @BeforeEach
    fun setUp() {
        service = HordePositionService()
    }

    @Test
    fun shouldReturnNearZero_whenStartIsNow() {
        val startEpoch = System.currentTimeMillis() / 1000

        val result = service.calculateVirtualPosition(startEpoch, 6.0)

        assertTrue(result >= 0.0)
        assertTrue(result < 0.01, "Distância deveria ser próxima de zero, mas foi $result")
    }

    @Test
    fun shouldCalculateCorrectDistance_after30Minutes() {
        // 1800 segundos = 30 minutos atrás
        val startEpoch = System.currentTimeMillis() / 1000 - 1800

        val result = service.calculateVirtualPosition(startEpoch, 6.0)

        // 30 min / 6.0 min/km = 5.0 km
        assertEquals(5.0, result, 0.05)
    }

    @Test
    fun shouldHandleFastPace() {
        // 600 segundos = 10 minutos atrás
        val startEpoch = System.currentTimeMillis() / 1000 - 600

        val result = service.calculateVirtualPosition(startEpoch, 3.0)

        // 10 min / 3.0 min/km ≈ 3.33 km
        assertEquals(3.33, result, 0.05)
    }

    @Test
    fun shouldHandleSlowPace() {
        // 600 segundos = 10 minutos atrás
        val startEpoch = System.currentTimeMillis() / 1000 - 600

        val result = service.calculateVirtualPosition(startEpoch, 10.0)

        // 10 min / 10.0 min/km = 1.0 km
        assertEquals(1.0, result, 0.05)
    }
}
