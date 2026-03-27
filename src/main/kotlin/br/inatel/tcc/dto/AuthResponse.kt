package br.inatel.tcc.dto

data class AuthResponse(
    val token: String,
    val name: String,
    val email: String
)
