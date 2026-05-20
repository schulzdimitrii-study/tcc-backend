package br.inatel.tcc.service

import br.inatel.tcc.domain.ranking.Ranking
import br.inatel.tcc.domain.ranking.RankingRepository
import br.inatel.tcc.domain.trainsession.TrainSession
import br.inatel.tcc.domain.trainsession.TrainSessionRepository
import br.inatel.tcc.domain.trainsession.TrainType
import br.inatel.tcc.domain.horde.HordeRepository
import br.inatel.tcc.domain.user.UserRepository
import br.inatel.tcc.dto.LeaderboardEntryDto
import br.inatel.tcc.dto.StartSessionRequest
import br.inatel.tcc.dto.StartSessionResponse
import br.inatel.tcc.service.redis.LeaderboardRedisService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Gerencia o ciclo de vida de uma sessão de treino.
 *
 * Responsabilidades:
 *   1. startSession: Cria TrainSession no PostgreSQL + inicializa keys Redis
 *   2. endSession: Lê leaderboard final do Redis → persiste no PostgreSQL → expira keys Redis
 *
 * Padrão de persistência:
 *   Durante a corrida → apenas Redis (< 1ms por operação)
 *   Ao encerrar       → flush para PostgreSQL (rankings + sessão)
 *
 * TODO [FASE 4 - AMIGOS]: Ao encerrar, notificar amigos do usuário via WebSocket
 *   sobre o resultado da sessão.
 *   Referência: domain/friendship/FriendshipRepository.kt
 *
 * TODO [FASE 5 - CHECKPOINTS]: Implementar flush incremental no PostgreSQL a cada 5 minutos
 *   como checkpoint (caso o app feche inesperadamente e a sessão não seja encerrada).
 */
@Service
class TrainSessionService(
    private val trainSessionRepository: TrainSessionRepository,
    private val userRepository: UserRepository,
    private val hordeRepository: HordeRepository,
    private val rankingRepository: RankingRepository,
    private val leaderboardRedisService: LeaderboardRedisService,
    private val hordePositionService: HordePositionService,
    private val achievementService: AchievementService
) {

    @Transactional
    fun startSession(userEmail: String, request: StartSessionRequest): StartSessionResponse {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { IllegalArgumentException("Usuário não encontrado: $userEmail") }

        val horde = request.hordeId?.let {
            hordeRepository.findById(it)
                .orElseThrow { IllegalArgumentException("Horda não encontrada: $it") }
        }

        val trainType = runCatching { TrainType.valueOf(request.trainType.uppercase()) }
            .getOrDefault(TrainType.RUN)

        val session = trainSessionRepository.save(
            TrainSession(
                user = user,
                horde = horde,
                trainType = trainType,
                startDate = LocalDateTime.now()
            )
        )

        val sessionId = session.id.toString()

        leaderboardRedisService.initSession(
            sessionId,
            horde?.let { hordePositionService.resolveEffectivePace(it) },
            horde?.isAdaptive ?: false
        )

        return StartSessionResponse(sessionId = sessionId)
    }

    @Transactional
    fun endSession(sessionId: String) {
        val id = UUID.fromString(sessionId)
        val session = trainSessionRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Sessão não encontrada: $sessionId") }

        val finalLeaderboard = leaderboardRedisService.getFullLeaderboard(sessionId)
        val period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

        finalLeaderboard?.forEachIndexed { index, entry ->
            val userId = UUID.fromString(entry.value ?: return@forEachIndexed)
            val distanceKm = entry.score ?: 0.0
            val user = userRepository.findById(userId).orElse(null) ?: return@forEachIndexed

            rankingRepository.save(
                Ranking(
                    user = user,
                    position = index + 1,
                    score = distanceKm,
                    period = period,
                    calculeDate = LocalDate.now()
                )
            )
        }

        val ownerDistance = finalLeaderboard
            ?.firstOrNull { it.value == session.user.id.toString() }
            ?.score

        val updatedSession = trainSessionRepository.save(
            TrainSession(
                id = session.id,
                user = session.user,
                horde = session.horde,
                trainType = session.trainType,
                startDate = session.startDate,
                endDate = LocalDateTime.now(),
                totalDistance = ownerDistance,
                estimatedCalories = session.estimatedCalories,
                biometricData = session.biometricData
            )
        )

        leaderboardRedisService.expireSessionKeys(sessionId)

        achievementService.verifyAndGrant(session.user, updatedSession, finalLeaderboard)
    }

    fun getLeaderboard(sessionId: String): List<LeaderboardEntryDto> {
        val entries = leaderboardRedisService.getTopEntries(sessionId) ?: return emptyList()
        return entries.mapIndexed { index, tuple ->
            LeaderboardEntryDto(
                userId = tuple.value ?: "",
                rank = index + 1,
                distanceKm = tuple.score ?: 0.0
            )
        }
    }

    fun getSession(sessionId: String): TrainSession {
        val id = UUID.fromString(sessionId)
        return trainSessionRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Sessão não encontrada: $sessionId") }
    }
}
