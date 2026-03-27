package br.inatel.tcc.domain.horde

import jakarta.persistence.*
import java.util.UUID

@Table(name = "hordes")
@Entity
class Horde(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val name: String = "",

    val description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val difficulty: HordeDifficulty = HordeDifficulty.MEDIUM,

    @Column(nullable = false)
    val estimatedDuration: Int = 0,

    val targetPace: Double? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horde_id", nullable = true)
    val parentHorde: Horde? = null
)
