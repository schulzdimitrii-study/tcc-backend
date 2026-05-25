package br.inatel.tcc.service

import br.inatel.tcc.domain.biometricdata.CardiacZone
import br.inatel.tcc.domain.user.User
import br.inatel.tcc.domain.user.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CardiacZoneServiceTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var redis: StringRedisTemplate
    @Mock private lateinit var valueOps: ValueOperations<String, String>

    @InjectMocks private lateinit var service: CardiacZoneService

    private val sessionId = "session-abc"
    private val userId = UUID.randomUUID()
    private val maxHrKey = "session:$sessionId:user:$userId:maxhr"

    // ─── calculate ────────────────────────────────────────────────────────────

    @Test fun shouldReturnZone1_whenBpmBelow60Pct() = assertEquals(CardiacZone.ZONE_1, service.calculate(100, 200))
    @Test fun shouldReturnZone2_whenBpmBetween60And70Pct() = assertEquals(CardiacZone.ZONE_2, service.calculate(130, 200))
    @Test fun shouldReturnZone3_whenBpmBetween70And80Pct() = assertEquals(CardiacZone.ZONE_3, service.calculate(150, 200))
    @Test fun shouldReturnZone4_whenBpmBetween80And90Pct() = assertEquals(CardiacZone.ZONE_4, service.calculate(170, 200))
    @Test fun shouldReturnZone5_whenBpmAt90PctOrAbove() = assertEquals(CardiacZone.ZONE_5, service.calculate(185, 200))

    // ─── getOrCacheMaxHr ──────────────────────────────────────────────────────

    @Test
    fun shouldReturnCachedMaxHr_whenPresentInRedis() {
        whenever(redis.opsForValue()).thenReturn(valueOps)
        whenever(valueOps.get(maxHrKey)).thenReturn("180")

        val result = service.getOrCacheMaxHr(sessionId, userId.toString())

        assertEquals(180, result)
        verify(userRepository, never()).findById(any())
    }

    @Test
    fun shouldQueryDbAndCache_whenNotInRedis() {
        val user = User(id = userId, email = "a@b.com", name = "A", password = "p", maxHeartRate = 190)
        whenever(redis.opsForValue()).thenReturn(valueOps)
        whenever(valueOps.get(maxHrKey)).thenReturn(null)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

        val result = service.getOrCacheMaxHr(sessionId, userId.toString())

        assertEquals(190, result)
        verify(valueOps).set(eq(maxHrKey), eq("190"), any<Duration>())
    }

    @Test
    fun shouldReturnNull_whenUserHasNoMaxHeartRate() {
        val user = User(id = userId, email = "a@b.com", name = "A", password = "p", maxHeartRate = null)
        whenever(redis.opsForValue()).thenReturn(valueOps)
        whenever(valueOps.get(maxHrKey)).thenReturn(null)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

        val result = service.getOrCacheMaxHr(sessionId, userId.toString())

        assertNull(result)
        verify(valueOps, never()).set(any<String>(), any<String>(), any<Duration>())
    }

    @Test
    fun shouldReturnNull_whenUserNotFound() {
        whenever(redis.opsForValue()).thenReturn(valueOps)
        whenever(valueOps.get(maxHrKey)).thenReturn(null)
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

        val result = service.getOrCacheMaxHr(sessionId, userId.toString())

        assertNull(result)
    }

    @Test
    fun shouldReturnNull_whenUserIdIsInvalidUUID() {
        whenever(redis.opsForValue()).thenReturn(valueOps)
        whenever(valueOps.get(any())).thenReturn(null)

        val result = service.getOrCacheMaxHr(sessionId, "not-a-uuid")

        assertNull(result)
        verify(userRepository, never()).findById(any())
    }
}
