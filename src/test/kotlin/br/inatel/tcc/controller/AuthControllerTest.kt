package br.inatel.tcc.controller

import br.inatel.tcc.domain.user.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = ["/test-cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun shouldRegisterNewUser() {
        val request = mapOf(
            "email" to "test@example.com",
            "name" to "Test User",
            "password" to "password123"
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.name").value("Test User"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
    }

    @Test
    fun shouldRejectDuplicateRegistration() {
        val request = mapOf(
            "email" to "test@example.com",
            "name" to "Test User",
            "password" to "password123"
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("Email already registered"))
    }

    @Test
    fun shouldLoginSuccessfully() {
        val registerRequest = mapOf(
            "email" to "login@example.com",
            "name" to "Login User",
            "password" to "password123"
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        ).andExpect(status().isCreated)

        val loginRequest = mapOf(
            "email" to "login@example.com",
            "password" to "password123"
        )

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.name").value("Login User"))
            .andExpect(jsonPath("$.email").value("login@example.com"))
    }

    @Test
    fun shouldRejectLoginWithWrongPassword() {
        val registerRequest = mapOf(
            "email" to "wrong@example.com",
            "name" to "Wrong Password User",
            "password" to "password123"
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        ).andExpect(status().isCreated)

        val loginRequest = mapOf(
            "email" to "wrong@example.com",
            "password" to "wrongpassword"
        )

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun shouldRejectInvalidEmail() {
        val request = mapOf(
            "email" to "not-an-email",
            "name" to "Test",
            "password" to "password123"
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldRejectShortPassword() {
        val request = mapOf(
            "email" to "test@example.com",
            "name" to "Test",
            "password" to "123"
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldRejectRegisterWithEmptyBody() {
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldRejectRegisterWithMissingEmail() {
        val request = mapOf(
            "name" to "Test User",
            "password" to "password123"
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldRejectRegisterWithMissingPassword() {
        val request = mapOf(
            "email" to "test@example.com",
            "name" to "Test User"
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldRejectRegisterWithMissingName() {
        val request = mapOf(
            "email" to "test@example.com",
            "password" to "password123"
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldRejectRegisterWithBlankEmail() {
        val request = mapOf(
            "email" to "",
            "name" to "Test User",
            "password" to "password123"
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldRejectRegisterWithBlankPassword() {
        val request = mapOf(
            "email" to "test@example.com",
            "name" to "Test User",
            "password" to ""
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldRejectLoginWithEmptyBody() {
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldRejectLoginWithNonexistentEmail() {
        val loginRequest = mapOf(
            "email" to "nonexistent@example.com",
            "password" to "password123"
        )

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun shouldRejectLoginWithBlankEmail() {
        val loginRequest = mapOf(
            "email" to "",
            "password" to "password123"
        )

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldRejectLoginWithBlankPassword() {
        val loginRequest = mapOf(
            "email" to "test@example.com",
            "password" to ""
        )

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun shouldRejectRegisterWithExplicitNullEmail() {
        val requestJson = """
            {
                "email": null,
                "name": "Test User",
                "password": "password123"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        ).andExpect(status().isBadRequest)
    }
}

