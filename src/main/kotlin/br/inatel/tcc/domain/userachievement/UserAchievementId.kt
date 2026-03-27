package br.inatel.tcc.domain.userachievement

import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

@Embeddable
data class UserAchievementId(
    val userId: UUID = UUID.randomUUID(),
    val achievementId: UUID = UUID.randomUUID()
) : Serializable
