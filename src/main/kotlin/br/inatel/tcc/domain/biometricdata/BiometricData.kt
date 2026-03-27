package br.inatel.tcc.domain.biometricdata

import br.inatel.tcc.domain.trainsession.TrainSession
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "biometric_data")
@Entity
class BiometricData(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val bpm: Int = 0,

    val cadence: Double? = null,

    val speed: Double? = null,

    val pace: Double? = null,

    val accumulatedDistance: Double? = null,

    val accumulatedCalories: Double? = null,

    @Enumerated(EnumType.STRING)
    val cardiacZone: CardiacZone? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_session_id", nullable = false)
    val trainSession: TrainSession
)
