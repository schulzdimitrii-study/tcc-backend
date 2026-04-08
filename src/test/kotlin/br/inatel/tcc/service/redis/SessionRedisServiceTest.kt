package br.inatel.tcc.service.redis

import br.inatel.tcc.dto.BiometricDataMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionRedisServiceTest {

    @Mock private lateinit var redis: StringRedisTemplate

    @Suppress("UNCHECKED_CAST")
    @Mock private lateinit var hashOps: HashOperations<String, Any, Any>

    private lateinit var service: SessionRedisService

    private val sessionId = "session-abc-123"
    private val userId = "user-xyz-456"
    private val expectedKey = "session:$sessionId:user:$userId"

    @BeforeEach
    fun setUp() {
        @Suppress("UNCHECKED_CAST")
        whenever(redis.opsForHash<Any, Any>()).thenReturn(hashOps)
        service = SessionRedisService(redis)
    }

    private fun buildMessage() = BiometricDataMessage(
        sessionId = sessionId,
        userId = userId,
        timestamp = 1711900000000L,
        bpm = 152,
        cadence = 85.0,
        speed = 9.5,
        pace = 6.3,
        accumulatedDistance = 2.1,
        accumulatedCalories = 180.0
    )

    // ─── saveUserState ────────────────────────────────────────────────────────

    @Test
    fun shouldSaveAllBiometricFields() {
        val message = buildMessage()
        val expectedFields: Map<Any, Any> = mapOf(
            "bpm"       to "152",
            "speed"     to "9.5",
            "pace"      to "6.3",
            "distance"  to "2.1",
            "cadence"   to "85.0",
            "calories"  to "180.0",
            "timestamp" to "1711900000000"
        )

        service.saveUserState(sessionId, userId, message)

        verify(hashOps).putAll(expectedKey, expectedFields)
    }

    @Test
    fun shouldSetExpiry24h_onSave() {
        service.saveUserState(sessionId, userId, buildMessage())

        verify(redis).expire(expectedKey, Duration.ofHours(24))
    }

    // ─── getUserState ─────────────────────────────────────────────────────────

    @Test
    fun shouldGetUserState() {
        val storedFields: Map<Any, Any> = mapOf("bpm" to "152", "speed" to "9.5")
        whenever(hashOps.entries(expectedKey)).thenReturn(storedFields)

        val result = service.getUserState(sessionId, userId)

        verify(hashOps).entries(expectedKey)
        assertEquals(storedFields, result)
    }

    // ─── expireUserKey ────────────────────────────────────────────────────────

    @Test
    fun shouldExpireUserKey_with1h() {
        service.expireUserKey(sessionId, userId)

        verify(redis).expire(eq(expectedKey), eq(Duration.ofHours(1)))
    }
}
