package br.inatel.tcc.domain.achievement

import jakarta.persistence.*
import java.util.UUID

@Table(name = "achievements")
@Entity
class Achievement(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val title: String = "",

    val description: String? = null,

    val urlIcon: String? = null,

    val criterion: String? = null,

    @Column(nullable = false)
    val active: Boolean = true
)
