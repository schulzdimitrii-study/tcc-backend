package br.inatel.tcc.dto

/**
 * Representa a entrada de um usuário no leaderboard da sessão.
 * Populado a partir do ZSET Redis (ZREVRANGE WITHSCORES).
 *
 * TODO [FASE 4 - ZONA CARDÍACA]: Adicionar campo cardiacZone (CardiacZone enum)
 * para exibir a zona de esforço de cada competidor em tempo real no app.
 */
data class LeaderboardEntryDto(
    val userId: String,
    val rank: Int,
    val distanceKm: Double
)
