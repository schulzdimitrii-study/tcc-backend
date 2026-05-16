package br.inatel.tcc.domain.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import java.time.LocalDate
import java.util.UUID

@DataJpaTest
class UserRepositoryTest(
    @Autowired val userRepository: UserRepository,
    @Autowired val entityManager: TestEntityManager
) {

    private fun createUser(email: String) = User(
        name = "Test User",
        email = email,
        password = "password123",
        birthdayDate = LocalDate.of(1995, 5, 10),
        maxHeartRate = 185,
        height = 180.0,
        weight = 75.0
    )

    @Test
    fun findByEmail_shouldReturnUser_whenEmailExists() {
        val user = createUser("find@example.com")
        entityManager.persist(user)
        entityManager.flush()

        val result = userRepository.findByEmail("find@example.com")

        assertThat(result).isPresent
        assertThat(result.get().name).isEqualTo("Test User")
    }

    @Test
    fun findByEmail_shouldReturnEmpty_whenEmailDoesNotExist() {
        val result = userRepository.findByEmail("nonexistent@example.com")
        assertThat(result).isEmpty
    }

    @Test
    fun existsByEmail_shouldReturnTrue_whenEmailExists() {
        val user = createUser("exists@example.com")
        entityManager.persist(user)
        entityManager.flush()

        val exists = userRepository.existsByEmail("exists@example.com")

        assertThat(exists).isTrue()
    }

    @Test
    fun existsByEmail_shouldReturnFalse_whenEmailDoesNotExist() {
        val exists = userRepository.existsByEmail("notexists@example.com")
        assertThat(exists).isFalse()
    }

    @Test
    fun findUserResponseById_shouldReturnUserResponseDto_whenUserExists() {
        val user = createUser("dto@example.com")
        val savedUser = entityManager.persist(user)
        entityManager.flush()

        val result = userRepository.findUserResponseById(savedUser.id!!)

        assertThat(result).isPresent
        assertThat(result.get().id).isEqualTo(savedUser.id)
        assertThat(result.get().email).isEqualTo("dto@example.com")
        assertThat(result.get().name).isEqualTo("Test User")
        assertThat(result.get().birthdayDate).isEqualTo(LocalDate.of(1995, 5, 10))
        assertThat(result.get().maxHeartRate).isEqualTo(185)
        assertThat(result.get().height).isEqualTo(180.0)
        assertThat(result.get().weight).isEqualTo(75.0)
    }

    @Test
    fun findUserResponseById_shouldReturnEmpty_whenUserDoesNotExist() {
        val randomId = UUID.randomUUID()
        val result = userRepository.findUserResponseById(randomId)
        assertThat(result).isEmpty
    }
}
