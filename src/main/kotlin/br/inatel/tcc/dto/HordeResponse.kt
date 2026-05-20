package br.inatel.tcc.dto

import br.inatel.tcc.domain.horde.HordeDifficulty
import java.util.UUID

data class HordeResponse(
    val id: UUID?,
    val name: String,
    val description: String?,
    val difficulty: HordeDifficulty,
    val estimatedDuration: Int,
    val targetPace: Double?
)
