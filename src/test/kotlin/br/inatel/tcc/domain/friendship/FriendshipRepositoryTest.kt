package br.inatel.tcc.domain.friendship

import br.inatel.tcc.domain.user.User
import br.inatel.tcc.domain.user.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = ["/test-cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FriendshipRepositoryTest {

    @Autowired
    private lateinit var friendshipRepository: FriendshipRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private fun savedUser(email: String) = userRepository.save(
        User(email = email, name = "Player", password = "encoded")
    )

    @Test
    fun shouldFindFriendshipByRequesterOrRecipient() {
        val user1 = savedUser("user1@test.com")
        val user2 = savedUser("user2@test.com")
        val user3 = savedUser("user3@test.com")

        friendshipRepository.save(Friendship(requester = user1, recipient = user2, requestDate = LocalDate.now()))
        friendshipRepository.save(Friendship(requester = user3, recipient = user1, requestDate = LocalDate.now()))

        val user1Friendships = friendshipRepository.findByRequesterIdOrRecipientId(user1.id!!, user1.id!!)

        assertEquals(2, user1Friendships.size)
    }

    @Test
    fun shouldFindFriendshipByBothIds() {
        val user1 = savedUser("a@test.com")
        val user2 = savedUser("b@test.com")

        friendshipRepository.save(Friendship(requester = user1, recipient = user2, requestDate = LocalDate.now()))

        val found = friendshipRepository.findByRequesterIdAndRecipientId(user1.id!!, user2.id!!)
        val notFound = friendshipRepository.findByRequesterIdAndRecipientId(user2.id!!, user1.id!!)

        assertNotNull(found)
        assertEquals(user1.id, found?.requester?.id)
        assertNull(notFound)
    }

    @Test
    fun shouldReturnNullForNonExistentFriendship() {
        val user1 = savedUser("x@test.com")
        val user2 = savedUser("y@test.com")

        val result = friendshipRepository.findByRequesterIdAndRecipientId(user1.id!!, user2.id!!)

        assertNull(result)
    }

    @Test
    fun shouldPersistFriendshipStatus() {
        val user1 = savedUser("s1@test.com")
        val user2 = savedUser("s2@test.com")

        val friendship = friendshipRepository.save(
            Friendship(requester = user1, recipient = user2, requestDate = LocalDate.now(), status = FriendshipStatus.ACCEPTED)
        )

        val found = friendshipRepository.findById(friendship.id!!)

        assertTrue(found.isPresent)
        assertEquals(FriendshipStatus.ACCEPTED, found.get().status)
    }
}
