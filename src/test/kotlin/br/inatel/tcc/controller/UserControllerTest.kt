package br.inatel.tcc.controller

import br.inatel.tcc.domain.user.User
import br.inatel.tcc.dto.UserResponseDto
import br.inatel.tcc.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserControllerTest {

    @Mock
    private lateinit var userService: UserService

    @InjectMocks
    private lateinit var userController: UserController

    private val userId = UUID.randomUUID()

    private fun buildUserResponseDto() = UserResponseDto(
        id = userId,
        name = "Test User",
        email = "test@example.com",
        birthdayDate = LocalDate.of(1990, 1, 1),
        maxHeartRate = 180,
        height = 175.99,
        weight = 70.56
    )

    // ─── GET /users/{id} ──────────────────────────────────────────────────────

    @Test
    fun getUser_shouldReturn200AndUserDto() {
        val dto = buildUserResponseDto()
        whenever(userService.getUser(userId)).thenReturn(dto)

        val response = userController.getUser(userId)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(dto, response.body)
        verify(userService).getUser(userId)
    }

    @Test
    fun getUser_shouldPropagateExceptionWhenNotFound() {
        whenever(userService.getUser(userId)).thenThrow(IllegalArgumentException("User not found"))

        assertThrows(IllegalArgumentException::class.java) {
            userController.getUser(userId)
        }
    }

    // ─── PATCH /users/{id} ────────────────────────────────────────────────────

    @Test
    fun updateUser_shouldReturn200AndUpdatedUserDto() {
        val dto = buildUserResponseDto().copy(name = "Updated User")
        val requestBody = User(name = "Updated User")
        
        whenever(userService.updateUser(userId, requestBody)).thenReturn(dto)

        val response = userController.updateUser(userId, requestBody)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Updated User", response.body?.name)
        verify(userService).updateUser(userId, requestBody)
    }

    @Test
    fun updateUser_shouldPropagateExceptionWhenNotFound() {
        val requestBody = User(name = "Updated User")
        whenever(userService.updateUser(userId, requestBody)).thenThrow(IllegalArgumentException("User not found"))

        assertThrows(IllegalArgumentException::class.java) {
            userController.updateUser(userId, requestBody)
        }
    }
}
