package br.inatel.tcc.controller

import br.inatel.tcc.dto.BiometricDataMessage
import br.inatel.tcc.dto.LeaderboardEntryDto
import br.inatel.tcc.dto.LeaderboardResponse
import br.inatel.tcc.dto.GameStateResponse
import br.inatel.tcc.dto.GameStatus
import br.inatel.tcc.service.BiometricPersistenceService
import br.inatel.tcc.service.CardiacZoneService
import br.inatel.tcc.service.HordePositionService
import br.inatel.tcc.service.calculateRaceProgressPercent
import br.inatel.tcc.service.redis.LeaderboardRedisService
import br.inatel.tcc.service.redis.SessionRedisService
import jakarta.validation.Validator
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * Recebe o fluxo de biometria do Galaxy Watch via WebSocket STOMP e
 * atualiza o estado em tempo real no Redis.
 *
 * Canal de entrada:  /app/train/data  (prefixo /app configurado em WebSocketConfig)
 * Canal de saída:    /topic/session/{sessionId}/leaderboard  (broadcast para todos na sessão)
 *
 * Cada operação Redis neste handler leva < 1ms → latência total do handler < 10ms.
 */
@Controller
@Tag(name = "Biometric Data", description = "Biometric Data API")
class BiometricWebSocketController(
    private val sessionRedisService: SessionRedisService,
    private val leaderboardRedisService: LeaderboardRedisService,
    private val hordePositionService: HordePositionService,
    private val messagingTemplate: SimpMessagingTemplate,
    private val validator: Validator,
    private val biometricPersistenceService: BiometricPersistenceService,
    private val cardiacZoneService: CardiacZoneService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "Receive biometric data from a user", responses = [ApiResponse(description = "Biometric data received", content = [Content(mediaType = "application/json", schema = Schema(implementation = BiometricDataMessage::class))])])
    @MessageMapping("/train/data")
    fun receiveBiometricData(message: BiometricDataMessage) {
        val violations = validator.validate(message)
        if (violations.isNotEmpty()) {
            log.warn("[BIOMETRIA] Dados inválidos rejeitados sessionId={}: {}",
                message.sessionId, violations.map { "${it.propertyPath}: ${it.message}" })
            return
        }

        val startNs = System.nanoTime()

        // Persiste no PostgreSQL de forma assíncrona — não bloqueia o handler
        biometricPersistenceService.persistAsync(message)

        // Zona cardíaca: cache hit em Redis (sem DB), null se maxHeartRate não configurado
        val maxHr = cardiacZoneService.getOrCacheMaxHr(message.sessionId, message.userId)
        val zone = if (maxHr != null) cardiacZoneService.calculate(message.bpm, maxHr) else null

        sessionRedisService.saveUserState(message.sessionId, message.userId, message, zone)
        leaderboardRedisService.updateUserDistance(message.sessionId, message.userId, message.accumulatedDistance)

        val userRank = (leaderboardRedisService.getUserRank(message.sessionId, message.userId) ?: 0L) + 1
        val topTuples = leaderboardRedisService.getTopEntries(message.sessionId, 10)?.toList() ?: emptyList()
        val topUserIds = topTuples.map { it.value ?: "" }

        // Zona cardíaca de cada competidor no top-10 (lida do HASH Redis)
        val cardiacZones = sessionRedisService.getCardiacZones(message.sessionId, topUserIds)
        val entries = topTuples.mapIndexed { index, tuple ->
            val entryUserId = tuple.value ?: ""
            LeaderboardEntryDto(
                userId = entryUserId,
                rank = index + 1,
                distanceKm = tuple.score ?: 0.0,
                cardiacZone = cardiacZones[entryUserId]
            )
        }

        // Horda adaptativa: atualiza o pace no Redis com a média dos usuários ativos
        if (leaderboardRedisService.isHordeAdaptive(message.sessionId) && entries.isNotEmpty()) {
            val activeUserIds = entries.map { it.userId }
            val avgPace = sessionRedisService.getAveragePace(message.sessionId, activeUserIds)
            if (avgPace != null && avgPace > 0) {
                leaderboardRedisService.updateHordePace(message.sessionId, avgPace)
            }
        }

        val startEpoch = leaderboardRedisService.getSessionStartEpoch(message.sessionId)
        val hordePace = leaderboardRedisService.getHordePace(message.sessionId)
        val currentEpoch = System.currentTimeMillis() / 1000
        val hordeElapsedSeconds = startEpoch?.let { currentEpoch - it }
        val hordeDelayRemainingSeconds = hordeElapsedSeconds
            ?.let { hordePositionService.getStartDelaySeconds() - it }
            ?.coerceAtLeast(0)
        val hordeVirtualDistance = if (startEpoch != null && hordePace != null && hordePace > 0) {
            hordePositionService.calculateVirtualPosition(startEpoch, hordePace, currentEpoch)
        } else {
            null
        }

        log.info(
            "[HORDE_DELAY] sessionId={} userId={} startEpoch={} currentEpoch={} elapsedSeconds={} delayRemainingSeconds={} hordePace={} hordeDistanceKm={} playerDistanceKm={}",
            message.sessionId,
            message.userId,
            startEpoch,
            currentEpoch,
            hordeElapsedSeconds,
            hordeDelayRemainingSeconds,
            hordePace,
            hordeVirtualDistance,
            message.accumulatedDistance
        )

        val isBehindHorde = hordeVirtualDistance?.let { message.accumulatedDistance < it }
        val distanceToHorde = hordeVirtualDistance?.let { it - message.accumulatedDistance }

        val response = LeaderboardResponse(
            sessionId = message.sessionId,
            userRank = userRank.toInt(),
            hordeVirtualDistanceKm = hordeVirtualDistance,
            entries = entries,
            isBehindHorde = isBehindHorde,
            distanceToHorde = distanceToHorde
        )
        messagingTemplate.convertAndSend("/topic/session/${message.sessionId}/leaderboard", response)

        val goalDistance = leaderboardRedisService.getGoalDistance(message.sessionId) ?: 0.0
        val hordePosition = hordeVirtualDistance ?: 0.0
        val playerPosition = message.accumulatedDistance
        val distanceToGoal = if (goalDistance > 0.0) kotlin.math.max(0.0, goalDistance - playerPosition) else 0.0
        val distancePlayerToHorde = kotlin.math.abs(playerPosition - hordePosition)
        val playerSpeed = message.speed
        val hordeSpeed = if (hordePace != null && hordePace > 0.0) 60.0 / hordePace else 0.0
        val raceProgress = calculateRaceProgressPercent(playerPosition, goalDistance)

        val isDelayActive = hordeElapsedSeconds != null && hordeElapsedSeconds < hordePositionService.getStartDelaySeconds()

        val gameStatus = if (goalDistance > 0.0 && playerPosition >= goalDistance) {
            GameStatus.ESCAPED
        } else if (isDelayActive) {
            GameStatus.RUNNING
        } else if (hordeVirtualDistance != null && hordeVirtualDistance >= playerPosition) {
            GameStatus.CAUGHT
        } else {
            GameStatus.RUNNING
        }

        log.info(
            "[HORDE_STATUS] sessionId={} userId={} playerPositionKm={} hordePositionKm={} distancePlayerToHordeKm={} goalDistanceKm={} raceProgress={} status={}",
            message.sessionId,
            message.userId,
            playerPosition,
            hordePosition,
            distancePlayerToHorde,
            goalDistance,
            raceProgress,
            gameStatus
        )

        val backendProcessingMs = ((System.nanoTime() - startNs) / 1_000_000).coerceAtLeast(0)

        val gameStateResponse = GameStateResponse(
            sessionId = message.sessionId,
            userId = message.userId,
            playerPosition = playerPosition,
            hordePosition = hordePosition,
            distanceToGoal = distanceToGoal,
            distancePlayerToHorde = distancePlayerToHorde,
            playerSpeed = playerSpeed,
            hordeSpeed = hordeSpeed,
            raceProgress = raceProgress,
            gameStatus = gameStatus,
            latencyTraceId = message.latencyTraceId,
            clientSentAtElapsedMs = message.clientSentAtElapsedMs,
            backendProcessingMs = backendProcessingMs,
            serverTimestampMs = System.currentTimeMillis()
        )
        messagingTemplate.convertAndSend("/topic/session/${message.sessionId}/game-state", gameStateResponse)

        log.info(
            "[BIOMETRIA] sessionId={} userId={} bpm={} zone={} distance={}km rank={} horde={}km redis={}ms",
            message.sessionId, message.userId, message.bpm, zone,
            message.accumulatedDistance, userRank, hordeVirtualDistance, backendProcessingMs
        )
        log.info(
            "[LATENCY_BACKEND] sessionId={} userId={} traceId={} backendProcessingMs={}",
            message.sessionId,
            message.userId,
            message.latencyTraceId,
            backendProcessingMs
        )
    }
}
