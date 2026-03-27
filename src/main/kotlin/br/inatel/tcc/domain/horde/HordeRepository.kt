package br.inatel.tcc.domain.horde

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface HordeRepository : JpaRepository<Horde, UUID> {
    fun findByDifficulty(difficulty: HordeDifficulty): List<Horde>
}
