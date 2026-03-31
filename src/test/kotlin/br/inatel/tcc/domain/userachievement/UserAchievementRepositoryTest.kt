package br.inatel.tcc.domain.userachievement

import br.inatel.tcc.domain.achievement.Achievement
import br.inatel.tcc.domain.achievement.AchievementRepository
import br.inatel.tcc.domain.user.User
import br.inatel.tcc.domain.user.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = ["/test-cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserAchievementRepositoryTest {

    @Autowired
    private lateinit var userAchievementRepository: UserAchievementRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var achievementRepository: AchievementRepository

    private fun savedUser(email: String) = userRepository.save(
        User(email = email, name = "User", password = "encoded")
    )

    private fun savedAchievement(title: String) = achievementRepository.save(
        Achievement(title = title, active = true)
    )

    @Test
    fun shouldFindByUserId() {
        val user1 = savedUser("user1@test.com")
        val user2 = savedUser("user2@test.com")
        val achievement1 = savedAchievement("Conquista A")
        val achievement2 = savedAchievement("Conquista B")

        userAchievementRepository.save(
            UserAchievement(user = user1, achievement = achievement1, unlockDate = LocalDate.now())
        )
        userAchievementRepository.save(
            UserAchievement(user = user1, achievement = achievement2, unlockDate = LocalDate.now())
        )
        userAchievementRepository.save(
            UserAchievement(user = user2, achievement = achievement1, unlockDate = LocalDate.now())
        )

        val user1Achievements = userAchievementRepository.findByUserId(user1.id!!)
        val user2Achievements = userAchievementRepository.findByUserId(user2.id!!)

        assertEquals(2, user1Achievements.size)
        assertEquals(1, user2Achievements.size)
    }

    @Test
    fun shouldFindByAchievementId() {
        val user1 = savedUser("u1@test.com")
        val user2 = savedUser("u2@test.com")
        val sharedAchievement = savedAchievement("Conquista Compartilhada")
        val exclusiveAchievement = savedAchievement("Conquista Exclusiva")

        userAchievementRepository.save(UserAchievement(user = user1, achievement = sharedAchievement, unlockDate = LocalDate.now()))
        userAchievementRepository.save(UserAchievement(user = user2, achievement = sharedAchievement, unlockDate = LocalDate.now()))
        userAchievementRepository.save(UserAchievement(user = user1, achievement = exclusiveAchievement, unlockDate = LocalDate.now()))

        val shared = userAchievementRepository.findByAchievementId(sharedAchievement.id!!)
        val exclusive = userAchievementRepository.findByAchievementId(exclusiveAchievement.id!!)

        assertEquals(2, shared.size)
        assertEquals(1, exclusive.size)
    }
}
