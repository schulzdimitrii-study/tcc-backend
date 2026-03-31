package br.inatel.tcc.domain.biometricdata

import br.inatel.tcc.domain.trainsession.TrainSession
import br.inatel.tcc.domain.trainsession.TrainSessionRepository
import br.inatel.tcc.domain.trainsession.TrainType
import br.inatel.tcc.domain.user.User
import br.inatel.tcc.domain.user.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = ["/test-cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BiometricDataRepositoryTest {

    @Autowired
    private lateinit var biometricDataRepository: BiometricDataRepository

    @Autowired
    private lateinit var trainSessionRepository: TrainSessionRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private fun savedSession(): TrainSession {
        val user = userRepository.save(User(email = "athlete@test.com", name = "Athlete", password = "encoded"))
        return trainSessionRepository.save(TrainSession(user = user, trainType = TrainType.RUN, startDate = LocalDateTime.now()))
    }

    @Test
    fun shouldFindByTrainSessionId() {
        val session1 = savedSession()
        val user2 = userRepository.save(User(email = "other@test.com", name = "Other", password = "encoded"))
        val session2 = trainSessionRepository.save(TrainSession(user = user2, trainType = TrainType.WALK, startDate = LocalDateTime.now()))

        repeat(3) { i ->
            biometricDataRepository.save(
                BiometricData(bpm = 130 + i, timestamp = LocalDateTime.now(), trainSession = session1)
            )
        }
        biometricDataRepository.save(BiometricData(bpm = 100, timestamp = LocalDateTime.now(), trainSession = session2))

        val session1Data = biometricDataRepository.findByTrainSessionId(session1.id!!)
        val session2Data = biometricDataRepository.findByTrainSessionId(session2.id!!)

        assertEquals(3, session1Data.size)
        assertEquals(1, session2Data.size)
    }

    @Test
    fun shouldPersistCardiacZone() {
        val session = savedSession()
        biometricDataRepository.save(
            BiometricData(bpm = 175, timestamp = LocalDateTime.now(), cardiacZone = CardiacZone.ZONE_4, trainSession = session)
        )

        val result = biometricDataRepository.findByTrainSessionId(session.id!!)

        assertEquals(1, result.size)
        assertEquals(CardiacZone.ZONE_4, result[0].cardiacZone)
        assertEquals(175, result[0].bpm)
    }
}
