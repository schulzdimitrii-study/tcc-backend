package br.inatel.tcc.service

import br.inatel.tcc.domain.horde.Horde
import br.inatel.tcc.domain.horde.HordeDifficulty
import br.inatel.tcc.domain.horde.HordeRepository
import br.inatel.tcc.domain.ranking.RankingRepository
import br.inatel.tcc.domain.trainsession.TrainSession
import br.inatel.tcc.domain.trainsession.TrainSessionRepository
import br.inatel.tcc.domain.trainsession.TrainType
import br.inatel.tcc.domain.user.User
import br.inatel.tcc.domain.user.UserRepository
import br.inatel.tcc.dto.StartSessionRequest
import br.inatel.tcc.service.redis.LeaderboardRedisService
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.DefaultTypedTuple
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TrainSessionServiceTest {

    @Mock private lateinit var trainSessionRepository: TrainSessionRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var hordeRepository: HordeRepository
    @Mock private lateinit var rankingRepository: RankingRepository
    @Mock private lateinit var leaderboardRedisService: LeaderboardRedisService

    @InjectMocks private lateinit var service: TrainSessionService

    private val userId = UUID.randomUUID()
    private val sessionId = UUID.randomUUID()

    private fun buildUser(id: UUID = userId) = User(
        id = id, email = "test@example.com", name = "Test User", password = "encoded"
    )

    private fun buildSession(horde: Horde? = null) = TrainSession(
        id = sessionId, user = buildUser(), horde = horde, trainType = TrainType.RUN
    )

    // ─── startSession ─────────────────────────────────────────────────────────

    @Test
    fun shouldStartSessionWithoutHorde() {
        val savedSession = buildSession()
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(buildUser()))
        whenever(trainSessionRepository.save(any())).thenReturn(savedSession)

        val response = service.startSession("test@example.com", StartSessionRequest(trainType = "RUN"))

        assertNotNull(response.sessionId)
        verify(leaderboardRedisService).initSession(sessionId.toString(), null)
    }

    @Test
    fun shouldStartSessionWithHorde() {
        val hordeId = UUID.randomUUID()
        val horde = Horde(
            id = hordeId, name = "Horda Teste",
            difficulty = HordeDifficulty.MEDIUM, estimatedDuration = 60, targetPace = 6.0
        )
        val savedSession = buildSession(horde)
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(buildUser()))
        whenever(hordeRepository.findById(hordeId)).thenReturn(Optional.of(horde))
        whenever(trainSessionRepository.save(any())).thenReturn(savedSession)

        val response = service.startSession("test@example.com", StartSessionRequest(hordeId = hordeId))

        assertNotNull(response.sessionId)
        verify(leaderboardRedisService).initSession(sessionId.toString(), 6.0)
    }

    @Test
    fun shouldThrowWhenUserNotFound() {
        whenever(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty())

        assertThrows(IllegalArgumentException::class.java) {
            service.startSession("unknown@example.com", StartSessionRequest())
        }
    }

    @Test
    fun shouldThrowWhenHordeNotFound() {
        val hordeId = UUID.randomUUID()
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(buildUser()))
        whenever(hordeRepository.findById(hordeId)).thenReturn(Optional.empty())

        assertThrows(IllegalArgumentException::class.java) {
            service.startSession("test@example.com", StartSessionRequest(hordeId = hordeId))
        }
    }

    @Test
    fun shouldDefaultToRUN_forInvalidTrainType() {
        val savedSession = buildSession()
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(buildUser()))
        whenever(trainSessionRepository.save(any())).thenReturn(savedSession)

        val response = service.startSession("test@example.com", StartSessionRequest(trainType = "TIPO_INVALIDO"))

        assertNotNull(response.sessionId)
        verify(trainSessionRepository).save(any<TrainSession>())
    }

    // ─── endSession ───────────────────────────────────────────────────────────

    @Test
    fun shouldEndSessionSuccessfully() {
        val user2Id = UUID.randomUUID()
        val user2 = User(id = user2Id, email = "other@example.com", name = "Other", password = "enc")
        val sess = buildSession()

        val leaderboard = linkedSetOf(
            DefaultTypedTuple(userId.toString(), 5.0),
            DefaultTypedTuple(user2Id.toString(), 3.0)
        )

        whenever(trainSessionRepository.findById(sessionId)).thenReturn(Optional.of(sess))
        whenever(leaderboardRedisService.getFullLeaderboard(sessionId.toString())).thenReturn(leaderboard)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(buildUser()))
        whenever(userRepository.findById(user2Id)).thenReturn(Optional.of(user2))
        whenever(rankingRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(trainSessionRepository.save(any())).thenAnswer { it.arguments[0] }

        service.endSession(sessionId.toString())

        verify(rankingRepository, times(2)).save(any())
        verify(trainSessionRepository).save(any<TrainSession>())
        verify(leaderboardRedisService).expireSessionKeys(sessionId.toString())
    }

    @Test
    fun shouldEndSessionWithNullLeaderboard() {
        val sess = buildSession()
        whenever(trainSessionRepository.findById(sessionId)).thenReturn(Optional.of(sess))
        whenever(leaderboardRedisService.getFullLeaderboard(sessionId.toString())).thenReturn(null)
        whenever(trainSessionRepository.save(any())).thenAnswer { it.arguments[0] }

        service.endSession(sessionId.toString())

        verify(rankingRepository, never()).save(any())
        verify(leaderboardRedisService).expireSessionKeys(sessionId.toString())
    }

    @Test
    fun shouldThrowWhenSessionNotFound() {
        whenever(trainSessionRepository.findById(sessionId)).thenReturn(Optional.empty())

        assertThrows(IllegalArgumentException::class.java) {
            service.endSession(sessionId.toString())
        }
    }

    @Test
    fun shouldExpireRedisKeys_onEnd() {
        val sess = buildSession()
        whenever(trainSessionRepository.findById(sessionId)).thenReturn(Optional.of(sess))
        whenever(leaderboardRedisService.getFullLeaderboard(sessionId.toString())).thenReturn(emptySet())
        whenever(trainSessionRepository.save(any())).thenAnswer { it.arguments[0] }

        service.endSession(sessionId.toString())

        verify(leaderboardRedisService).expireSessionKeys(sessionId.toString())
    }
}
