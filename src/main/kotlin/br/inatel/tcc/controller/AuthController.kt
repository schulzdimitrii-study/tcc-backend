package br.inatel.tcc.controller

import br.inatel.tcc.dto.AuthResponse
import br.inatel.tcc.dto.LoginRequest
import br.inatel.tcc.dto.RegisterRequest
import br.inatel.tcc.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "Authentication", description = "Authentication API")
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @Operation(
        summary = "Register a new user",
        responses = [
            ApiResponse(
                description = "User registered",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AuthResponse::class)
                    )
                ]
            )
        ]
    )
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @Operation(
        summary = "Login to the system",
        responses = [
            ApiResponse(
                description = "User logged in",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AuthResponse::class)
                    )
                ]
            )
        ]
    )
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(request))
}
