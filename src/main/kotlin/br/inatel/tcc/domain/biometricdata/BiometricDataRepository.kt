package br.inatel.tcc.domain.biometricdata

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BiometricDataRepository : JpaRepository<BiometricData, UUID> {
    fun findByTrainSessionId(trainSessionId: UUID): List<BiometricData>
}
