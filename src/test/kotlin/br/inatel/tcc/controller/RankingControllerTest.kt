package br.inatel.tcc.controller

import br.inatel.tcc.dto.RankingResponseDto
import br.inatel.tcc.service.RankingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RankingControllerTest {

    @Mock
    private lateinit var rankingService: RankingService

    @InjectMocks
    private lateinit var controller: RankingController

    private val userId = UUID.randomUUID()
    private val rankingId = UUID.randomUUID()

    private val principal = mock(Authentication::class.java)

    private fun buildRankingResponseDto() = RankingResponseDto(
        id = rankingId,
        userId = userId,
        userName = "Competitor",
        position = 1,
        score = 15.0,
        period = "2026-06",
        calculeDate = LocalDate.now(),
        targetDistance = 5.0
    )

    @Test
    fun getRankingsByPeriod_shouldReturn200WithRankings() {
        val period = "2026-06"
        val rankings = listOf(buildRankingResponseDto())
        whenever(rankingService.getRankingsByPeriod(period, 5.0)).thenReturn(rankings)

        val response = controller.getRankingsByPeriod(period, 5.0, principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.size)
        assertEquals(rankingId, response.body?.first()?.id)
        assertEquals("Competitor", response.body?.first()?.userName)
        assertEquals(5.0, response.body?.first()?.targetDistance)
    }

    @Test
    fun getRankingsByUserId_shouldReturn200WithRankings() {
        val rankings = listOf(buildRankingResponseDto())
        whenever(rankingService.getRankingsByUserId(userId)).thenReturn(rankings)

        val response = controller.getRankingsByUserId(userId, principal)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.size)
        assertEquals(rankingId, response.body?.first()?.id)
        assertEquals("Competitor", response.body?.first()?.userName)
        assertEquals(5.0, response.body?.first()?.targetDistance)
    }
}
