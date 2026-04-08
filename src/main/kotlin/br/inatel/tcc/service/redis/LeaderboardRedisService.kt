package br.inatel.tcc.service.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Gerencia o leaderboard da sessão de treino no Redis usando Sorted Sets (ZSET).
 *
 * Estrutura de keys Redis:
 *   session:{sessionId}:leaderboard  → ZSET  member=userId, score=distânciaKm
 *   session:{sessionId}:start        → STRING época em segundos do início da sessão
 *   session:{sessionId}:horde:pace   → STRING targetPace da Horda em min/km (opcional)
 *
 * Por que ZSET para o leaderboard?
 *   - ZADD é O(log N) — atualiza posição e reordena em uma única operação
 *   - ZREVRANK retorna o rank do usuário em O(log N) sem varrer a lista inteira
 *   - ZREVRANGE retorna os top-N com scores em O(log N + N), ideal para top 10
 *
 * TODO [FASE 5 - RANKING GLOBAL]: Além do leaderboard por sessão, manter
 *   ZSET ranking:global:{yyyy-MM} com score acumulado por período mensal.
 *   Atualizar via ZADD com flag INCR ao encerrar cada sessão.
 */
@Service
class LeaderboardRedisService(
    private val redis: StringRedisTemplate
) {

    private fun leaderboardKey(sessionId: String) = "session:$sessionId:leaderboard"
    private fun startKey(sessionId: String) = "session:$sessionId:start"
    private fun hordeKey(sessionId: String) = "session:$sessionId:horde:pace"

    /**
     * Inicializa os metadados da sessão no Redis.
     * Chamado uma vez no POST /sessions/iniciar, logo após criar o TrainSession no PostgreSQL.
     *
     * Usa verificação de existência para ser idempotente — reconexões não sobrescrevem o start.
     */
    fun initSession(sessionId: String, targetPaceMinPerKm: Double?) {
        val startKey = startKey(sessionId)
        if (redis.hasKey(startKey) != true) {
            val epochSeconds = System.currentTimeMillis() / 1000
            redis.opsForValue().set(startKey, epochSeconds.toString(), Duration.ofHours(24))

            // Armazena o pace da Horda para evitar busca no PostgreSQL a cada update de biometria
            targetPaceMinPerKm?.let {
                redis.opsForValue().set(hordeKey(sessionId), it.toString(), Duration.ofHours(24))
            }
        }
    }

    /**
     * Atualiza a distância do usuário no leaderboard.
     * ZADD sobrescreve o score anterior — sempre reflete a posição atual, não acumulada dupla.
     */
    fun updateUserDistance(sessionId: String, userId: String, distanceKm: Double) {
        redis.opsForZSet().add(leaderboardKey(sessionId), userId, distanceKm)
    }

    /**
     * Retorna o rank 0-based do usuário (0 = primeiro lugar).
     * ZREVRANK: ordena do maior score (maior distância) para o menor.
     */
    fun getUserRank(sessionId: String, userId: String): Long? {
        return redis.opsForZSet().reverseRank(leaderboardKey(sessionId), userId)
    }

    /** Retorna os top-N usuários com seus scores (ZREVRANGE WITHSCORES). */
    fun getTopEntries(sessionId: String, count: Long = 10): Set<ZSetOperations.TypedTuple<String>>? {
        return redis.opsForZSet().reverseRangeWithScores(leaderboardKey(sessionId), 0, count - 1)
    }

    /** Retorna o leaderboard completo — usado no flush final para o PostgreSQL. */
    fun getFullLeaderboard(sessionId: String): Set<ZSetOperations.TypedTuple<String>>? {
        return redis.opsForZSet().reverseRangeWithScores(leaderboardKey(sessionId), 0, -1)
    }

    fun getSessionStartEpoch(sessionId: String): Long? {
        return redis.opsForValue().get(startKey(sessionId))?.toLong()
    }

    fun getHordePace(sessionId: String): Double? {
        return redis.opsForValue().get(hordeKey(sessionId))?.toDouble()
    }

    /**
     * Reduz o TTL dos keys da sessão para 1h após encerramento.
     * Mantém dados disponíveis brevemente para consultas pós-corrida, sem ocupar memória indefinidamente.
     */
    fun expireSessionKeys(sessionId: String) {
        val ttl = Duration.ofHours(1)
        redis.expire(leaderboardKey(sessionId), ttl)
        redis.expire(startKey(sessionId), ttl)
        redis.expire(hordeKey(sessionId), ttl)
    }
}
