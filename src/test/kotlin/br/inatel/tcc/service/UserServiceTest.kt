package br.inatel.tcc.service

import br.inatel.tcc.domain.user.User
import br.inatel.tcc.domain.user.UserRepository
import br.inatel.tcc.dto.UserResponseDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var userService: UserService

    private val userId = UUID.randomUUID()

    private fun buildUser() = User(
        id = userId,
        name = "Test User",
        email = "test@example.com",
        password = "oldPassword123",
        birthdayDate = LocalDate.of(1990, 1, 1),
        maxHeartRate = 180,
        height = 175.0,
        weight = 70.0
    )

    private fun buildUserResponseDto() = UserResponseDto(
        id = userId,
        name = "Test User",
        email = "test@example.com",
        birthdayDate = LocalDate.of(1990, 1, 1),
        maxHeartRate = 180,
        height = 175.0,
        weight = 70.0
    )

    // ─── GET USER ─────────────────────────────────────────────────────────────

    @Test
    fun getUser_shouldReturnDto_whenFound() {
        val dto = buildUserResponseDto()
        whenever(userRepository.findUserResponseById(userId)).thenReturn(Optional.of(dto))

        val result = userService.getUser(userId)

        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("Test User", result.name)
        verify(userRepository).findUserResponseById(userId)
    }

    @Test
    fun getUser_shouldThrowException_whenNotFound() {
        whenever(userRepository.findUserResponseById(userId)).thenReturn(Optional.empty())

        val exception = assertThrows(IllegalArgumentException::class.java) {
            userService.getUser(userId)
        }

        assertEquals("User not found", exception.message)
        verify(userRepository).findUserResponseById(userId)
    }

    // ─── UPDATE USER ──────────────────────────────────────────────────────────

    @Test
    fun updateUser_shouldUpdateProvidedFields_andPreserveOthers() {
        val existingUser = buildUser()
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(existingUser))

        val updatedUserMock = User(
            id = userId,
            name = "Updated Name",
            email = "test@example.com",
            password = "oldPassword123",
            birthdayDate = LocalDate.of(1990, 1, 1),
            maxHeartRate = 180,
            height = 175.0,
            weight = 70.0
        )
        whenever(userRepository.save(any())).thenReturn(updatedUserMock)

        val updateRequest = User(
            name = "Updated Name",
            email = "",
            password = "",
            birthdayDate = null
        )

        val result = userService.updateUser(userId, updateRequest)

        assertNotNull(result)
        assertEquals("Updated Name", result.name)

        verify(userRepository).findById(userId)
        verify(userRepository).save(org.mockito.kotlin.argThat {
            this.name == "Updated Name" &&
            this.email == "test@example.com" &&
            this.password == "oldPassword123" &&
            this.birthdayDate == LocalDate.of(1990, 1, 1)
        })
    }

    @Test
    fun updateUser_shouldUpdatePassword_whenProvided() {
        val existingUser = buildUser()
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(existingUser))

        val updatedUserMock = User(
            id = userId,
            name = "Test User",
            email = "test@example.com",
            password = "newPassword123",
            birthdayDate = LocalDate.of(1990, 1, 1),
            maxHeartRate = 180,
            height = 175.0,
            weight = 70.0
        )
        whenever(userRepository.save(any())).thenReturn(updatedUserMock)

        val updateRequest = User(
            name = "",
            email = "",
            password = "newPassword123"
        )

        userService.updateUser(userId, updateRequest)

        verify(userRepository).save(org.mockito.kotlin.argThat {
            this.password == "newPassword123" &&
            this.name == "Test User"
        })
    }

    @Test
    fun updateUser_shouldThrowException_whenNotFound() {
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())
        
        val updateRequest = User(name = "Test")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            userService.updateUser(userId, updateRequest)
        }

        assertEquals("User not found", exception.message)
    }
}
