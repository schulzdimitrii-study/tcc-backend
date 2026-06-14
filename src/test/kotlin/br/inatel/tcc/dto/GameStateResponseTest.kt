package br.inatel.tcc.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GameStateResponseTest {

    @Test
    fun shouldCreateGameStateResponseWithCorrectValues() {
        val response = GameStateResponse(
            sessionId = "session-abc",
            userId = "user-123",
            playerPosition = 100.0,
            hordePosition = 50.0,
            distanceToGoal = 400.0,
            distancePlayerToHorde = 50.0,
            playerSpeed = 5.5,
            hordeSpeed = 4.8,
            raceProgress = 20.0,
            gameStatus = GameStatus.RUNNING,
            serverTimestampMs = 1717970400000L
        )

        assertEquals("session-abc", response.sessionId)
        assertEquals("user-123", response.userId)
        assertEquals(100.0, response.playerPosition)
        assertEquals(50.0, response.hordePosition)
        assertEquals(400.0, response.distanceToGoal)
        assertEquals(50.0, response.distancePlayerToHorde)
        assertEquals(5.5, response.playerSpeed)
        assertEquals(4.8, response.hordeSpeed)
        assertEquals(20.0, response.raceProgress)
        assertEquals(GameStatus.RUNNING, response.gameStatus)
        assertEquals(1717970400000L, response.serverTimestampMs)
    }
}
