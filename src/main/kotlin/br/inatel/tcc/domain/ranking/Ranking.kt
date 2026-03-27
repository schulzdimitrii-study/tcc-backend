package br.inatel.tcc.domain.ranking

import br.inatel.tcc.domain.user.User
import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

@Table(name = "rankings")
@Entity
class Ranking(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    val position: Int = 0,

    @Column(nullable = false)
    val score: Double = 0.0,

    @Column(nullable = false)
    val period: String = "",

    @Column(nullable = false)
    val calculeDate: LocalDate = LocalDate.now()
)
