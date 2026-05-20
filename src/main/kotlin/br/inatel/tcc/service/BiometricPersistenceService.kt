package br.inatel.tcc.service

import br.inatel.tcc.domain.biometricdata.BiometricData
import br.inatel.tcc.domain.biometricdata.BiometricDataRepository
import br.inatel.tcc.domain.trainsession.TrainSessionRepository
import br.inatel.tcc.dto.BiometricDataMessage
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/**
 * Persiste dados biométricos no PostgreSQL de forma assíncrona,
 * sem bloquear o fluxo do WebSocket handler (< 10ms target).
 *
 * Cada chamada a persistAsync() é executada em thread separada do pool de async do Spring.
 * A TrainSession é buscada dentro do método assíncrono para garantir contexto JPA válido.
 */
@Service
class BiometricPersistenceService(
    private val biometricDataRepository: BiometricDataRepository,
    private val trainSessionRepository: TrainSessionRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @Transactional
    fun persistAsync(message: BiometricDataMessage) {
        val sessionId = runCatching { UUID.fromString(message.sessionId) }.getOrNull() ?: return

        val session = trainSessionRepository.findById(sessionId).orElse(null)
        if (session == null) {
            log.warn("[BIOMETRIA-ASYNC] Sessão não encontrada para persistência: {}", message.sessionId)
            return
        }

        biometricDataRepository.save(
            BiometricData(
                timestamp = Instant.ofEpochMilli(message.timestamp)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDateTime(),
                bpm = message.bpm,
                cadence = message.cadence,
                speed = message.speed,
                pace = message.pace,
                accumulatedDistance = message.accumulatedDistance,
                accumulatedCalories = message.accumulatedCalories,
                trainSession = session
            )
        )
    }
}
