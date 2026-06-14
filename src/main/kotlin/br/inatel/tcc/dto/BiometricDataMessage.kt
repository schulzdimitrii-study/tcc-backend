package br.inatel.tcc.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

/**
 * Mensagem recebida via WebSocket STOMP do app React Native (companion do Galaxy Watch).
 *
 * O app React Native recebe esses dados do relógio via Wear OS Data Layer API (Bluetooth)
 * e repassa ao backend pelo canal /app/train/data.
 */
data class BiometricDataMessage(
    @field:NotBlank val sessionId: String,
    @field:NotBlank val userId: String,
    @field:Positive val timestamp: Long,           // Epoch ms — gerado pelo relógio
    @field:Min(1) val bpm: Int,                    // Frequência cardíaca (batimentos por minuto)
    @field:PositiveOrZero val cadence: Double,     // Passadas por minuto
    @field:PositiveOrZero val speed: Double,       // Velocidade em km/h
    @field:PositiveOrZero val pace: Double,        // Ritmo em min/km
    @field:PositiveOrZero val accumulatedDistance: Double, // Distância total percorrida na sessão em km
    @field:PositiveOrZero val accumulatedCalories: Double, // Calorias estimadas acumuladas
    val latencyTraceId: String? = null,
    val clientSentAtElapsedMs: Long? = null
)
