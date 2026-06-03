package br.inatel.tcc.dto

import java.time.LocalDate
import java.util.UUID

data class RankingResponseDto(
    val id: UUID,
    val userId: UUID,
    val userName: String,
    val position: Int,
    val score: Double,
    val period: String,
    val calculeDate: LocalDate,
    val targetDistance: Double?
)
