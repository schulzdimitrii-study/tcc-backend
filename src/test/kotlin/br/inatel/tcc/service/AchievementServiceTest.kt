package br.inatel.tcc.service

import br.inatel.tcc.domain.achievement.Achievement
import br.inatel.tcc.domain.achievement.AchievementRepository
import br.inatel.tcc.domain.horde.Horde
import br.inatel.tcc.domain.horde.HordeDifficulty
import br.inatel.tcc.domain.trainsession.TrainSession
import br.inatel.tcc.domain.trainsession.TrainSessionRepository
import br.inatel.tcc.domain.user.User
import br.inatel.tcc.domain.userachievement.UserAchievement
import br.inatel.tcc.domain.userachievement.UserAchievementRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.DefaultTypedTuple
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AchievementServiceTest {

    @Mock private lateinit var achievementRepository: AchievementRepository
    @Mock private lateinit var userAchievementRepository: UserAchievementRepository
    @Mock private lateinit var trainSessionRepository: TrainSessionRepository

    @InjectMocks private lateinit var service: AchievementService

    private val userId = UUID.randomUUID()
    private val user = User(id = userId, email = "test@test.com", name = "Test", password = "enc")

    private fun buildAchievement(criterion: String) = Achievement(
        id = UUID.randomUUID(), title = "Achievement $criterion", criterion = criterion
    )

    private fun buildSession(horde: Horde? = null) = TrainSession(
        id = UUID.randomUUID(), user = user, horde = horde
    )

    private fun buildHorde() = Horde(
        id = UUID.randomUUID(), name = "Horda", difficulty = HordeDifficulty.MEDIUM, estimatedDuration = 30
    )

    // ─── FIRST_RUN ────────────────────────────────────────────────────────────

    @Test
    fun shouldGrantFirstRun_whenFirstCompletedSession() {
        val achievement = buildAchievement("FIRST_RUN")
        whenever(achievementRepository.findByActive(true)).thenReturn(listOf(achievement))
        whenever(userAchievementRepository.existsByIdUserIdAndIdAchievementId(userId, achievement.id!!)).thenReturn(false)
        whenever(trainSessionRepository.countByUser_IdAndEndDateIsNotNull(userId)).thenReturn(1L)
        whenever(userAchievementRepository.save(any<UserAchievement>())).thenAnswer { it.arguments[0] }

        service.verifyAndGrant(user, buildSession(), emptySet())

        verify(userAchievementRepository).save(any<UserAchievement>())
    }

    @Test
    fun shouldNotGrantFirstRun_whenMultipleSessionsCompleted() {
        val achievement = buildAchievement("FIRST_RUN")
        whenever(achievementRepository.findByActive(true)).thenReturn(listOf(achievement))
        whenever(userAchievementRepository.existsByIdUserIdAndIdAchievementId(userId, achievement.id!!)).thenReturn(false)
        whenever(trainSessionRepository.countByUser_IdAndEndDateIsNotNull(userId)).thenReturn(3L)

        service.verifyAndGrant(user, buildSession(), emptySet())

        verify(userAchievementRepository, never()).save(any())
    }

    // ─── DISTANCE_5KM ─────────────────────────────────────────────────────────

    @Test
    fun shouldGrantDistance5km_whenUserRanFiveOrMoreKm() {
        val achievement = buildAchievement("DISTANCE_5KM")
        val leaderboard = linkedSetOf(DefaultTypedTuple(userId.toString(), 5.0))
        whenever(achievementRepository.findByActive(true)).thenReturn(listOf(achievement))
        whenever(userAchievementRepository.existsByIdUserIdAndIdAchievementId(userId, achievement.id!!)).thenReturn(false)
        whenever(userAchievementRepository.save(any<UserAchievement>())).thenAnswer { it.arguments[0] }

        service.verifyAndGrant(user, buildSession(), leaderboard)

        verify(userAchievementRepository).save(any<UserAchievement>())
    }

    @Test
    fun shouldNotGrantDistance5km_whenUserRanLessThanFiveKm() {
        val achievement = buildAchievement("DISTANCE_5KM")
        val leaderboard = linkedSetOf(DefaultTypedTuple(userId.toString(), 4.9))
        whenever(achievementRepository.findByActive(true)).thenReturn(listOf(achievement))
        whenever(userAchievementRepository.existsByIdUserIdAndIdAchievementId(userId, achievement.id!!)).thenReturn(false)

        service.verifyAndGrant(user, buildSession(), leaderboard)

        verify(userAchievementRepository, never()).save(any())
    }

    // ─── HORDE_TOP_1 ──────────────────────────────────────────────────────────

    @Test
    fun shouldGrantHordeTop1_whenUserFinishedFirstWithHorde() {
        val achievement = buildAchievement("HORDE_TOP_1")
        val leaderboard = linkedSetOf(
            DefaultTypedTuple(userId.toString(), 5.0),
            DefaultTypedTuple(UUID.randomUUID().toString(), 3.0)
        )
        whenever(achievementRepository.findByActive(true)).thenReturn(listOf(achievement))
        whenever(userAchievementRepository.existsByIdUserIdAndIdAchievementId(userId, achievement.id!!)).thenReturn(false)
        whenever(userAchievementRepository.save(any<UserAchievement>())).thenAnswer { it.arguments[0] }

        service.verifyAndGrant(user, buildSession(horde = buildHorde()), leaderboard)

        verify(userAchievementRepository).save(any<UserAchievement>())
    }

    @Test
    fun shouldNotGrantHordeTop1_whenSessionHasNoHorde() {
        val achievement = buildAchievement("HORDE_TOP_1")
        val leaderboard = linkedSetOf(DefaultTypedTuple(userId.toString(), 5.0))
        whenever(achievementRepository.findByActive(true)).thenReturn(listOf(achievement))
        whenever(userAchievementRepository.existsByIdUserIdAndIdAchievementId(userId, achievement.id!!)).thenReturn(false)

        service.verifyAndGrant(user, buildSession(horde = null), leaderboard)

        verify(userAchievementRepository, never()).save(any())
    }

    @Test
    fun shouldNotGrantHordeTop1_whenUserIsNotFirst() {
        val achievement = buildAchievement("HORDE_TOP_1")
        val otherId = UUID.randomUUID()
        val leaderboard = linkedSetOf(
            DefaultTypedTuple(otherId.toString(), 6.0),
            DefaultTypedTuple(userId.toString(), 5.0)
        )
        whenever(achievementRepository.findByActive(true)).thenReturn(listOf(achievement))
        whenever(userAchievementRepository.existsByIdUserIdAndIdAchievementId(userId, achievement.id!!)).thenReturn(false)

        service.verifyAndGrant(user, buildSession(horde = buildHorde()), leaderboard)

        verify(userAchievementRepository, never()).save(any())
    }

    // ─── Casos gerais ─────────────────────────────────────────────────────────

    @Test
    fun shouldNotGrant_whenAchievementAlreadyConceded() {
        val achievement = buildAchievement("FIRST_RUN")
        whenever(achievementRepository.findByActive(true)).thenReturn(listOf(achievement))
        whenever(userAchievementRepository.existsByIdUserIdAndIdAchievementId(userId, achievement.id!!)).thenReturn(true)

        service.verifyAndGrant(user, buildSession(), emptySet())

        verify(userAchievementRepository, never()).save(any())
    }

    @Test
    fun shouldDoNothing_whenNoActiveAchievements() {
        whenever(achievementRepository.findByActive(true)).thenReturn(emptyList())

        service.verifyAndGrant(user, buildSession(), emptySet())

        verify(userAchievementRepository, never()).save(any())
    }

    @Test
    fun shouldIgnoreUnknownCriterion() {
        val achievement = buildAchievement("UNKNOWN_CRITERION")
        whenever(achievementRepository.findByActive(true)).thenReturn(listOf(achievement))
        whenever(userAchievementRepository.existsByIdUserIdAndIdAchievementId(userId, achievement.id!!)).thenReturn(false)

        service.verifyAndGrant(user, buildSession(), emptySet())

        verify(userAchievementRepository, never()).save(any())
    }
}
