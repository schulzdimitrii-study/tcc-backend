package br.inatel.tcc.domain.trainsession

import br.inatel.tcc.domain.biometricdata.BiometricData
import br.inatel.tcc.domain.horde.Horde
import br.inatel.tcc.domain.user.User
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "train_sessions")
@Entity
class TrainSession(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    val startDate: LocalDateTime = LocalDateTime.now(),

    val endDate: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val trainType: TrainType = TrainType.RUN,

    val totalDistance: Double? = null,

    val estimatedCalories: Double? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horde_id", nullable = true)
    val horde: Horde? = null,

    @OneToMany(mappedBy = "trainSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val biometricData: MutableList<BiometricData> = mutableListOf()
)
