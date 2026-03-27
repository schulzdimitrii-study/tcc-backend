package br.inatel.tcc.domain.friendship

import br.inatel.tcc.domain.user.User
import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

@Table(name = "friendships")
@Entity
class Friendship(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    val requester: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    val recipient: User,

    @Column(nullable = false)
    val requestDate: LocalDate = LocalDate.now(),

    val responseDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: FriendshipStatus = FriendshipStatus.PENDING
)
