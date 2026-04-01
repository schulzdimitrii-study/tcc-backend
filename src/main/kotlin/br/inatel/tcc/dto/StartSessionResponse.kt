package br.inatel.tcc.dto

/**
 * Retornado pelo POST /sessions/iniciar.
 * O app React Native armazena o sessionId e o inclui em cada mensagem WebSocket de biometria.
 */
data class StartSessionResponse(
    val sessionId: String
)
