package br.inatel.tcc.controller

import br.inatel.tcc.domain.trainsession.TrainSession
import br.inatel.tcc.domain.trainsession.TrainType
import br.inatel.tcc.domain.user.User
import br.inatel.tcc.dto.HordeResponse
import br.inatel.tcc.dto.LeaderboardEntryDto
import br.inatel.tcc.dto.StartSessionRequest
import br.inatel.tcc.dto.StartSessionResponse
import br.inatel.tcc.service.TrainSessionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class TrainSessionControllerTest {

    @Mock private lateinit var trainSessionService: TrainSessionService

    @InjectMocks private lateinit var controller: TrainSessionController

    private val sessionId = UUID.randomUUID()

    private fun buildUser() = User(
        id = UUID.randomUUID(), email = "test@example.com", name = "Test", password = "enc"
    )

    private fun buildSession() = TrainSession(
        id = sessionId, user = buildUser(), trainType = TrainType.RUN
    )

    // ─── POST /sessions/start ──────────────────────────────────────────────────

    @Test
    fun startSession_shouldReturn201WithSessionId() {
        val request = StartSessionRequest(trainType = "RUN")
        val principal = mockAuthentication("user@example.com")
        whenever(trainSessionService.startSession("user@example.com", request))
            .thenReturn(StartSessionResponse(sessionId = sessionId.toString()))

        val response = controller.startSession(request, principal)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(sessionId.toString(), response.body?.sessionId)
    }

    @Test
    fun startSession_shouldDelegateToService() {
        val request = StartSessionRequest(trainType = "RUN")
        val principal = mockAuthentication("user@example.com")
        whenever(trainSessionService.startSession(any(), any()))
            .thenReturn(StartSessionResponse(sessionId = sessionId.toString()))

        controller.startSession(request, principal)

        verify(trainSessionService).startSession("user@example.com", request)
    }

    @Test
    fun startSession_shouldPropagateServiceException() {
        val request = StartSessionRequest(trainType = "RUN")
        val principal = mockAuthentication("unknown@example.com")
        whenever(trainSessionService.startSession(any(), any()))
            .thenThrow(IllegalArgumentException("Usuário não encontrado"))

        assertThrows(IllegalArgumentException::class.java) {
            controller.startSession(request, principal)
        }
    }

    // ─── POST /sessions/{sessionId}/finish ────────────────────────────────────

    @Test
    fun endSession_shouldReturn204NoContent() {
        val principal = mockAuthentication("user@example.com")

        val response = controller.endSession(sessionId.toString(), principal)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        verify(trainSessionService).endSession(sessionId.toString())
    }

    @Test
    fun endSession_shouldPropagateServiceException() {
        val principal = mockAuthentication("user@example.com")
        whenever(trainSessionService.endSession(any()))
            .thenThrow(IllegalArgumentException("Sessão não encontrada"))

        assertThrows(IllegalArgumentException::class.java) {
            controller.endSession(sessionId.toString(), principal)
        }
    }

    // ─── GET /sessions/{sessionId}/leaderboard ────────────────────────────────

    @Test
    fun getLeaderboard_shouldReturn200WithEntries() {
        val principal = mockAuthentication("user@example.com")
        val entries = listOf(
            LeaderboardEntryDto(userId = "user-1", rank = 1, distanceKm = 5.0),
            LeaderboardEntryDto(userId = "user-2", rank = 2, distanceKm = 3.5)
        )
        whenever(trainSessionService.getLeaderboard(sessionId.toString())).thenReturn(entries)

        val response = controller.getLeaderboard(sessionId.toString(), principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(2, response.body?.size)
        assertEquals(1, response.body?.first()?.rank)
        assertEquals(5.0, response.body?.first()?.distanceKm)
    }

    @Test
    fun getLeaderboard_shouldReturnEmptyListWhenNoEntries() {
        val principal = mockAuthentication("user@example.com")
        whenever(trainSessionService.getLeaderboard(sessionId.toString())).thenReturn(emptyList())

        val response = controller.getLeaderboard(sessionId.toString(), principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(0, response.body?.size)
    }

    // ─── GET /sessions/{sessionId} ────────────────────────────────────────────

    @Test
    fun getSession_shouldReturn200WithSession() {
        val principal = mockAuthentication("user@example.com")
        val session = buildSession()
        whenever(trainSessionService.getSession(sessionId.toString())).thenReturn(session)

        val response = controller.getSession(sessionId.toString(), principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(sessionId, response.body?.id)
    }

    @Test
    fun getSession_shouldPropagateExceptionWhenNotFound() {
        val principal = mockAuthentication("user@example.com")
        whenever(trainSessionService.getSession(any()))
            .thenThrow(IllegalArgumentException("Sessão não encontrada"))

        assertThrows(IllegalArgumentException::class.java) {
            controller.getSession(sessionId.toString(), principal)
        }
    }

    // ─── GET /sessions/hordes ─────────────────────────────────────────────────

    @Test
    fun getHordes_shouldReturn200WithHordes() {
        val principal = mockAuthentication("user@example.com")
        val hordes = listOf(
            HordeResponse(id = UUID.randomUUID(), name = "Horde 1", description = "Desc 1", difficulty = br.inatel.tcc.domain.horde.HordeDifficulty.EASY, estimatedDuration = 30, targetPace = 5.0)
        )
        whenever(trainSessionService.getAllHordes()).thenReturn(hordes)

        val response = controller.getHordes(principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.size)
        assertEquals("Horde 1", response.body?.first()?.name)
        verify(trainSessionService).getAllHordes()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun mockAuthentication(email: String): org.springframework.security.core.Authentication {
        val auth = org.mockito.Mockito.mock(org.springframework.security.core.Authentication::class.java)
        org.mockito.Mockito.lenient().`when`(auth.name).thenReturn(email)
        return auth
    }
}
