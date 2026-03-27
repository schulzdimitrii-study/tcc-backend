package br.inatel.tcc.domain.trainsession

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TrainSessionRepository : JpaRepository<TrainSession, UUID> {
    fun findByUserId(userId: UUID): List<TrainSession>
    fun findByUserIdAndHordeId(userId: UUID, hordeId: UUID): List<TrainSession>
}
