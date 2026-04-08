package br.inatel.tcc.service.redis

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
 * TODO [FASE 4 - ZONA CARDÍACA]: Calcular e armazenar cardiacZone neste HASH
 *   com base no bpm recebido e no User.maxHeartRate (buscar do PostgreSQL no initSession
 *   e cachear em Redis para evitar query a cada update).
 *   Referência: domain/biometricdata/CardiacZone.kt
 */
@Service
class SessionRedisService(
    private val redis: StringRedisTemplate
) {

    private fun userKey(sessionId: String, userId: String) = "session:$sessionId:user:$userId"

    /** Salva ou atualiza o estado biométrico atual do usuário (HMSET). */
    fun saveUserState(sessionId: String, userId: String, message: BiometricDataMessage) {
        val key = userKey(sessionId, userId)
        val fields = mapOf(
            "bpm"       to message.bpm.toString(),
            "speed"     to message.speed.toString(),
            "pace"      to message.pace.toString(),
            "distance"  to message.accumulatedDistance.toString(),
            "cadence"   to message.cadence.toString(),
            "calories"  to message.accumulatedCalories.toString(),
            "timestamp" to message.timestamp.toString()
        )
        redis.opsForHash<String, String>().putAll(key, fields)
        redis.expire(key, Duration.ofHours(24))
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
}
