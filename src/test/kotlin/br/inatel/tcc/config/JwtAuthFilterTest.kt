package br.inatel.tcc.config

import br.inatel.tcc.domain.user.User
import br.inatel.tcc.domain.user.UserRepository
import br.inatel.tcc.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class JwtAuthFilterTest {

    @Mock
    private lateinit var jwtService: JwtService

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var jwtAuthFilter: JwtAuthFilter

    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        request = mock(HttpServletRequest::class.java)
        response = mock(HttpServletResponse::class.java)
        filterChain = mock(FilterChain::class.java)
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun doFilterInternal_shouldSkip_whenNoAuthHeader() {
        whenever(request.getHeader("Authorization")).thenReturn(null)

        jwtAuthFilter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        verify(jwtService, never()).extractEmail(any())
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun doFilterInternal_shouldSkip_whenHeaderDoesNotStartWithBearer() {
        whenever(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz")

        jwtAuthFilter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        verify(jwtService, never()).extractEmail(any())
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun doFilterInternal_shouldAuthenticateUser_whenTokenIsValid() {
        val token = "valid.jwt.token"
        val email = "test@example.com"
        val user = User(name = "Test", email = email, password = "pwd")

        whenever(request.getHeader("Authorization")).thenReturn("Bearer $token")
        whenever(jwtService.extractEmail(token)).thenReturn(email)
        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(jwtService.isTokenValid(token, user)).thenReturn(true)

        jwtAuthFilter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        val authentication = SecurityContextHolder.getContext().authentication
        assertThat(authentication).isNotNull
        assertThat(authentication.principal).isEqualTo(user)
    }

    @Test
    fun doFilterInternal_shouldNotAuthenticate_whenTokenIsInvalid() {
        val token = "invalid.jwt.token"
        val email = "test@example.com"
        val user = User(name = "Test", email = email, password = "pwd")

        whenever(request.getHeader("Authorization")).thenReturn("Bearer $token")
        whenever(jwtService.extractEmail(token)).thenReturn(email)
        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(jwtService.isTokenValid(token, user)).thenReturn(false)

        jwtAuthFilter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun doFilterInternal_shouldNotAuthenticate_whenExceptionOccurs() {
        val token = "malformed.token"

        whenever(request.getHeader("Authorization")).thenReturn("Bearer $token")
        whenever(jwtService.extractEmail(token)).thenThrow(RuntimeException("JWT Error"))

        jwtAuthFilter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }
}
