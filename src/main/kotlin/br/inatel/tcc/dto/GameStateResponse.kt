package br.inatel.tcc.dto

enum class GameStatus {
    RUNNING,
    CAUGHT,
    ESCAPED
}

data class GameStateResponse(
    val sessionId: String,
    val userId: String,
    val playerPosition: Double,
    val hordePosition: Double,
    val distanceToGoal: Double,
    val distancePlayerToHorde: Double,
    val playerSpeed: Double,
    val hordeSpeed: Double,
    val raceProgress: Double,
    val gameStatus: GameStatus,
    val latencyTraceId: String? = null,
    val clientSentAtElapsedMs: Long? = null,
    val backendProcessingMs: Long? = null,
    val serverTimestampMs: Long = System.currentTimeMillis()
)
