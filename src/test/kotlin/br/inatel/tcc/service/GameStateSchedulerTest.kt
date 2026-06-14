package br.inatel.tcc.service

import br.inatel.tcc.dto.GameStateResponse
import br.inatel.tcc.dto.GameStatus
import br.inatel.tcc.service.redis.LeaderboardRedisService
import br.inatel.tcc.service.redis.SessionRedisService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.DefaultTypedTuple
import org.springframework.messaging.simp.SimpMessagingTemplate

@ExtendWith(MockitoExtension::class)
class GameStateSchedulerTest {

    @Mock private lateinit var leaderboardRedisService: LeaderboardRedisService
    @Mock private lateinit var sessionRedisService: SessionRedisService
    @Mock private lateinit var hordePositionService: HordePositionService
    @Mock private lateinit var messagingTemplate: SimpMessagingTemplate

    @InjectMocks private lateinit var scheduler: GameStateScheduler

    private val sessionId = "session-abc"
    private val userId = "user-123"

    // ─── Casos de skip (sem publicação) ──────────────────────────────────────

    @Test
    fun shouldDoNothing_whenNoActiveSessions() {
        whenever(leaderboardRedisService.getActiveSessions()).thenReturn(emptySet())

        scheduler.broadcastHordeGameState()

        verify(messagingTemplate, never()).convertAndSend(any<String>(), any<GameStateResponse>())
    }

    @Test
    fun shouldSkipSession_whenHordePaceIsNull() {
        whenever(leaderboardRedisService.getActiveSessions()).thenReturn(setOf(sessionId))
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(null)

        scheduler.broadcastHordeGameState()

        verify(messagingTemplate, never()).convertAndSend(any<String>(), any<GameStateResponse>())
    }

    @Test
    fun shouldSkipSession_whenHordePaceIsZero() {
        whenever(leaderboardRedisService.getActiveSessions()).thenReturn(setOf(sessionId))
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(0.0)

        scheduler.broadcastHordeGameState()

        verify(messagingTemplate, never()).convertAndSend(any<String>(), any<GameStateResponse>())
    }

    @Test
    fun shouldSkipSession_whenStartEpochIsNull() {
        whenever(leaderboardRedisService.getActiveSessions()).thenReturn(setOf(sessionId))
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(6.0)
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(null)

        scheduler.broadcastHordeGameState()

        verify(messagingTemplate, never()).convertAndSend(any<String>(), any<GameStateResponse>())
    }

    @Test
    fun shouldSkipSession_whenLeaderboardIsNull() {
        whenever(leaderboardRedisService.getActiveSessions()).thenReturn(setOf(sessionId))
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(6.0)
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(1000L)
        whenever(leaderboardRedisService.getFullLeaderboard(sessionId)).thenReturn(null)
        whenever(hordePositionService.calculateVirtualPosition(any(), any())).thenReturn(1.0)

        scheduler.broadcastHordeGameState()

        verify(messagingTemplate, never()).convertAndSend(any<String>(), any<GameStateResponse>())
    }

    // ─── Campos do GameStateResponse ─────────────────────────────────────────

    @Test
    fun shouldCalculateAllGameStateFieldsCorrectly() {
        setupSession(playerPosition = 2.0, hordePosition = 1.0, goalDistance = 10.0, playerSpeed = 10.0)

        val captor = argumentCaptor<GameStateResponse>()
        scheduler.broadcastHordeGameState()

        verify(messagingTemplate).convertAndSend(
            eq("/topic/session/$sessionId/game-state"),
            captor.capture()
        )
        val state = captor.firstValue
        assertEquals(sessionId, state.sessionId)
        assertEquals(userId, state.userId)
        assertEquals(2.0, state.playerPosition)
        assertEquals(1.0, state.hordePosition)
        assertEquals(8.0, state.distanceToGoal)         // 10.0 - 2.0
        assertEquals(1.0, state.distancePlayerToHorde)  // |2.0 - 1.0|
        assertEquals(10.0, state.playerSpeed)
        assertEquals(10.0, state.hordeSpeed)            // 60.0 / 6.0
        assertEquals(20.0, state.raceProgress)          // (2.0 / 10.0) * 100
        assertEquals(GameStatus.RUNNING, state.gameStatus)
        org.junit.jupiter.api.Assertions.assertTrue(state.serverTimestampMs > 0L)
    }

    @Test
    fun shouldUseZeroAsPlayerSpeed_whenUserStateHasNoSpeedField() {
        setupSession(playerPosition = 2.0, hordePosition = 1.0, goalDistance = 10.0, playerSpeed = null)

        val captor = argumentCaptor<GameStateResponse>()
        scheduler.broadcastHordeGameState()

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assertEquals(0.0, captor.firstValue.playerSpeed)
    }

    @Test
    fun shouldCalculateHordeSpeed_from60DividedByPace() {
        setupSession(playerPosition = 2.0, hordePosition = 1.0, goalDistance = 10.0, hordePace = 4.0)

        val captor = argumentCaptor<GameStateResponse>()
        scheduler.broadcastHordeGameState()

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assertEquals(15.0, captor.firstValue.hordeSpeed)  // 60.0 / 4.0
    }

    // ─── GameStatus ───────────────────────────────────────────────────────────

    @Test
    fun shouldPublishRunningStatus_whenPlayerIsAheadOfHorde() {
        setupSession(playerPosition = 3.0, hordePosition = 1.0, goalDistance = 10.0)

        val captor = argumentCaptor<GameStateResponse>()
        scheduler.broadcastHordeGameState()

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assertEquals(GameStatus.RUNNING, captor.firstValue.gameStatus)
    }

    @Test
    fun shouldPublishCaughtStatus_whenHordePositionEqualsPlayerPosition() {
        setupSession(playerPosition = 2.0, hordePosition = 2.0, goalDistance = 10.0)

        val captor = argumentCaptor<GameStateResponse>()
        scheduler.broadcastHordeGameState()

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assertEquals(GameStatus.CAUGHT, captor.firstValue.gameStatus)
    }

    @Test
    fun shouldPublishCaughtStatus_whenHordeIsAheadOfPlayer() {
        setupSession(playerPosition = 1.0, hordePosition = 2.5, goalDistance = 10.0)

        val captor = argumentCaptor<GameStateResponse>()
        scheduler.broadcastHordeGameState()

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assertEquals(GameStatus.CAUGHT, captor.firstValue.gameStatus)
    }

    @Test
    fun shouldPublishRunningStatus_whenHordePositionIsAheadButDelayIsActive() {
        val currentSec = System.currentTimeMillis() / 1000
        whenever(hordePositionService.getStartDelaySeconds()).thenReturn(7L)
        setupSession(
            playerPosition = 0.0,
            hordePosition = 0.0,
            goalDistance = 10.0,
            startEpoch = currentSec - 3
        )

        val captor = argumentCaptor<GameStateResponse>()
        scheduler.broadcastHordeGameState()

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assertEquals(GameStatus.RUNNING, captor.firstValue.gameStatus)
    }

    @Test
    fun shouldPublishEscapedStatus_whenPlayerReachesGoal() {
        setupSession(playerPosition = 10.5, hordePosition = 5.0, goalDistance = 10.0)

        val captor = argumentCaptor<GameStateResponse>()
        scheduler.broadcastHordeGameState()

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assertEquals(GameStatus.ESCAPED, captor.firstValue.gameStatus)
    }

    @Test
    fun shouldPublishEscapedStatus_whenPlayerPositionEqualsGoal() {
        setupSession(playerPosition = 10.0, hordePosition = 5.0, goalDistance = 10.0)

        val captor = argumentCaptor<GameStateResponse>()
        scheduler.broadcastHordeGameState()

        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assertEquals(GameStatus.ESCAPED, captor.firstValue.gameStatus)
    }

    // ─── Publicação para múltiplos jogadores e sessões ────────────────────────

    @Test
    fun shouldPublishGameState_forEachPlayerInSession() {
        val startEpoch = 1000L
        val entries = linkedSetOf(
            DefaultTypedTuple("user-1", 3.0),
            DefaultTypedTuple("user-2", 2.0),
            DefaultTypedTuple("user-3", 1.0)
        )
        whenever(leaderboardRedisService.getActiveSessions()).thenReturn(setOf(sessionId))
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(6.0)
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(startEpoch)
        whenever(leaderboardRedisService.getGoalDistance(sessionId)).thenReturn(10.0)
        whenever(leaderboardRedisService.getFullLeaderboard(sessionId)).thenReturn(entries)
        whenever(hordePositionService.calculateVirtualPosition(startEpoch, 6.0)).thenReturn(1.5)
        whenever(sessionRedisService.getUserState(eq(sessionId), any())).thenReturn(mapOf("speed" to "10.0"))

        scheduler.broadcastHordeGameState()

        verify(messagingTemplate, times(3)).convertAndSend(
            eq("/topic/session/$sessionId/game-state"),
            any<GameStateResponse>()
        )
    }

    @Test
    fun shouldPublishToCorrectTopic_forEachSession() {
        val sessionId2 = "session-xyz"
        val entry = linkedSetOf(DefaultTypedTuple(userId, 2.0))

        listOf(sessionId, sessionId2).forEach { sid ->
            whenever(leaderboardRedisService.getHordePace(sid)).thenReturn(6.0)
            whenever(leaderboardRedisService.getSessionStartEpoch(sid)).thenReturn(1000L)
            whenever(leaderboardRedisService.getGoalDistance(sid)).thenReturn(10.0)
            whenever(leaderboardRedisService.getFullLeaderboard(sid)).thenReturn(entry)
            whenever(sessionRedisService.getUserState(eq(sid), any())).thenReturn(mapOf("speed" to "10.0"))
        }
        whenever(leaderboardRedisService.getActiveSessions()).thenReturn(setOf(sessionId, sessionId2))
        whenever(hordePositionService.calculateVirtualPosition(any(), any())).thenReturn(1.0)

        scheduler.broadcastHordeGameState()

        verify(messagingTemplate).convertAndSend(
            eq("/topic/session/$sessionId/game-state"),
            any<GameStateResponse>()
        )
        verify(messagingTemplate).convertAndSend(
            eq("/topic/session/$sessionId2/game-state"),
            any<GameStateResponse>()
        )
    }

    @Test
    fun shouldSkipOnlySessionWithoutHorde_andPublishForSessionWithHorde() {
        val sessionWithoutHorde = "session-free"
        whenever(leaderboardRedisService.getActiveSessions()).thenReturn(setOf(sessionWithoutHorde, sessionId))
        whenever(leaderboardRedisService.getHordePace(sessionWithoutHorde)).thenReturn(null)
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(6.0)
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(1000L)
        whenever(leaderboardRedisService.getGoalDistance(sessionId)).thenReturn(10.0)
        whenever(leaderboardRedisService.getFullLeaderboard(sessionId)).thenReturn(
            linkedSetOf(DefaultTypedTuple(userId, 2.0))
        )
        whenever(hordePositionService.calculateVirtualPosition(any(), any())).thenReturn(1.0)
        whenever(sessionRedisService.getUserState(eq(sessionId), any())).thenReturn(mapOf("speed" to "10.0"))

        scheduler.broadcastHordeGameState()

        verify(messagingTemplate, never()).convertAndSend(
            eq("/topic/session/$sessionWithoutHorde/game-state"),
            any<GameStateResponse>()
        )
        verify(messagingTemplate).convertAndSend(
            eq("/topic/session/$sessionId/game-state"),
            any<GameStateResponse>()
        )
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun setupSession(
        playerPosition: Double,
        hordePosition: Double,
        goalDistance: Double,
        playerSpeed: Double? = 10.0,
        hordePace: Double = 6.0,
        startEpoch: Long = 1000L
    ) {
        whenever(leaderboardRedisService.getActiveSessions()).thenReturn(setOf(sessionId))
        whenever(leaderboardRedisService.getHordePace(sessionId)).thenReturn(hordePace)
        whenever(leaderboardRedisService.getSessionStartEpoch(sessionId)).thenReturn(startEpoch)
        whenever(leaderboardRedisService.getGoalDistance(sessionId)).thenReturn(goalDistance)
        whenever(leaderboardRedisService.getFullLeaderboard(sessionId)).thenReturn(
            linkedSetOf(DefaultTypedTuple(userId, playerPosition))
        )
        whenever(hordePositionService.calculateVirtualPosition(startEpoch, hordePace)).thenReturn(hordePosition)
        val stateMap = if (playerSpeed != null) mapOf("speed" to playerSpeed.toString()) else emptyMap()
        whenever(sessionRedisService.getUserState(sessionId, userId)).thenReturn(stateMap)
    }
}
