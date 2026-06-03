package br.inatel.tcc.service

import br.inatel.tcc.domain.ranking.Ranking
import br.inatel.tcc.domain.ranking.RankingRepository
import br.inatel.tcc.domain.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RankingServiceTest {

    @Mock
    private lateinit var rankingRepository: RankingRepository

    @InjectMocks
    private lateinit var rankingService: RankingService

    private val userId = UUID.randomUUID()
    private val rankingId = UUID.randomUUID()

    private fun buildUser() = User(
        id = userId,
        name = "Competitor 1",
        email = "competitor1@example.com",
        password = "password123",
        birthdayDate = LocalDate.of(1995, 5, 5)
    )

    private fun buildRanking() = Ranking(
        id = rankingId,
        user = buildUser(),
        position = 1,
        score = 12.5,
        period = "2026-06",
        calculeDate = LocalDate.now(),
        targetDistance = 5.0
    )

    @Test
    fun getRankingsByPeriod_shouldReturnMappedDtoList() {
        val period = "2026-06"
        val rankingsList = listOf(buildRanking())
        whenever(rankingRepository.findByPeriodOrderByPositionAsc(period)).thenReturn(rankingsList)

        val result = rankingService.getRankingsByPeriod(period)

        assertNotNull(result)
        assertEquals(1, result.size)
        val dto = result[0]
        assertEquals(rankingId, dto.id)
        assertEquals(userId, dto.userId)
        assertEquals("Competitor 1", dto.userName)
        assertEquals(1, dto.position)
        assertEquals(12.5, dto.score)
        assertEquals(period, dto.period)
        assertEquals(5.0, dto.targetDistance)
    }

    @Test
    fun getRankingsByPeriod_withTargetDistance_shouldReturnFilteredMappedDtoList() {
        val period = "2026-06"
        val targetDistance = 5.0
        val rankingsList = listOf(buildRanking())
        whenever(rankingRepository.findByPeriodAndTargetDistanceOrderByPositionAsc(period, targetDistance)).thenReturn(rankingsList)

        val result = rankingService.getRankingsByPeriod(period, targetDistance)

        assertNotNull(result)
        assertEquals(1, result.size)
        val dto = result[0]
        assertEquals(rankingId, dto.id)
        assertEquals(5.0, dto.targetDistance)
    }

    @Test
    fun getRankingsByUserId_shouldReturnMappedDtoList() {
        val rankingsList = listOf(buildRanking())
        whenever(rankingRepository.findByUserId(userId)).thenReturn(rankingsList)

        val result = rankingService.getRankingsByUserId(userId)

        assertNotNull(result)
        assertEquals(1, result.size)
        val dto = result[0]
        assertEquals(rankingId, dto.id)
        assertEquals(userId, dto.userId)
        assertEquals("Competitor 1", dto.userName)
        assertEquals(1, dto.position)
        assertEquals(12.5, dto.score)
        assertEquals("2026-06", dto.period)
        assertEquals(5.0, dto.targetDistance)
    }
}
