package br.inatel.tcc.dto

import java.time.LocalDate
import java.util.UUID

data class UserResponseDto(
    val id: UUID,
    val email: String,
    val name: String,
    val birthdayDate: LocalDate?,
    val maxHeartRate: Int?,
    val height: Double?,
    val weight: Double?
)
