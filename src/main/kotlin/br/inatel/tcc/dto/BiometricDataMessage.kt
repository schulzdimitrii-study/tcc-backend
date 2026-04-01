package br.inatel.tcc.dto

/**
 * Mensagem recebida via WebSocket STOMP do app React Native (companion do Galaxy Watch).
 *
 * O app React Native recebe esses dados do relógio via Wear OS Data Layer API (Bluetooth)
 * e repassa ao backend pelo canal /app/treino/dados.
 *
 * TODO [FASE 2 - VALIDAÇÃO]: Adicionar validações (@NotBlank, @Min) para garantir
 * que dados malformados do relógio não corrompam o leaderboard no Redis.
 */
data class BiometricDataMessage(
    val sessionId: String,
    val userId: String,
    val timestamp: Long,           // Epoch ms — gerado pelo relógio
    val bpm: Int,                  // Frequência cardíaca (batimentos por minuto)
    val cadence: Double,           // Passadas por minuto
    val speed: Double,             // Velocidade em km/h
    val pace: Double,              // Ritmo em min/km
    val accumulatedDistance: Double, // Distância total percorrida na sessão em km
    val accumulatedCalories: Double  // Calorias estimadas acumuladas
)
