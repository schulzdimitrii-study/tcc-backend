package br.inatel.tcc.domain.friendship

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FriendshipRepository : JpaRepository<Friendship, UUID> {
    fun findByRequesterIdOrRecipientId(requesterId: UUID, recipientId: UUID): List<Friendship>
    fun findByRequesterIdAndRecipientId(requesterId: UUID, recipientId: UUID): Friendship?
}
