package br.inatel.tcc.controller

import br.inatel.tcc.dto.RankingResponseDto
import br.inatel.tcc.service.RankingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@RestController
@Tag(name = "Ranking", description = "Ranking API")
@RequestMapping("/rankings")
class RankingController(
    private val rankingService: RankingService
) {
    @Operation(
        summary = "Get historical rankings by period",
        responses = [
            ApiResponse(
                description = "Historical rankings",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = RankingResponseDto::class)
                    )
                ]
            )
        ]
    )
    @GetMapping("/{period}")
    fun getRankingsByPeriod(
        @PathVariable period: String,
        @RequestParam(required = false) targetDistance: Double?,
        authentication: Authentication
    ): ResponseEntity<List<RankingResponseDto>> {
        val rankings = rankingService.getRankingsByPeriod(period, targetDistance)
        return ResponseEntity.ok(rankings)
    }

    @Operation(
        summary = "Get historical rankings by user ID",
        responses = [
            ApiResponse(
                description = "Historical rankings for user",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = RankingResponseDto::class)
                    )
                ]
            )
        ]
    )
    @GetMapping("/user/{userId}")
    fun getRankingsByUserId(
        @PathVariable userId: UUID,
        authentication: Authentication
    ): ResponseEntity<List<RankingResponseDto>> {
        val rankings = rankingService.getRankingsByUserId(userId)
        return ResponseEntity.ok(rankings)
    }
}
