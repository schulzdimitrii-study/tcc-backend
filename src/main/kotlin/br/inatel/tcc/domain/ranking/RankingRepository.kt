package br.inatel.tcc.domain.ranking

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RankingRepository : JpaRepository<Ranking, UUID> {
    fun findByPeriodOrderByPositionAsc(period: String): List<Ranking>
    fun findByUserId(userId: UUID): List<Ranking>
}
