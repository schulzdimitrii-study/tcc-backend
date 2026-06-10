package br.inatel.tcc.service

import br.inatel.tcc.domain.horde.Horde
import br.inatel.tcc.domain.horde.HordeDifficulty
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
    fun shouldKeepHordeStoppedBeforeSevenSecondDelay() {
        val currentEpoch = 1_000L
        val startEpoch = currentEpoch - 6

        val result = service.calculateVirtualPosition(startEpoch, 6.0, currentEpoch)

        assertEquals(0.0, result)
    }

    @Test
    fun shouldStartHordeAfterSevenSecondDelay() {
        val currentEpoch = 1_000L
        val startEpoch = currentEpoch - 67

        val result = service.calculateVirtualPosition(startEpoch, 6.0, currentEpoch)

        assertEquals(1.0 / 6.0, result, 0.01)
    }

    @Test
    fun shouldCalculateCorrectDistance_after30Minutes() {
        val startEpoch = System.currentTimeMillis() / 1000 - 1800

        val result = service.calculateVirtualPosition(startEpoch, 6.0)

        assertEquals(5.0, result, 0.05)
    }

    @Test
    fun shouldHandleFastPace() {
        val startEpoch = System.currentTimeMillis() / 1000 - 600

        val result = service.calculateVirtualPosition(startEpoch, 3.0)

        assertEquals(3.33, result, 0.05)
    }

    @Test
    fun shouldHandleSlowPace() {
        val startEpoch = System.currentTimeMillis() / 1000 - 600

        val result = service.calculateVirtualPosition(startEpoch, 10.0)

        assertEquals(1.0, result, 0.05)
    }

    // ─── resolveEffectivePace ─────────────────────────────────────────────────

    @Test
    fun shouldReturn10_whenDifficultyIsEasy() {
        val horde = Horde(name = "Easy Test", difficulty = HordeDifficulty.EASY, estimatedDuration = 30)
        assertEquals(10.0, service.resolveEffectivePace(horde))
    }

    @Test
    fun shouldReturn8_whenDifficultyIsMedium() {
        val horde = Horde(name = "Medium Test", difficulty = HordeDifficulty.MEDIUM, estimatedDuration = 30)
        assertEquals(8.0, service.resolveEffectivePace(horde))
    }

    @Test
    fun shouldReturn4_whenDifficultyIsHard() {
        val horde = Horde(name = "Hard Test", difficulty = HordeDifficulty.HARD, estimatedDuration = 30)
        assertEquals(4.0, service.resolveEffectivePace(horde))
    }
}
