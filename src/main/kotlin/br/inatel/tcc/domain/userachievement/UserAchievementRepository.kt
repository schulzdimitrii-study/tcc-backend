package br.inatel.tcc.domain.userachievement

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserAchievementRepository : JpaRepository<UserAchievement, UserAchievementId> {
    fun findByUserId(userId: UUID): List<UserAchievement>
    fun findByAchievementId(achievementId: UUID): List<UserAchievement>
}
