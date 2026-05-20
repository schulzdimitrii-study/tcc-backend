package br.inatel.tcc.dto

import java.time.LocalDate
import java.util.UUID

data class UserResponseDto(
    val id: UUID,
    val email: String,
    val name: String,
    val birthdayDate: LocalDate?,
    val maxHeartRate: Int?,
    var height: Double?,
    var weight: Double?
) {
    init {
        height = height?.let { Math.round(it * 100) / 100.0 }
        weight = weight?.let { Math.round(it * 100) / 100.0 }
    }
}
