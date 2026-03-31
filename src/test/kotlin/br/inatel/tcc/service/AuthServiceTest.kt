package br.inatel.tcc.service

import br.inatel.tcc.domain.user.UserRepository
import br.inatel.tcc.dto.LoginRequest
import br.inatel.tcc.dto.RegisterRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = ["/test-cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthServiceTest {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun shouldRegisterSuccessfully() {
        val request = RegisterRequest("test@example.com", "Test User", "password123")

        val response = authService.register(request)

        assertNotNull(response)
        assertNotNull(response.token)
        assertTrue(response.token.isNotEmpty())
        assertEquals("Test User", response.name)
        assertEquals("test@example.com", response.email)
        assertTrue(userRepository.existsByEmail("test@example.com"))
    }

    @Test
    fun shouldRejectDuplicateEmail() {
        val request = RegisterRequest("duplicate@example.com", "User", "password123")
        authService.register(request)

        assertThrows(IllegalArgumentException::class.java) { authService.register(request) }
    }

    @Test
    fun shouldLoginSuccessfully() {
        authService.register(RegisterRequest("login@example.com", "Login User", "password123"))

        val loginRequest = LoginRequest("login@example.com", "password123")
        val response = authService.login(loginRequest)

        assertNotNull(response)
        assertNotNull(response.token)
        assertEquals("Login User", response.name)
        assertEquals("login@example.com", response.email)
    }

    @Test
    fun shouldRejectLoginWithWrongPassword() {
        authService.register(RegisterRequest("wrong@example.com", "User", "password123"))

        val loginRequest = LoginRequest("wrong@example.com", "wrongpassword")

        assertThrows(BadCredentialsException::class.java) { authService.login(loginRequest) }
    }

    @Test
    fun shouldRejectLoginWithNonexistentEmail() {
        val loginRequest = LoginRequest("nonexistent@example.com", "password123")

        assertThrows(BadCredentialsException::class.java) { authService.login(loginRequest) }
    }
}
