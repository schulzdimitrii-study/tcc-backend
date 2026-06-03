package br.inatel.tcc.service

import br.inatel.tcc.domain.ranking.RankingRepository
import br.inatel.tcc.dto.RankingResponseDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RankingService(
    private val rankingRepository: RankingRepository
) {
    @Transactional(readOnly = true)
    fun getRankingsByPeriod(period: String, targetDistance: Double? = null): List<RankingResponseDto> {
        val rankings = if (targetDistance != null) {
            rankingRepository.findByPeriodAndTargetDistanceOrderByPositionAsc(period, targetDistance)
        } else {
            rankingRepository.findByPeriodOrderByPositionAsc(period)
        }
        return rankings.map {
            RankingResponseDto(
                id = it.id ?: UUID.randomUUID(),
                userId = it.user.id ?: UUID.randomUUID(),
                userName = it.user.name,
                position = it.position,
                score = it.score,
                period = it.period,
                calculeDate = it.calculeDate,
                targetDistance = it.targetDistance
            )
        }
    }

    @Transactional(readOnly = true)
    fun getRankingsByUserId(userId: UUID): List<RankingResponseDto> {
        return rankingRepository.findByUserId(userId).map {
            RankingResponseDto(
                id = it.id ?: UUID.randomUUID(),
                userId = it.user.id ?: UUID.randomUUID(),
                userName = it.user.name,
                position = it.position,
                score = it.score,
                period = it.period,
                calculeDate = it.calculeDate,
                targetDistance = it.targetDistance
            )
        }
    }
}
