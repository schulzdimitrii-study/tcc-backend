package br.inatel.tcc.service.redis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.ZSetOperations
import java.time.Duration

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeaderboardRedisServiceTest {

    @Mock private lateinit var redis: StringRedisTemplate
    @Mock private lateinit var valueOps: ValueOperations<String, String>
    @Mock private lateinit var zSetOps: ZSetOperations<String, String>

    private lateinit var service: LeaderboardRedisService

    private val sessionId = "session-abc-123"
    private val leaderboardKey = "session:$sessionId:leaderboard"
    private val startKey = "session:$sessionId:start"
    private val hordeKey = "session:$sessionId:horde:pace"

    @BeforeEach
    fun setUp() {
        whenever(redis.opsForValue()).thenReturn(valueOps)
        whenever(redis.opsForZSet()).thenReturn(zSetOps)
        service = LeaderboardRedisService(redis)
    }

    // ─── initSession ──────────────────────────────────────────────────────────

    @Test
    fun shouldInitSession_whenKeyDoesNotExist() {
        whenever(redis.hasKey(startKey)).thenReturn(false)

        service.initSession(sessionId, 6.0)

        verify(valueOps).set(eq(startKey), any(), eq(Duration.ofHours(24)))
        verify(valueOps).set(eq(hordeKey), eq("6.0"), eq(Duration.ofHours(24)))
    }

    @Test
    fun shouldBeIdempotent_whenKeyAlreadyExists() {
        whenever(redis.hasKey(startKey)).thenReturn(true)

        service.initSession(sessionId, 6.0)

        verify(valueOps, never()).set(any(), any(), any<Duration>())
    }

    @Test
    fun shouldInitSession_withoutHordePace() {
        whenever(redis.hasKey(startKey)).thenReturn(false)

        service.initSession(sessionId, null)

        verify(valueOps).set(eq(startKey), any(), eq(Duration.ofHours(24)))
        verify(valueOps, never()).set(eq(hordeKey), any(), any<Duration>())
    }

    // ─── updateUserDistance ───────────────────────────────────────────────────

    @Test
    fun shouldUpdateUserDistance() {
        service.updateUserDistance(sessionId, "user-1", 3.5)

        verify(zSetOps).add(leaderboardKey, "user-1", 3.5)
    }

    // ─── getUserRank ──────────────────────────────────────────────────────────

    @Test
    fun shouldGetUserRank() {
        whenever(zSetOps.reverseRank(leaderboardKey, "user-1")).thenReturn(0L)

        val rank = service.getUserRank(sessionId, "user-1")

        assertEquals(0L, rank)
    }

    // ─── getTopEntries ────────────────────────────────────────────────────────

    @Test
    fun shouldGetTopEntries_defaultCount10() {
        service.getTopEntries(sessionId)

        verify(zSetOps).reverseRangeWithScores(leaderboardKey, 0L, 9L)
    }

    @Test
    fun shouldGetTopEntries_withCustomCount() {
        service.getTopEntries(sessionId, count = 3)

        verify(zSetOps).reverseRangeWithScores(leaderboardKey, 0L, 2L)
    }

    // ─── getFullLeaderboard ───────────────────────────────────────────────────

    @Test
    fun shouldGetFullLeaderboard() {
        service.getFullLeaderboard(sessionId)

        verify(zSetOps).reverseRangeWithScores(leaderboardKey, 0L, -1L)
    }

    // ─── getSessionStartEpoch ─────────────────────────────────────────────────

    @Test
    fun shouldGetSessionStartEpoch() {
        whenever(valueOps.get(startKey)).thenReturn("1711900000")

        val epoch = service.getSessionStartEpoch(sessionId)

        assertEquals(1711900000L, epoch)
    }

    @Test
    fun shouldReturnNull_whenStartEpochKeyMissing() {
        whenever(valueOps.get(startKey)).thenReturn(null)

        val epoch = service.getSessionStartEpoch(sessionId)

        assertNull(epoch)
    }

    // ─── getHordePace ─────────────────────────────────────────────────────────

    @Test
    fun shouldGetHordePace() {
        whenever(valueOps.get(hordeKey)).thenReturn("6.5")

        val pace = service.getHordePace(sessionId)

        assertEquals(6.5, pace)
    }

    // ─── expireSessionKeys ────────────────────────────────────────────────────

    @Test
    fun shouldExpireAllThreeKeys() {
        service.expireSessionKeys(sessionId)

        verify(redis, times(3)).expire(any(), eq(Duration.ofHours(1)))
        verify(redis).expire(leaderboardKey, Duration.ofHours(1))
        verify(redis).expire(startKey, Duration.ofHours(1))
        verify(redis).expire(hordeKey, Duration.ofHours(1))
    }
}
