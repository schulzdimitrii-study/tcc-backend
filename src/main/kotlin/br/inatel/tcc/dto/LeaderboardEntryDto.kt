package br.inatel.tcc.dto

import br.inatel.tcc.domain.biometricdata.CardiacZone

/**
 * Representa a entrada de um usuário no leaderboard da sessão.
 * Populado a partir do ZSET Redis (ZREVRANGE WITHSCORES) e do HASH de estado biométrico.
 */
data class LeaderboardEntryDto(
    val userId: String,
    val rank: Int,
    val distanceKm: Double,
    val cardiacZone: CardiacZone? = null  // null quando usuário não tem maxHeartRate configurado
)
