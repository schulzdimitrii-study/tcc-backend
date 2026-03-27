package br.inatel.tcc.service

import br.inatel.tcc.domain.user.User
import br.inatel.tcc.domain.user.UserRepository
import br.inatel.tcc.dto.AuthResponse
import br.inatel.tcc.dto.LoginRequest
import br.inatel.tcc.dto.RegisterRequest
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {

    fun register(request: RegisterRequest): AuthResponse {
        val email = request.email!!
        
        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("Email already registered")
        }

        val user = User(
            email = email,
            name = request.name!!,
            password = passwordEncoder.encode(request.password!!),
            birthdayDate = request.birthdayDate,
            height = request.height,
            weight = request.weight
        )

        userRepository.save(user)
        val token = jwtService.generateToken(user)

        return AuthResponse(token = token, name = user.name, email = user.email)
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email!!)
            .orElseThrow { BadCredentialsException("Invalid credentials") }

        if (!passwordEncoder.matches(request.password!!, user.password)) {
            throw BadCredentialsException("Invalid credentials")
        }

        val token = jwtService.generateToken(user)
        return AuthResponse(token = token, name = user.name, email = user.email)
    }
}
