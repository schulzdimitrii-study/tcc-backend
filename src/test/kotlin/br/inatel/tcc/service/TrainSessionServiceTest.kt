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
import br.inatel.tcc.domain.friendship.Friendship
import br.inatel.tcc.domain.friendship.FriendshipRepository
import br.inatel.tcc.domain.friendship.FriendshipStatus
import br.inatel.tcc.dto.StartSessionRequest
import br.inatel.tcc.service.redis.LeaderboardRedisService
import br.inatel.tcc.service.AchievementService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.DefaultTypedTuple
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TrainSessionServiceTest {

    @Mock private lateinit var trainSessionRepository: TrainSessionRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var hordeRepository: HordeRepository
    @Mock private lateinit var rankingRepository: RankingRepository
    @Mock private lateinit var leaderboardRedisService: LeaderboardRedisService
    @Mock private lateinit var hordePositionService: HordePositionService
    @Mock private lateinit var achievementService: AchievementService
    @Mock private lateinit var friendshipRepository: FriendshipRepository
    @Mock private lateinit var messagingTemplate: SimpMessagingTemplate

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
        verify(leaderboardRedisService).initSession(sessionId.toString(), null, false, null)
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
        whenever(hordePositionService.resolveEffectivePace(horde)).thenReturn(6.0)
 
        val response = service.startSession("test@example.com", StartSessionRequest(hordeId = hordeId))
 
        assertNotNull(response.sessionId)
        verify(leaderboardRedisService).initSession(sessionId.toString(), 6.0, false, 60)
    }
 
    @Test
    fun shouldInheritParentPace_forSubHordeWithoutOwnPace() {
        val parentId = UUID.randomUUID()
        val subHordeId = UUID.randomUUID()
        val parent = Horde(id = parentId, name = "Horda Pai", difficulty = HordeDifficulty.EASY, estimatedDuration = 30, targetPace = 5.0)
        val subHorde = Horde(id = subHordeId, name = "Sub-Horda", difficulty = HordeDifficulty.MEDIUM, estimatedDuration = 30, targetPace = null, parentHorde = parent)
        val savedSession = buildSession(subHorde)
        whenever(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(buildUser()))
        whenever(hordeRepository.findById(subHordeId)).thenReturn(Optional.of(subHorde))
        whenever(trainSessionRepository.save(any())).thenReturn(savedSession)
        whenever(hordePositionService.resolveEffectivePace(subHorde)).thenReturn(5.0)
 
        service.startSession("test@example.com", StartSessionRequest(hordeId = subHordeId))
 
        verify(leaderboardRedisService).initSession(sessionId.toString(), 5.0, false, 30)
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
        verify(leaderboardRedisService).incrementGlobalScore(any(), eq(userId.toString()), eq(5.0))
        verify(leaderboardRedisService).incrementGlobalScore(any(), eq(user2Id.toString()), eq(3.0))
        verify(leaderboardRedisService).expireSessionKeys(sessionId.toString())
    }

    @Test
    fun shouldNotIncrementGlobalScore_forZeroDistance() {
        val sess = buildSession()
        val leaderboard = linkedSetOf(DefaultTypedTuple(userId.toString(), 0.0))

        whenever(trainSessionRepository.findById(sessionId)).thenReturn(Optional.of(sess))
        whenever(leaderboardRedisService.getFullLeaderboard(sessionId.toString())).thenReturn(leaderboard)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(buildUser()))
        whenever(rankingRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(trainSessionRepository.save(any())).thenAnswer { it.arguments[0] }

        service.endSession(sessionId.toString())

        verify(leaderboardRedisService, never()).incrementGlobalScore(any(), any(), any())
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

    // ─── notifyFriends ────────────────────────────────────────────────────────

    @Test
    fun shouldNotifyAcceptedFriends_onSessionEnd() {
        val friendId = UUID.randomUUID()
        val friend = User(id = friendId, email = "friend@test.com", name = "Friend", password = "enc")
        val sess = buildSession()
        val friendship = Friendship(
            requester = buildUser(),
            recipient = friend,
            status = FriendshipStatus.ACCEPTED
        )
        val leaderboard = linkedSetOf(DefaultTypedTuple(userId.toString(), 5.0))

        whenever(trainSessionRepository.findById(sessionId)).thenReturn(Optional.of(sess))
        whenever(leaderboardRedisService.getFullLeaderboard(sessionId.toString())).thenReturn(leaderboard)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(buildUser()))
        whenever(rankingRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(trainSessionRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(friendshipRepository.findByRequesterIdOrRecipientId(userId, userId)).thenReturn(listOf(friendship))

        service.endSession(sessionId.toString())

        verify(messagingTemplate).convertAndSend(
            eq("/topic/user/$friendId/notifications"),
            any<br.inatel.tcc.dto.SessionResultNotification>()
        )
    }

    @Test
    fun shouldNotNotify_whenUserHasNoAcceptedFriends() {
        val sess = buildSession()
        whenever(trainSessionRepository.findById(sessionId)).thenReturn(Optional.of(sess))
        whenever(leaderboardRedisService.getFullLeaderboard(sessionId.toString())).thenReturn(emptySet())
        whenever(trainSessionRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(friendshipRepository.findByRequesterIdOrRecipientId(userId, userId)).thenReturn(emptyList())

        service.endSession(sessionId.toString())

        verify(messagingTemplate, never()).convertAndSend(any<String>(), any<Any>())
    }

    // ─── getGlobalRanking ─────────────────────────────────────────────────────

    @Test
    fun shouldReturnGlobalRanking_forPeriod() {
        val user1Id = UUID.randomUUID()
        val user2Id = UUID.randomUUID()
        val entries = linkedSetOf(
            DefaultTypedTuple(user1Id.toString(), 12.5),
            DefaultTypedTuple(user2Id.toString(), 8.0)
        )
        whenever(leaderboardRedisService.getGlobalRanking("2026-05")).thenReturn(entries)

        val result = service.getGlobalRanking("2026-05")

        assertEquals(2, result.size)
        assertEquals(1, result[0].rank)
        assertEquals(12.5, result[0].totalDistanceKm)
        assertEquals(2, result[1].rank)
        assertEquals(8.0, result[1].totalDistanceKm)
    }

    @Test
    fun shouldReturnEmptyList_whenNoGlobalRanking() {
        whenever(leaderboardRedisService.getGlobalRanking("2026-05")).thenReturn(null)

        val result = service.getGlobalRanking("2026-05")

        assertEquals(emptyList<Any>(), result)
    }

    // ─── getAllHordes ─────────────────────────────────────────────────────────

    @Test
    fun shouldReturnAllHordes() {
        val hordeId1 = UUID.randomUUID()
        val hordeId2 = UUID.randomUUID()
        val hordes = listOf(
            Horde(id = hordeId1, name = "Easy Horde", difficulty = HordeDifficulty.EASY, estimatedDuration = 30),
            Horde(id = hordeId2, name = "Hard Horde", difficulty = HordeDifficulty.HARD, estimatedDuration = 90)
        )
        whenever(hordeRepository.findAll()).thenReturn(hordes)

        val result = service.getAllHordes()

        assertEquals(2, result.size)
        assertEquals("Easy Horde", result[0].name)
        assertEquals(HordeDifficulty.EASY, result[0].difficulty)
        assertEquals("Hard Horde", result[1].name)
        assertEquals(HordeDifficulty.HARD, result[1].difficulty)
    }
}

