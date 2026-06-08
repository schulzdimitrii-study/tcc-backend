package br.inatel.tcc.service

import br.inatel.tcc.domain.horde.Horde
import br.inatel.tcc.domain.horde.HordeDifficulty
import org.springframework.stereotype.Service

@Service
class HordePositionService {
    fun getStartDelaySeconds(): Long = HORDE_START_DELAY_SECONDS

    fun calculateVirtualPosition(
        startEpochSeconds: Long,
        targetPaceMinPerKm: Double
    ): Double {
        return calculateVirtualPosition(
            startEpochSeconds,
            targetPaceMinPerKm,
            System.currentTimeMillis() / 1000
        )
    }

    fun calculateVirtualPosition(
        startEpochSeconds: Long,
        targetPaceMinPerKm: Double,
        currentEpochSeconds: Long
    ): Double {
        val elapsedSeconds = (currentEpochSeconds - startEpochSeconds - HORDE_START_DELAY_SECONDS)
            .coerceAtLeast(0)
        val elapsedMinutes = elapsedSeconds / 60.0
        return elapsedMinutes / targetPaceMinPerKm
    }

    fun resolveEffectivePace(horde: Horde): Double? {
        return horde.targetPace
            ?: horde.parentHorde?.targetPace
            ?: when (horde.difficulty) {
                HordeDifficulty.EASY   -> 10.0
                HordeDifficulty.MEDIUM -> 8.0
                HordeDifficulty.HARD   -> 4.0
            }
    }

    private companion object {
        const val HORDE_START_DELAY_SECONDS = 5L
    }
}
