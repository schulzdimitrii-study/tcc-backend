package br.inatel.tcc.service

import br.inatel.tcc.dto.GameStateResponse
import br.inatel.tcc.dto.GameStatus
import br.inatel.tcc.service.redis.LeaderboardRedisService
import br.inatel.tcc.service.redis.SessionRedisService
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Publica o game-state de cada sessão ativa com horda a cada 1 segundo.
 *
 * O BiometricWebSocketController já publica o game-state quando o jogador envia dados
 * biométricos (resposta imediata ao movimento). Este scheduler garante que o mobile
 * receba atualizações mesmo quando o jogador está parado — a horda avança com o tempo
 * e o backend é o árbitro único do estado do jogo.
 *
 * Sessões sem horda (hordePace == null) são ignoradas.
 */
@Component
class GameStateScheduler(
    private val leaderboardRedisService: LeaderboardRedisService,
    private val sessionRedisService: SessionRedisService,
    private val hordePositionService: HordePositionService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1000)
    fun broadcastHordeGameState() {
        val activeSessions = leaderboardRedisService.getActiveSessions()
        if (activeSessions.isEmpty()) return

        for (sessionId in activeSessions) {
            val hordePace = leaderboardRedisService.getHordePace(sessionId)
                ?.takeIf { it > 0 } ?: continue   // sessão sem horda → ignora

            val startEpoch = leaderboardRedisService.getSessionStartEpoch(sessionId) ?: continue
            val hordePosition = hordePositionService.calculateVirtualPosition(startEpoch, hordePace)
            val hordeSpeed = 60.0 / hordePace
            val goalDistance = leaderboardRedisService.getGoalDistance(sessionId) ?: 0.0

            val entries = leaderboardRedisService.getFullLeaderboard(sessionId)
                ?.toList() ?: continue

            for (tuple in entries) {
                val userId = tuple.value ?: continue
                val playerPosition = tuple.score ?: 0.0
                val playerSpeed = sessionRedisService
                    .getUserState(sessionId, userId)["speed"]?.toDoubleOrNull() ?: 0.0

                val distanceToGoal = if (goalDistance > 0.0) max(0.0, goalDistance - playerPosition) else 0.0
                val distancePlayerToHorde = abs(playerPosition - hordePosition)
                val raceProgress = if (goalDistance > 0.0) min(100.0, (playerPosition / goalDistance) * 100.0) else 0.0
                val gameStatus = when {
                    hordePosition >= playerPosition                       -> GameStatus.CAUGHT
                    goalDistance > 0.0 && playerPosition >= goalDistance -> GameStatus.ESCAPED
                    else                                                  -> GameStatus.RUNNING
                }

                messagingTemplate.convertAndSend(
                    "/topic/session/$sessionId/game-state",
                    GameStateResponse(
                        sessionId = sessionId,
                        userId = userId,
                        playerPosition = playerPosition,
                        hordePosition = hordePosition,
                        distanceToGoal = distanceToGoal,
                        distancePlayerToHorde = distancePlayerToHorde,
                        playerSpeed = playerSpeed,
                        hordeSpeed = hordeSpeed,
                        raceProgress = raceProgress,
                        gameStatus = gameStatus
                    )
                )
            }

            log.debug(
                "[SCHEDULER] sessionId={} hordePosition={}km entries={}",
                sessionId, hordePosition, entries.size
            )
        }
    }
}
