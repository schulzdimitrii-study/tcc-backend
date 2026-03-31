package br.inatel.tcc.domain.ranking

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
class RankingRepositoryTest {

    @Autowired
    private lateinit var rankingRepository: RankingRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private fun savedUser(email: String) = userRepository.save(
        User(email = email, name = "Player", password = "encoded")
    )

    @Test
    fun shouldFindByPeriodOrderedByPosition() {
        val user1 = savedUser("p1@test.com")
        val user2 = savedUser("p2@test.com")
        val user3 = savedUser("p3@test.com")

        rankingRepository.save(Ranking(user = user1, position = 3, score = 100.0, period = "2026-03", calculeDate = LocalDate.now()))
        rankingRepository.save(Ranking(user = user2, position = 1, score = 300.0, period = "2026-03", calculeDate = LocalDate.now()))
        rankingRepository.save(Ranking(user = user3, position = 2, score = 200.0, period = "2026-03", calculeDate = LocalDate.now()))
        rankingRepository.save(Ranking(user = user1, position = 1, score = 500.0, period = "2026-02", calculeDate = LocalDate.now()))

        val march2026 = rankingRepository.findByPeriodOrderByPositionAsc("2026-03")

        assertEquals(3, march2026.size)
        assertEquals(1, march2026[0].position)
        assertEquals(2, march2026[1].position)
        assertEquals(3, march2026[2].position)
    }

    @Test
    fun shouldFindByUserId() {
        val user1 = savedUser("athlete@test.com")
        val user2 = savedUser("other@test.com")

        rankingRepository.save(Ranking(user = user1, position = 1, score = 500.0, period = "2026-01", calculeDate = LocalDate.now()))
        rankingRepository.save(Ranking(user = user1, position = 2, score = 450.0, period = "2026-02", calculeDate = LocalDate.now()))
        rankingRepository.save(Ranking(user = user2, position = 1, score = 600.0, period = "2026-01", calculeDate = LocalDate.now()))

        val user1Rankings = rankingRepository.findByUserId(user1.id!!)

        assertEquals(2, user1Rankings.size)
        assertTrue(user1Rankings.all { it.user.id == user1.id })
    }
}
