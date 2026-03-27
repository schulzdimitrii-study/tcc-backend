package br.inatel.tcc.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class RegisterRequest(
    @field:NotBlank(message = "Email is required") 
    @field:Email(message = "Invalid email format") 
    val email: String?,

    @field:NotBlank(message = "Name is required") 
    val name: String?,

    @field:NotBlank(message = "Password is required") 
    @field:Size(min = 6, message = "Password must have at least 6 characters") 
    val password: String?,

    val birthdayDate: LocalDate? = null,
    val height: Double? = null,
    val weight: Double? = null
)
