package br.inatel.tcc.domain.horde

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = ["/test-cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class HordeRepositoryTest {

    @Autowired
    private lateinit var hordeRepository: HordeRepository

    @Test
    fun shouldSaveAndFindHorde() {
        val horde = hordeRepository.save(
            Horde(name = "Horda do Amanhecer", difficulty = HordeDifficulty.MEDIUM, estimatedDuration = 30)
        )

        val found = hordeRepository.findById(horde.id!!)

        assertTrue(found.isPresent)
        assertEquals("Horda do Amanhecer", found.get().name)
        assertEquals(HordeDifficulty.MEDIUM, found.get().difficulty)
        assertEquals(30, found.get().estimatedDuration)
    }

    @Test
    fun shouldFindByDifficulty() {
        hordeRepository.save(Horde(name = "Fácil 1", difficulty = HordeDifficulty.EASY, estimatedDuration = 20))
        hordeRepository.save(Horde(name = "Fácil 2", difficulty = HordeDifficulty.EASY, estimatedDuration = 25))
        hordeRepository.save(Horde(name = "Difícil 1", difficulty = HordeDifficulty.HARD, estimatedDuration = 60))

        val easyHordes = hordeRepository.findByDifficulty(HordeDifficulty.EASY)
        val hardHordes = hordeRepository.findByDifficulty(HordeDifficulty.HARD)

        assertEquals(2, easyHordes.size)
        assertEquals(1, hardHordes.size)
        assertTrue(easyHordes.all { it.difficulty == HordeDifficulty.EASY })
    }

    @Test
    fun shouldSaveHordeWithParentHorde() {
        val parent = hordeRepository.save(
            Horde(name = "Horda Mãe", difficulty = HordeDifficulty.MEDIUM, estimatedDuration = 45)
        )
        val child = hordeRepository.save(
            Horde(name = "Horda Filha", difficulty = HordeDifficulty.EASY, estimatedDuration = 20, parentHorde = parent)
        )

        val found = hordeRepository.findById(child.id!!)

        assertTrue(found.isPresent)
        assertNotNull(found.get().parentHorde)
        assertEquals("Horda Mãe", found.get().parentHorde?.name)
    }
}
