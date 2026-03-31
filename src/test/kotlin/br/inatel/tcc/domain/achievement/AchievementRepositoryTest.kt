package br.inatel.tcc.domain.achievement

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = ["/test-cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AchievementRepositoryTest {

    @Autowired
    private lateinit var achievementRepository: AchievementRepository

    @Test
    fun shouldFindActiveAchievements() {
        achievementRepository.save(Achievement(title = "Primeira Corrida", active = true))
        achievementRepository.save(Achievement(title = "10km Completos", active = true))
        achievementRepository.save(Achievement(title = "Conquista Removida", active = false))

        val active = achievementRepository.findByActive(true)

        assertEquals(2, active.size)
        assertTrue(active.all { it.active })
    }

    @Test
    fun shouldFindInactiveAchievements() {
        achievementRepository.save(Achievement(title = "Ativo", active = true))
        achievementRepository.save(Achievement(title = "Inativo 1", active = false))
        achievementRepository.save(Achievement(title = "Inativo 2", active = false))

        val inactive = achievementRepository.findByActive(false)

        assertEquals(2, inactive.size)
        assertTrue(inactive.none { it.active })
    }

    @Test
    fun shouldSaveAndFindAchievementWithAllFields() {
        val achievement = achievementRepository.save(
            Achievement(
                title = "Maratonista",
                description = "Complete 42km em uma sessão",
                urlIcon = "https://example.com/icon.png",
                criterion = "totalDistance >= 42000",
                active = true
            )
        )

        val found = achievementRepository.findById(achievement.id!!)

        assertTrue(found.isPresent)
        assertEquals("Maratonista", found.get().title)
        assertEquals("Complete 42km em uma sessão", found.get().description)
        assertEquals("criterion = totalDistance >= 42000", "criterion = ${found.get().criterion}")
        assertTrue(found.get().active)
    }
}
