package br.inatel.tcc.controller

import br.inatel.tcc.dto.LeaderboardEntryDto
import br.inatel.tcc.dto.StartSessionRequest
import br.inatel.tcc.dto.StartSessionResponse
import br.inatel.tcc.domain.trainsession.TrainSession
import br.inatel.tcc.service.TrainSessionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/sessions")
class TrainSessionController(
    private val trainSessionService: TrainSessionService
) {
    @PostMapping("/start")
    fun startSession(
        @RequestBody request: StartSessionRequest,
        authentication: Authentication
    ): ResponseEntity<StartSessionResponse> {
        val response = trainSessionService.startSession(authentication.name, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/{sessionId}/finish")
    fun endSession(
        @PathVariable sessionId: String,
        authentication: Authentication
    ): ResponseEntity<Void> {
        trainSessionService.endSession(sessionId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{sessionId}/leaderboard")
    fun getLeaderboard(
        @PathVariable sessionId: String,
        authentication: Authentication
    ): ResponseEntity<List<LeaderboardEntryDto>> {
        val leaderboard = trainSessionService.getLeaderboard(sessionId)
        return ResponseEntity.ok(leaderboard)
    }

    @GetMapping("/{sessionId}")
    fun getSession(
        @PathVariable sessionId: String,
        authentication: Authentication
    ): ResponseEntity<TrainSession> {
        val session = trainSessionService.getSession(sessionId)
        return ResponseEntity.ok(session)
    }
}
