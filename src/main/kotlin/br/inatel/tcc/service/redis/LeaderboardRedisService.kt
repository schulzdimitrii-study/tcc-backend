package br.inatel.tcc.service.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Gerencia o leaderboard da sessão de treino no Redis usando Sorted Sets (ZSET).
 *
 * Estrutura de keys Redis:
 *   session:{sessionId}:leaderboard    → ZSET  member=userId, score=distânciaKm
 *   session:{sessionId}:start          → STRING época em segundos do início da sessão
 *   session:{sessionId}:horde:pace     → STRING targetPace da Horda em min/km (opcional)
 *   session:{sessionId}:horde:adaptive → STRING "true" se horda é adaptativa (opcional)
 *
 * Por que ZSET para o leaderboard?
 *   - ZADD é O(log N) — atualiza posição e reordena em uma única operação
 *   - ZREVRANK retorna o rank do usuário em O(log N) sem varrer a lista inteira
 *   - ZREVRANGE retorna os top-N com scores em O(log N + N), ideal para top 10
 *
 */
@Service
class LeaderboardRedisService(
    private val redis: StringRedisTemplate
) {

    private fun leaderboardKey(sessionId: String) = "session:$sessionId:leaderboard"
    private fun startKey(sessionId: String) = "session:$sessionId:start"
    private fun hordeKey(sessionId: String) = "session:$sessionId:horde:pace"
    private fun hordeAdaptiveKey(sessionId: String) = "session:$sessionId:horde:adaptive"
    private fun goalDistanceKey(sessionId: String) = "session:$sessionId:goal:distance"
    private fun globalRankingKey(period: String) = "ranking:global:$period"

    private val ACTIVE_SESSIONS_KEY = "sessions:active"

    fun initSession(
        sessionId: String,
        targetPaceMinPerKm: Double?,
        isAdaptive: Boolean = false,
        estimatedDurationMin: Int? = null,
        goalDistanceKm: Double? = null
    ) {
        val startKey = startKey(sessionId)
        if (redis.hasKey(startKey) != true) {
            val epochSeconds = System.currentTimeMillis() / 1000
            redis.opsForValue().set(startKey, epochSeconds.toString(), Duration.ofHours(24))
            redis.opsForSet().add(ACTIVE_SESSIONS_KEY, sessionId)

            targetPaceMinPerKm?.let {
                redis.opsForValue().set(hordeKey(sessionId), it.toString(), Duration.ofHours(24))
            }

            if (isAdaptive) {
                redis.opsForValue().set(hordeAdaptiveKey(sessionId), "true", Duration.ofHours(24))
            }

            val goalDistance = goalDistanceKm
                ?.takeIf { it > 0.0 }
                ?: if (targetPaceMinPerKm != null && targetPaceMinPerKm > 0 && estimatedDurationMin != null) {
                    estimatedDurationMin.toDouble() / targetPaceMinPerKm
                } else {
                    null
                }

            goalDistance?.let {
                redis.opsForValue().set(goalDistanceKey(sessionId), it.toString(), Duration.ofHours(24))
            }
        }
    }

    fun getGoalDistance(sessionId: String): Double? {
        return redis.opsForValue().get(goalDistanceKey(sessionId))?.toDouble()
    }

    fun isHordeAdaptive(sessionId: String): Boolean =
        redis.opsForValue().get(hordeAdaptiveKey(sessionId)) == "true"

    fun updateHordePace(sessionId: String, pace: Double) {
        redis.opsForValue().set(hordeKey(sessionId), pace.toString(), Duration.ofHours(24))
    }

    fun updateUserDistance(sessionId: String, userId: String, distanceKm: Double) {
        redis.opsForZSet().add(leaderboardKey(sessionId), userId, distanceKm)
    }

    fun getUserRank(sessionId: String, userId: String): Long? {
        return redis.opsForZSet().reverseRank(leaderboardKey(sessionId), userId)
    }

    fun getTopEntries(sessionId: String, count: Long = 10): Set<ZSetOperations.TypedTuple<String>>? {
        return redis.opsForZSet().reverseRangeWithScores(leaderboardKey(sessionId), 0, count - 1)
    }

    fun getFullLeaderboard(sessionId: String): Set<ZSetOperations.TypedTuple<String>>? {
        return redis.opsForZSet().reverseRangeWithScores(leaderboardKey(sessionId), 0, -1)
    }

    fun getSessionStartEpoch(sessionId: String): Long? {
        return redis.opsForValue().get(startKey(sessionId))?.toLong()
    }

    fun getHordePace(sessionId: String): Double? {
        return redis.opsForValue().get(hordeKey(sessionId))?.toDouble()
    }

    fun incrementGlobalScore(period: String, userId: String, distanceKm: Double) {
        val key = globalRankingKey(period)
        redis.opsForZSet().incrementScore(key, userId, distanceKm)
        redis.expire(key, Duration.ofDays(90))
    }

    fun getGlobalRanking(period: String, count: Long = 50): Set<ZSetOperations.TypedTuple<String>>? {
        return redis.opsForZSet().reverseRangeWithScores(globalRankingKey(period), 0, count - 1)
    }

    fun expireSessionKeys(sessionId: String) {
        val ttl = Duration.ofHours(1)
        redis.expire(leaderboardKey(sessionId), ttl)
        redis.expire(startKey(sessionId), ttl)
        redis.expire(hordeKey(sessionId), ttl)
        redis.expire(hordeAdaptiveKey(sessionId), ttl)
        redis.expire(goalDistanceKey(sessionId), ttl)
        redis.opsForSet().remove(ACTIVE_SESSIONS_KEY, sessionId)
    }

    fun getActiveSessions(): Set<String> =
        redis.opsForSet().members(ACTIVE_SESSIONS_KEY) ?: emptySet()
}
