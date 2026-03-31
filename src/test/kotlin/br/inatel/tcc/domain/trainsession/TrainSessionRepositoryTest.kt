package br.inatel.tcc.domain.trainsession

import br.inatel.tcc.domain.horde.Horde
import br.inatel.tcc.domain.horde.HordeDifficulty
import br.inatel.tcc.domain.horde.HordeRepository
import br.inatel.tcc.domain.user.User
import br.inatel.tcc.domain.user.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = ["/test-cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TrainSessionRepositoryTest {

    @Autowired
    private lateinit var trainSessionRepository: TrainSessionRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var hordeRepository: HordeRepository

    private fun savedUser(email: String = "runner@test.com") = userRepository.save(
        User(email = email, name = "Runner", password = "encoded_password")
    )

    @Test
    fun shouldFindByUserId() {
        val user1 = savedUser("user1@test.com")
        val user2 = savedUser("user2@test.com")

        trainSessionRepository.save(TrainSession(user = user1, trainType = TrainType.RUN, startDate = LocalDateTime.now()))
        trainSessionRepository.save(TrainSession(user = user1, trainType = TrainType.WALK, startDate = LocalDateTime.now()))
        trainSessionRepository.save(TrainSession(user = user2, trainType = TrainType.CYCLE, startDate = LocalDateTime.now()))

        val user1Sessions = trainSessionRepository.findByUserId(user1.id!!)
        val user2Sessions = trainSessionRepository.findByUserId(user2.id!!)

        assertEquals(2, user1Sessions.size)
        assertEquals(1, user2Sessions.size)
    }

    @Test
    fun shouldFindByUserIdAndHordeId() {
        val user = savedUser()
        val horde = hordeRepository.save(Horde(name = "Horda Teste", difficulty = HordeDifficulty.MEDIUM, estimatedDuration = 30))

        trainSessionRepository.save(TrainSession(user = user, trainType = TrainType.RUN, startDate = LocalDateTime.now(), horde = horde))
        trainSessionRepository.save(TrainSession(user = user, trainType = TrainType.WALK, startDate = LocalDateTime.now()))

        val result = trainSessionRepository.findByUserIdAndHordeId(user.id!!, horde.id!!)

        assertEquals(1, result.size)
        assertEquals(horde.id, result[0].horde?.id)
    }

    @Test
    fun shouldReturnEmptyListForUnknownUser() {
        val result = trainSessionRepository.findByUserId(UUID.randomUUID())

        assertTrue(result.isEmpty())
    }
}
