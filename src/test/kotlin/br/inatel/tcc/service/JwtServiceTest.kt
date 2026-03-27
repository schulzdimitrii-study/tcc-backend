package br.inatel.tcc.service

import br.inatel.tcc.domain.user.User
import io.jsonwebtoken.ExpiredJwtException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JwtServiceTest {

    companion object {
        private const val SECRET = "TestSecretKeyForJwtTokenGenerationMustBeAtLeast256BitsLongEnough"
        private const val EXPIRATION = 3600000L
    }

    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setUp() {
        jwtService = JwtService(SECRET, EXPIRATION)
    }

    private fun createUser(): User = User(
        email = "test@example.com",
        name = "Test User",
        password = "encoded_password"
    )

    @Test
    fun shouldGenerateToken() {
        val user = createUser()
        val token = jwtService.generateToken(user)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun shouldExtractEmailFromToken() {
        val user = createUser()
        val token = jwtService.generateToken(user)

        val email = jwtService.extractEmail(token)

        assertEquals("test@example.com", email)
    }

    @Test
    fun shouldValidateToken() {
        val user = createUser()
        val token = jwtService.generateToken(user)

        assertTrue(jwtService.isTokenValid(token, user))
    }

    @Test
    fun shouldRejectTokenForDifferentUser() {
        val user = createUser()
        val otherUser = User(
            email = "other@example.com",
            name = "Other User",
            password = "encoded_password"
        )

        val token = jwtService.generateToken(user)

        assertFalse(jwtService.isTokenValid(token, otherUser))
    }

    @Test
    fun shouldRejectExpiredToken() {
        val shortLivedService = JwtService(SECRET, 0)
        val user = createUser()

        val token = shortLivedService.generateToken(user)

        assertThrows(ExpiredJwtException::class.java) { jwtService.extractEmail(token) }
    }
}
