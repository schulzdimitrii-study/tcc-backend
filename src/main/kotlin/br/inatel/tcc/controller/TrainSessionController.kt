package br.inatel.tcc.controller

import br.inatel.tcc.dto.GlobalRankingEntryDto
import br.inatel.tcc.dto.HordeResponse
import br.inatel.tcc.dto.LeaderboardEntryDto
import br.inatel.tcc.dto.StartSessionRequest
import br.inatel.tcc.dto.StartSessionResponse
import br.inatel.tcc.domain.trainsession.TrainSession
import br.inatel.tcc.service.TrainSessionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Training Session", description = "Training Session API")
@RequestMapping("/sessions")
class TrainSessionController(
    private val trainSessionService: TrainSessionService
) {

    @Operation(summary = "Start a new training session", responses = [ApiResponse(description = "Session started", content = [Content(mediaType = "application/json", schema = Schema(implementation = StartSessionResponse::class))])])
    @PostMapping("/start")
    fun startSession(
        @RequestBody request: StartSessionRequest,
        authentication: Authentication
    ): ResponseEntity<StartSessionResponse> {
        val response = trainSessionService.startSession(authentication.name, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "End a training session", responses = [ApiResponse(description = "Session ended")])
    @PostMapping("/{sessionId}/finish")
    fun endSession(
        @PathVariable sessionId: String,
        authentication: Authentication
    ): ResponseEntity<Void> {
        trainSessionService.endSession(sessionId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Get leaderboard for a training session", responses = [ApiResponse(description = "Leaderboard", content = [Content(mediaType = "application/json", schema = Schema(implementation = LeaderboardEntryDto::class))])])
    @GetMapping("/{sessionId}/leaderboard")
    fun getLeaderboard(
        @PathVariable sessionId: String,
        authentication: Authentication
    ): ResponseEntity<List<LeaderboardEntryDto>> {
        val leaderboard = trainSessionService.getLeaderboard(sessionId)
        return ResponseEntity.ok(leaderboard)
    }

    @Operation(summary = "Get a training session", responses = [ApiResponse(description = "Training session", content = [Content(mediaType = "application/json", schema = Schema(implementation = TrainSession::class))])])
    @GetMapping("/{sessionId}")
    fun getSession(
        @PathVariable sessionId: String,
        authentication: Authentication
    ): ResponseEntity<TrainSession> {
        val session = trainSessionService.getSession(sessionId)
        return ResponseEntity.ok(session)
    }

    @Operation(summary = "List all hordes", responses = [ApiResponse(description = "List of hordes", content = [Content(mediaType = "application/json", schema = Schema(implementation = HordeResponse::class))])])
    @GetMapping(value = ["/hordes"])
    fun getHordes(
        authentication: Authentication
    ): ResponseEntity<List<HordeResponse>> {
        val hordes = trainSessionService.getAllHordes()
        return ResponseEntity.ok(hordes)
    }

    @Operation(summary = "Get global ranking for a period", responses = [ApiResponse(description = "Global ranking", content = [Content(mediaType = "application/json", schema = Schema(implementation = GlobalRankingEntryDto::class))])])
    @GetMapping("/ranking/{period}")
    fun getGlobalRanking(
        @PathVariable period: String,
        authentication: Authentication
    ): ResponseEntity<List<GlobalRankingEntryDto>> {
        return ResponseEntity.ok(trainSessionService.getGlobalRanking(period))
    }
}
