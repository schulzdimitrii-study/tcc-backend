package br.inatel.tcc.service

import br.inatel.tcc.domain.horde.Horde
import org.springframework.stereotype.Service

@Service
class HordePositionService {
    fun calculateVirtualPosition(startEpochSeconds: Long, targetPaceMinPerKm: Double): Double {
        val elapsedSeconds = (System.currentTimeMillis() / 1000) - startEpochSeconds
        val elapsedMinutes = elapsedSeconds / 60.0
        return elapsedMinutes / targetPaceMinPerKm
    }

    fun resolveEffectivePace(horde: Horde): Double? {
        return horde.targetPace ?: horde.parentHorde?.targetPace
    }
}
