package br.inatel.tcc.service

import br.inatel.tcc.domain.horde.Horde
import br.inatel.tcc.domain.horde.HordeDifficulty
import org.springframework.stereotype.Service

@Service
class HordePositionService {
    fun calculateVirtualPosition(startEpochSeconds: Long, targetPaceMinPerKm: Double): Double {
        val elapsedSeconds = (System.currentTimeMillis() / 1000) - startEpochSeconds
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
}
