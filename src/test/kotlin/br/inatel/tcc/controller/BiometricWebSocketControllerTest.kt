package br.inatel.tcc.controller

import br.inatel.tcc.dto.BiometricDataMessage
import br.inatel.tcc.dto.LeaderboardEntryDto
import br.inatel.tcc.service.BiometricPersistenceService
import br.inatel.tcc.service.CardiacZoneService
import br.inatel.tcc.service.HordePositionService
import br.inatel.tcc.service.redis.LeaderboardRedisService
import br.inatel.tcc.service.redis.SessionRedisService
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.DefaultTypedTuple
import org.springframework.messaging.simp.SimpMessagingTemplate
import br.inatel.tcc.dto.LeaderboardResponse
import br.inatel.tcc.dto.GameStateResponse
import br.inatel.tcc.dto.GameStatus

@ExtendWith(MockitoExtension::class)
class BiometricWebSocketControllerTest {

    @Mock private lateinit var sessionRedisService: SessionRedisService
    @Mock private lateinit var leaderboardRedisService: LeaderboardRedisService
    @Mock private lateinit var hordePositionService: HordePositionService
    @Mock private lateinit var messagingTemplate: SimpMessagingTemplate
    @Mock private lateinit var validator: Validator
    @Mock private lateinit var biometricPersistenceService: BiometricPersistenceService
    @Mock private lateinit var cardiacZoneService: CardiacZoneService

    @InjectMocks private lateinit var controller: BiometricWebSocketController

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        org.mockito.Mockito.lenient().`when`(leaderboardRedisService.getGoalDistance(any())).thenReturn(null)
    }

    private val sessionId = "session-abc"
    private val userId = "user-123"

    private fun buildMessage(
        distance: Double = 2.5,
        bpm: Int = 155,
        sessionId: String = this.sessionId,
        userId: String = this.userId
    ) = BiometricDataMessage(
        sessionId = sessionId,
        userId = userId,
        timestamp = System.currentTimeMillis(),
        bpm = bpm,
        cadence = 80.0,
        speed = 10.0,
        pace = 6.0,
        accumulatedDistance = distance,
        accumulatedCalories = 120.0
    )

    // ─── receiveBiometricData ─────────────────────────────────────────────────

    @Test
    fun shouldSaveUserStateAndUpdateLeaderboard() {
        val message = buildMessage()
        setupLeaderboardMocks(rank = 1L)

        controller.receiveBiometricData(message)

        verify(sessionRedisService).saveUserState(sessionId, userId, message)
        verify(leaderboardRedisService).updateUserDistance(sessionId, userId, message.accumulatedDistance)
    }

    @Test
    fun shouldBroadcastLeaderboardToCorrectTopic() {
        val message = buildMessage()
        setupLeaderboardMocks(rank = 0L)

        controller.receiveBiometricData(message)

        verify(messagingTemplate).convertAndSend(
            eq("/topic/session/$sessionId/leaderboard"),
            any<LeaderboardResponse>()
        )
    }

    @Test
    fun shouldIncludeHordeVirtualDistanceWhenSessionHasHorde() {
        val message = buildMessage()
        val startEpoch = System.currentTimeMillis() / 1000 - 600
        whenever(leaderboardRedisService.getUserRank(sessionId, userId)).thenReturn(0L)
        whenever(leaderboardRedisService.getTopEntries(sessionId, 10)).thenReturn(emptySet())
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(startEpoch)
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(6.0)
        whenever(hordePositionService.calculateVirtualPosition(startEpoch, 6.0)).thenReturn(1.666)

        val captor = argumentCaptor<LeaderboardResponse>()
        controller.receiveBiometricData(message)

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        val response = captor.firstValue
        assert(response.hordeVirtualDistanceKm != null)
        assert(response.hordeVirtualDistanceKm == 1.666)
    }

    @Test
    fun shouldSetHordeDistanceNullWhenNoHorde() {
        val message = buildMessage()
        setupLeaderboardMocks(rank = 0L, includeHorde = false)

        val captor = argumentCaptor<LeaderboardResponse>()
        controller.receiveBiometricData(message)

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assert(captor.firstValue.hordeVirtualDistanceKm == null)
    }

    @Test
    fun shouldSetHordeDistanceNullWhenHordePaceIsZero() {
        val message = buildMessage()
        whenever(leaderboardRedisService.getUserRank(sessionId, userId)).thenReturn(0L)
        whenever(leaderboardRedisService.getTopEntries(sessionId, 10)).thenReturn(emptySet())
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(1000L)
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(0.0)

        val captor = argumentCaptor<LeaderboardResponse>()
        controller.receiveBiometricData(message)

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assert(captor.firstValue.hordeVirtualDistanceKm == null)
        verify(hordePositionService, never()).calculateVirtualPosition(any(), any())
    }

    @Test
    fun shouldMapUserRankCorrectly() {
        val message = buildMessage()
        whenever(leaderboardRedisService.getUserRank(sessionId, userId)).thenReturn(2L)
        whenever(leaderboardRedisService.getTopEntries(sessionId, 10)).thenReturn(emptySet())
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(null)
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(null)

        val captor = argumentCaptor<LeaderboardResponse>()
        controller.receiveBiometricData(message)

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assert(captor.firstValue.userRank == 3)
    }

    @Test
    fun shouldMapLeaderboardEntriesCorrectly() {
        val message = buildMessage()
        val entries = linkedSetOf(
            DefaultTypedTuple("user-1", 5.0),
            DefaultTypedTuple("user-2", 3.5)
        )
        whenever(leaderboardRedisService.getUserRank(sessionId, userId)).thenReturn(0L)
        whenever(leaderboardRedisService.getTopEntries(sessionId, 10)).thenReturn(entries)
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(null)
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(null)

        val captor = argumentCaptor<LeaderboardResponse>()
        controller.receiveBiometricData(message)

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        val responseEntries = captor.firstValue.entries
        assert(responseEntries.size == 2)
        assert(responseEntries[0].userId == "user-1")
        assert(responseEntries[0].rank == 1)
        assert(responseEntries[0].distanceKm == 5.0)
    }

    @Test
    fun shouldHandleNullUserRankAsFirst() {
        val message = buildMessage()
        whenever(leaderboardRedisService.getUserRank(sessionId, userId)).thenReturn(null)
        whenever(leaderboardRedisService.getTopEntries(sessionId, 10)).thenReturn(emptySet())
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(null)
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(null)

        val captor = argumentCaptor<LeaderboardResponse>()
        controller.receiveBiometricData(message)

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assert(captor.firstValue.userRank == 1)
    }

    @Test
    fun shouldUpdateHordePace_whenHordeIsAdaptive() {
        val message = buildMessage()
        setupLeaderboardMocks(rank = 0L)
        whenever(leaderboardRedisService.isHordeAdaptive(sessionId)).thenReturn(true)
        whenever(leaderboardRedisService.getTopEntries(sessionId, 10)).thenReturn(
            linkedSetOf(org.springframework.data.redis.core.DefaultTypedTuple(userId, 2.5))
        )
        whenever(sessionRedisService.getAveragePace(sessionId, listOf(userId))).thenReturn(5.5)

        controller.receiveBiometricData(message)

        verify(leaderboardRedisService).updateHordePace(sessionId, 5.5)
    }

    @Test
    fun shouldNotUpdateHordePace_whenHordeIsNotAdaptive() {
        val message = buildMessage()
        setupLeaderboardMocks(rank = 0L)
        whenever(leaderboardRedisService.isHordeAdaptive(sessionId)).thenReturn(false)

        controller.receiveBiometricData(message)

        verify(leaderboardRedisService, never()).updateHordePace(any(), any())
    }

    @Test
    fun shouldRejectMessageWhenValidationFails() {
        val message = buildMessage()
        val violation = mock<ConstraintViolation<BiometricDataMessage>>()
        whenever(validator.validate(message)).thenReturn(setOf(violation))

        controller.receiveBiometricData(message)

        verify(messagingTemplate, never()).convertAndSend(any<String>(), any<LeaderboardResponse>())
    }

    @Test
    fun shouldBroadcastGameStateWithCorrectDetails() {
        val message = buildMessage(distance = 2.0)
        setupLeaderboardMocks(rank = 0L)
        whenever(leaderboardRedisService.getGoalDistance(sessionId)).thenReturn(10.0)
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(1000L)
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(6.0)
        whenever(hordePositionService.calculateVirtualPosition(any(), any())).thenReturn(1.0)

        val captor = argumentCaptor<GameStateResponse>()
        controller.receiveBiometricData(message)

        verify(messagingTemplate).convertAndSend(
            eq("/topic/session/$sessionId/user/$userId/game-state"),
            captor.capture()
        )
        val state = captor.firstValue
        org.junit.jupiter.api.Assertions.assertEquals(2.0, state.playerPosition)
        org.junit.jupiter.api.Assertions.assertEquals(1.0, state.hordePosition)
        org.junit.jupiter.api.Assertions.assertEquals(8.0, state.distanceToGoal)
        org.junit.jupiter.api.Assertions.assertEquals(1.0, state.distancePlayerToHorde)
        org.junit.jupiter.api.Assertions.assertEquals(10.0, state.playerSpeed)
        org.junit.jupiter.api.Assertions.assertEquals(10.0, state.hordeSpeed)
        org.junit.jupiter.api.Assertions.assertEquals(20.0, state.raceProgress)
        org.junit.jupiter.api.Assertions.assertEquals(GameStatus.RUNNING, state.gameStatus)
    }

    @Test
    fun shouldBroadcastGameStateWhenCaught() {
        val message = buildMessage(distance = 1.0)
        setupLeaderboardMocks(rank = 0L)
        whenever(leaderboardRedisService.getGoalDistance(sessionId)).thenReturn(10.0)
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(1000L)
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(6.0)
        whenever(hordePositionService.calculateVirtualPosition(any(), any())).thenReturn(1.5)

        val captor = argumentCaptor<GameStateResponse>()
        controller.receiveBiometricData(message)

        verify(messagingTemplate).convertAndSend(
            eq("/topic/session/$sessionId/user/$userId/game-state"),
            captor.capture()
        )
        val state = captor.firstValue
        org.junit.jupiter.api.Assertions.assertEquals(GameStatus.CAUGHT, state.gameStatus)
    }

    @Test
    fun shouldBroadcastGameStateWhenEscaped() {
        val message = buildMessage(distance = 10.5)
        setupLeaderboardMocks(rank = 0L)
        whenever(leaderboardRedisService.getGoalDistance(sessionId)).thenReturn(10.0)
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(1000L)
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(6.0)
        whenever(hordePositionService.calculateVirtualPosition(any(), any())).thenReturn(5.0)

        val captor = argumentCaptor<GameStateResponse>()
        controller.receiveBiometricData(message)

        verify(messagingTemplate).convertAndSend(
            eq("/topic/session/$sessionId/user/$userId/game-state"),
            captor.capture()
        )
        val state = captor.firstValue
        org.junit.jupiter.api.Assertions.assertEquals(GameStatus.ESCAPED, state.gameStatus)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun setupLeaderboardMocks(rank: Long, includeHorde: Boolean = false) {
        whenever(leaderboardRedisService.getUserRank(sessionId, userId)).thenReturn(rank)
        whenever(leaderboardRedisService.getTopEntries(sessionId, 10)).thenReturn(emptySet())
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(null)
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(null)
    }
}
