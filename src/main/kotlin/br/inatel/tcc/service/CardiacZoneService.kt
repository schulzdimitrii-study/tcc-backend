package br.inatel.tcc.service

import br.inatel.tcc.domain.biometricdata.CardiacZone
import br.inatel.tcc.domain.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

/**
 * Calcula a zona cardíaca do usuário com base no BPM atual e no FC máximo (maxHeartRate).
 *
 * O maxHeartRate é buscado do PostgreSQL na primeira mensagem de cada usuário por sessão
 * e cacheado em Redis (TTL 24h) para evitar query a cada update de biometria.
 *
 * Zonas (% do FC máximo):
 *   ZONE_1 → < 60%  (repouso / recuperação ativa)
 *   ZONE_2 → 60-70% (queima de gordura)
 *   ZONE_3 → 70-80% (aeróbico)
 *   ZONE_4 → 80-90% (anaeróbico)
 *   ZONE_5 → ≥ 90%  (máximo / VO2 max)
 */
@Service
class CardiacZoneService(
    private val userRepository: UserRepository,
    private val redis: StringRedisTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private fun maxHrKey(sessionId: String, userId: String) = "session:$sessionId:user:$userId:maxhr"

    /**
     * Retorna o FC máximo do usuário, usando Redis como cache.
     * Na primeira chamada por sessão/usuário: consulta o PostgreSQL e armazena o resultado.
     *
     * @return maxHeartRate do usuário, ou null se não cadastrado ou userId inválido
     */
    fun getOrCacheMaxHr(sessionId: String, userId: String): Int? {
        val key = maxHrKey(sessionId, userId)
        val cached = redis.opsForValue().get(key)
        if (cached != null) return cached.toIntOrNull()

        val userUuid = runCatching { UUID.fromString(userId) }.getOrNull() ?: return null
        val maxHr = userRepository.findById(userUuid).orElse(null)?.maxHeartRate
        if (maxHr == null) {
            log.debug("[CARDIAC] maxHeartRate não definido para userId={}", userId)
            return null
        }
        redis.opsForValue().set(key, maxHr.toString(), Duration.ofHours(24))
        return maxHr
    }

    /**
     * Calcula a zona cardíaca com base no BPM atual e no FC máximo.
     */
    fun calculate(bpm: Int, maxHr: Int): CardiacZone {
        val pct = bpm.toDouble() / maxHr
        return when {
            pct < 0.60 -> CardiacZone.ZONE_1
            pct < 0.70 -> CardiacZone.ZONE_2
            pct < 0.80 -> CardiacZone.ZONE_3
            pct < 0.90 -> CardiacZone.ZONE_4
            else        -> CardiacZone.ZONE_5
        }
    }
}
