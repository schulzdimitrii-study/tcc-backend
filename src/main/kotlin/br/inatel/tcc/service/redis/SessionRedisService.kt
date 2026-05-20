package br.inatel.tcc.service.redis

import br.inatel.tcc.domain.biometricdata.CardiacZone
import br.inatel.tcc.dto.BiometricDataMessage
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Armazena o estado biométrico instantâneo de cada usuário na sessão usando Redis Hash.
 *
 * Estrutura de key Redis:
 *   session:{sessionId}:user:{userId} → HASH
 *     bpm, speed, pace, distance, cadence, calories, timestamp
 *
 * Por que HASH e não String JSON?
 *   - HGETALL retorna todos os campos sem desserialização manual
 *   - HMSET atualiza campos individuais sem reescrever o objeto inteiro
 *   - Permite leituras parciais (HGET "bpm") para dashboards específicos
 *
 */
@Service
class SessionRedisService(
    private val redis: StringRedisTemplate
) {

    private fun userKey(sessionId: String, userId: String) = "session:$sessionId:user:$userId"

    /** Salva ou atualiza o estado biométrico atual do usuário (HMSET). */
    fun saveUserState(sessionId: String, userId: String, message: BiometricDataMessage, cardiacZone: CardiacZone? = null) {
        val key = userKey(sessionId, userId)
        val fields = mutableMapOf(
            "bpm"       to message.bpm.toString(),
            "speed"     to message.speed.toString(),
            "pace"      to message.pace.toString(),
            "distance"  to message.accumulatedDistance.toString(),
            "cadence"   to message.cadence.toString(),
            "calories"  to message.accumulatedCalories.toString(),
            "timestamp" to message.timestamp.toString()
        )
        cardiacZone?.let { fields["cardiacZone"] = it.name }
        redis.opsForHash<String, String>().putAll(key, fields)
        redis.expire(key, Duration.ofHours(24))
    }

    /**
     * Retorna o mapa de userId → CardiacZone para os usuários fornecidos.
     * Lê o campo "cardiacZone" do HASH de cada usuário.
     */
    fun getCardiacZones(sessionId: String, userIds: List<String>): Map<String, CardiacZone?> =
        userIds.associateWith { userId ->
            getUserState(sessionId, userId)["cardiacZone"]
                ?.let { runCatching { CardiacZone.valueOf(it) }.getOrNull() }
        }

    /** Lê o último estado biométrico do usuário na sessão. */
    fun getUserState(sessionId: String, userId: String): Map<String, String> {
        val key = userKey(sessionId, userId)
        @Suppress("UNCHECKED_CAST")
        return redis.opsForHash<String, String>().entries(key) as Map<String, String>
    }

    fun expireUserKey(sessionId: String, userId: String) {
        redis.expire(userKey(sessionId, userId), Duration.ofHours(1))
    }

    /**
     * Calcula o pace médio dos usuários ativos na sessão.
     * Usado pelo modo adaptativo de horda para atualizar o pace em tempo real.
     *
     * @param userIds Lista de userIds ativos (obtida do leaderboard ZSET)
     * @return Média do pace em min/km, ou null se nenhum usuário tiver pace válido
     */
    fun getAveragePace(sessionId: String, userIds: List<String>): Double? {
        val paces = userIds.mapNotNull { userId ->
            getUserState(sessionId, userId)["pace"]?.toDoubleOrNull()
        }.filter { it > 0 }
        return if (paces.isEmpty()) null else paces.average()
    }
}
