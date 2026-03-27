package br.inatel.tcc.domain.achievement

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AchievementRepository : JpaRepository<Achievement, UUID> {
    fun findByActive(active: Boolean): List<Achievement>
}
