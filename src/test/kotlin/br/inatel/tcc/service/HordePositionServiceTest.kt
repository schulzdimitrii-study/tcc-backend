package br.inatel.tcc.service

import br.inatel.tcc.domain.horde.Horde
import br.inatel.tcc.domain.horde.HordeDifficulty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HordePositionServiceTest {

    private lateinit var service: HordePositionService

    @BeforeEach
    fun setUp() {
        service = HordePositionService()
    }

    private fun buildHorde(targetPace: Double? = null, parentHorde: Horde? = null) =
        Horde(name = "Test", difficulty = HordeDifficulty.MEDIUM, estimatedDuration = 30,
            targetPace = targetPace, parentHorde = parentHorde)

    @Test
    fun shouldReturnNearZero_whenStartIsNow() {
        val startEpoch = System.currentTimeMillis() / 1000

        val result = service.calculateVirtualPosition(startEpoch, 6.0)

        assertTrue(result >= 0.0)
        assertTrue(result < 0.01, "Distância deveria ser próxima de zero, mas foi $result")
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
    fun shouldReturnOwnPace_whenHordeHasTargetPace() {
        val horde = buildHorde(targetPace = 6.0)
        assertEquals(6.0, service.resolveEffectivePace(horde))
    }

    @Test
    fun shouldReturnParentPace_whenSubHordeHasNoPace() {
        val parent = buildHorde(targetPace = 5.0)
        val subHorde = buildHorde(targetPace = null, parentHorde = parent)
        assertEquals(5.0, service.resolveEffectivePace(subHorde))
    }

    @Test
    fun shouldPreferOwnPace_overParentPace() {
        val parent = buildHorde(targetPace = 5.0)
        val subHorde = buildHorde(targetPace = 7.0, parentHorde = parent)
        assertEquals(7.0, service.resolveEffectivePace(subHorde))
    }

    @Test
    fun shouldReturnNull_whenStandaloneHordeHasNoPace() {
        val horde = buildHorde(targetPace = null)
        assertNull(service.resolveEffectivePace(horde))
    }

    @Test
    fun shouldReturnNull_whenBothHordeAndParentHaveNoPace() {
        val parent = buildHorde(targetPace = null)
        val subHorde = buildHorde(targetPace = null, parentHorde = parent)
        assertNull(service.resolveEffectivePace(subHorde))
    }

    @Test
    fun shouldMultiplyPaceBy12_whenDifficultyIsEasy() {
        val horde = Horde(name = "Easy Test", difficulty = HordeDifficulty.EASY, estimatedDuration = 30, targetPace = 5.0)
        assertEquals(6.0, service.resolveEffectivePace(horde))
    }

    @Test
    fun shouldKeepPaceUnchanged_whenDifficultyIsMedium() {
        val horde = Horde(name = "Medium Test", difficulty = HordeDifficulty.MEDIUM, estimatedDuration = 30, targetPace = 5.0)
        assertEquals(5.0, service.resolveEffectivePace(horde))
    }

    @Test
    fun shouldMultiplyPaceBy08_whenDifficultyIsHard() {
        val horde = Horde(name = "Hard Test", difficulty = HordeDifficulty.HARD, estimatedDuration = 30, targetPace = 5.0)
        assertEquals(4.0, service.resolveEffectivePace(horde))
    }
}
