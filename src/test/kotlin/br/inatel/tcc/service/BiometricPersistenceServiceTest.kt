package br.inatel.tcc.service

import br.inatel.tcc.domain.biometricdata.BiometricData
import br.inatel.tcc.domain.biometricdata.BiometricDataRepository
import br.inatel.tcc.domain.trainsession.TrainSession
import br.inatel.tcc.domain.trainsession.TrainSessionRepository
import br.inatel.tcc.domain.user.User
import br.inatel.tcc.dto.BiometricDataMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class BiometricPersistenceServiceTest {

    @Mock private lateinit var biometricDataRepository: BiometricDataRepository
    @Mock private lateinit var trainSessionRepository: TrainSessionRepository

    @InjectMocks private lateinit var service: BiometricPersistenceService

    private val sessionId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    private fun buildMessage(sid: String = sessionId.toString()) = BiometricDataMessage(
        sessionId = sid,
        userId = userId.toString(),
        timestamp = System.currentTimeMillis(),
        bpm = 155,
        cadence = 80.0,
        speed = 10.0,
        pace = 6.0,
        accumulatedDistance = 2.5,
        accumulatedCalories = 120.0
    )

    private fun buildSession() = TrainSession(
        id = sessionId,
        user = User(id = userId, email = "test@test.com", name = "Test", password = "enc")
    )

    @Test
    fun shouldSaveBiometricData_whenSessionExists() {
        val message = buildMessage()
        val session = buildSession()
        whenever(trainSessionRepository.findById(sessionId)).thenReturn(Optional.of(session))
        whenever(biometricDataRepository.save(any<BiometricData>())).thenAnswer { it.arguments[0] }

        service.persistAsync(message)

        verify(biometricDataRepository).save(any<BiometricData>())
    }

    @Test
    fun shouldNotSave_whenSessionNotFound() {
        val message = buildMessage()
        whenever(trainSessionRepository.findById(sessionId)).thenReturn(Optional.empty())

        service.persistAsync(message)

        verify(biometricDataRepository, never()).save(any())
    }

    @Test
    fun shouldNotSave_whenSessionIdIsInvalidUUID() {
        val message = buildMessage(sid = "not-a-uuid")

        service.persistAsync(message)

        verify(trainSessionRepository, never()).findById(any())
        verify(biometricDataRepository, never()).save(any())
    }
}
