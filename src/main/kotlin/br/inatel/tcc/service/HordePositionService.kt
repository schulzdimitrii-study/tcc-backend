package br.inatel.tcc.service

import org.springframework.stereotype.Service

/**
 * Calcula a posição virtual da Horda durante uma sessão de treino.
 *
 * A Horda avança autonomamente com base no seu targetPace (Horde.targetPace),
 * simulando um competidor virtual que corre no ritmo alvo da Horda.
 *
 * Fórmula:
 *   distância (km) = tempo_decorrido (min) / targetPace (min/km)
 *
 * Por que calcular aqui e não no Redis?
 *   - O tempo decorrido é derivado do clock atual + start time já no Redis
 *   - Sem I/O adicional — cálculo puramente local (< 1µs)
 *   - Resultado sempre fresco (não fica desatualizado em cache)
 *
 * TODO [FASE 2 - HORDA DINÂMICA]: Suportar hordes com parentHorde (Horde.parentHorde).
 *   A posição de uma sub-horda pode ter um multiplicador ou offset relativo à horda pai.
 *   Referência: domain/horde/Horde.kt (campo parentHorde)
 *
 * TODO [FASE 3 - HORDA ADAPTATIVA]: Implementar modo onde o pace da Horda se adapta
 *   dinamicamente à performance média dos usuários ativos na sessão.
 */
@Service
class HordePositionService {

    /**
     * @param startEpochSeconds Epoch em segundos gravado no Redis ao iniciar a sessão
     * @param targetPaceMinPerKm Pace alvo da Horda em min/km (Horde.targetPace)
     * @return Distância virtual percorrida pela Horda em km
     */
    fun calculateVirtualPosition(startEpochSeconds: Long, targetPaceMinPerKm: Double): Double {
        val elapsedSeconds = (System.currentTimeMillis() / 1000) - startEpochSeconds
        val elapsedMinutes = elapsedSeconds / 60.0
        return elapsedMinutes / targetPaceMinPerKm
    }
}
