package br.inatel.tcc.service

import br.inatel.tcc.domain.achievement.AchievementRepository
import br.inatel.tcc.domain.trainsession.TrainSession
import br.inatel.tcc.domain.trainsession.TrainSessionRepository
import br.inatel.tcc.domain.user.User
import br.inatel.tcc.domain.userachievement.UserAchievement
import br.inatel.tcc.domain.userachievement.UserAchievementId
import br.inatel.tcc.domain.userachievement.UserAchievementRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Verifica e concede conquistas ao usuário ao encerrar uma sessão de treino.
 *
 * Critérios suportados (campo Achievement.criterion):
 *   FIRST_RUN    — primeira sessão completada pelo usuário
 *   DISTANCE_5KM — correu ≥ 5km na sessão
 *   HORDE_TOP_1  — terminou em 1º lugar com horda ativa
 *
 * Apenas achievements com active=true são avaliados.
 * Achievements já concedidos anteriormente são ignorados (idempotente).
 */
@Service
class AchievementService(
    private val achievementRepository: AchievementRepository,
    private val userAchievementRepository: UserAchievementRepository,
    private val trainSessionRepository: TrainSessionRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun verifyAndGrant(
        user: User,
        session: TrainSession,
        finalLeaderboard: Set<ZSetOperations.TypedTuple<String>>?
    ) {
        val activeAchievements = achievementRepository.findByActive(true)
        if (activeAchievements.isEmpty()) return

        for (achievement in activeAchievements) {
            val achievementId = achievement.id ?: continue
            val userId = user.id ?: continue

            if (userAchievementRepository.existsByIdUserIdAndIdAchievementId(userId, achievementId)) continue

            val shouldGrant = when (achievement.criterion) {
                "FIRST_RUN"    -> isFirstRun(user)
                "DISTANCE_5KM" -> hasRunFiveKm(user, finalLeaderboard)
                "HORDE_TOP_1"  -> isHordeTopOne(user, session, finalLeaderboard)
                else           -> false
            }

            if (shouldGrant) {
                userAchievementRepository.save(
                    UserAchievement(
                        id = UserAchievementId(userId = userId, achievementId = achievementId),
                        user = user,
                        achievement = achievement
                    )
                )
                log.info("[ACHIEVEMENT] Conquista '{}' concedida ao usuário {}", achievement.title, user.email)
            }
        }
    }

    /** Verifica se esta foi a primeira sessão completada do usuário. */
    private fun isFirstRun(user: User): Boolean =
        trainSessionRepository.countByUser_IdAndEndDateIsNotNull(user.id!!) == 1L

    /** Verifica se o usuário percorreu pelo menos 5km nesta sessão. */
    private fun hasRunFiveKm(user: User, leaderboard: Set<ZSetOperations.TypedTuple<String>>?): Boolean {
        val distance = leaderboard
            ?.firstOrNull { it.value == user.id.toString() }
            ?.score ?: 0.0
        return distance >= 5.0
    }

    /** Verifica se o usuário terminou em 1º lugar em uma sessão com horda. */
    private fun isHordeTopOne(
        user: User,
        session: TrainSession,
        leaderboard: Set<ZSetOperations.TypedTuple<String>>?
    ): Boolean {
        if (session.horde == null) return false
        val topEntry = leaderboard?.firstOrNull() ?: return false
        return topEntry.value == user.id.toString()
    }
}
