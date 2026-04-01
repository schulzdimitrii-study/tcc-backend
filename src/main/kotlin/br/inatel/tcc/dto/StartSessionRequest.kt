package br.inatel.tcc.dto

import java.util.UUID

/**
 * Payload para iniciar uma nova sessão de treino.
 * Enviado pelo app React Native via POST /sessions/iniciar.
 *
 * hordeId é opcional — quando nulo, a sessão é um treino livre (sem competição).
 * Quando informado, o leaderboard da Horda é ativado e a posição virtual é calculada.
 */
data class StartSessionRequest(
    val hordeId: UUID? = null,
    val trainType: String = "RUN"
)
