package br.inatel.tcc.dto

/**
 * Resposta broadcast enviada via WebSocket para todos os inscritos em
 * /topic/session/{sessionId}/leaderboard após cada update de biometria.
 *
 * O app React Native usa esse payload para atualizar a UI de corrida em tempo real.
 *
 * TODO [FASE 4 - AMIGOS]: Adicionar campo isBehindHorde (Boolean) e
 * distanceToHorde (Double) para facilitar a exibição no mapa do app.
 */
data class LeaderboardResponse(
    val sessionId: String,
    val userRank: Int,
    // null quando a sessão não está associada a uma Horda (treino livre)
    val hordeVirtualDistanceKm: Double?,
    val entries: List<LeaderboardEntryDto>
)
