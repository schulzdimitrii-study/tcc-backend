package br.inatel.tcc.domain.userachievement

import br.inatel.tcc.domain.achievement.Achievement
import br.inatel.tcc.domain.user.User
import jakarta.persistence.*
import java.time.LocalDate

@Table(name = "user_achievements")
@Entity
class UserAchievement(
    @EmbeddedId
    val id: UserAchievementId = UserAchievementId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("achievementId")
    @JoinColumn(name = "achievement_id")
    val achievement: Achievement,

    @Column(nullable = false)
    val unlockDate: LocalDate = LocalDate.now()
)
