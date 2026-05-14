package br.inatel.tcc.controller

import br.inatel.tcc.dto.BiometricDataMessage
import br.inatel.tcc.dto.LeaderboardEntryDto
import br.inatel.tcc.dto.LeaderboardResponse
import br.inatel.tcc.service.HordePositionService
import br.inatel.tcc.service.redis.LeaderboardRedisService
import br.inatel.tcc.service.redis.SessionRedisService
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

/**
 * Recebe o fluxo de biometria do Galaxy Watch via WebSocket STOMP e
 * atualiza o estado em tempo real no Redis.
 *
 * Canal de entrada:  /app/treino/dados  (prefixo /app configurado em WebSocketConfig)
 * Canal de saída:    /topic/session/{sessionId}/leaderboard  (broadcast para todos na sessão)
 *
 * Cada operação Redis neste handler leva < 1ms → latência total do handler < 10ms.
 *
 * TODO [FASE 3 - PERSISTÊNCIA BIOMÉTRICA]: Persistir cada BiometricDataMessage no PostgreSQL
 *   de forma assíncrona usando @Async (Spring) ou uma fila em memória (ArrayBlockingQueue)
 *   para não bloquear o fluxo WebSocket. Criar BiometricData entity e associar ao TrainSession.
 *   Referência: domain/biometricdata/BiometricData.kt + BiometricDataRepository
 *
 * TODO [FASE 4 - ZONA CARDÍACA]: Após calcular a zona cardíaca (CardiacZone.kt),
 *   incluí-la no LeaderboardResponse para que o app exiba o ícone de intensidade
 *   de cada competidor em tempo real.
 *   Referência: domain/biometricdata/CardiacZone.kt
 */
@Controller
class BiometricWebSocketController(
    private val sessionRedisService: SessionRedisService,
    private val leaderboardRedisService: LeaderboardRedisService,
    private val hordePositionService: HordePositionService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @MessageMapping("/train/data")
    fun receiveBiometricData(message: BiometricDataMessage) {
        val start = System.currentTimeMillis()

        sessionRedisService.saveUserState(message.sessionId, message.userId, message)
        leaderboardRedisService.updateUserDistance(message.sessionId, message.userId, message.accumulatedDistance)

        val userRank = (leaderboardRedisService.getUserRank(message.sessionId, message.userId) ?: 0L) + 1
        val entries = leaderboardRedisService.getTopEntries(message.sessionId, 10)
            ?.mapIndexed { index, tuple ->
                LeaderboardEntryDto(
                    userId = tuple.value ?: "",
                    rank = index + 1,
                    distanceKm = tuple.score ?: 0.0
                )
            } ?: emptyList()

        val startEpoch = leaderboardRedisService.getSessionStartEpoch(message.sessionId)
        val hordePace = leaderboardRedisService.getHordePace(message.sessionId)
        val hordeVirtualDistance = if (startEpoch != null && hordePace != null && hordePace > 0) {
            hordePositionService.calculateVirtualPosition(startEpoch, hordePace)
        } else {
            null
        }

        val response = LeaderboardResponse(
            sessionId = message.sessionId,
            userRank = userRank.toInt(),
            hordeVirtualDistanceKm = hordeVirtualDistance,
            entries = entries
        )
        messagingTemplate.convertAndSend("/topic/session/${message.sessionId}/leaderboard", response)

        val elapsed = System.currentTimeMillis() - start
        log.info(
            "[BIOMETRIA] sessionId={} userId={} bpm={} distance={}km rank={} horde={}km redis={}ms",
            message.sessionId, message.userId, message.bpm,
            message.accumulatedDistance, userRank, hordeVirtualDistance, elapsed
        )
    }
}
