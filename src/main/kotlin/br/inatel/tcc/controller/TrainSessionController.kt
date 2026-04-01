package br.inatel.tcc.controller

import br.inatel.tcc.dto.StartSessionRequest
import br.inatel.tcc.dto.StartSessionResponse
import br.inatel.tcc.service.TrainSessionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Gerencia o ciclo de vida REST de uma sessão de treino.
 *
 * POST /sessions/iniciar   → cria sessão no PostgreSQL + inicializa Redis
 * POST /sessions/{id}/encerrar → lê leaderboard do Redis + persiste no PostgreSQL
 *
 * Esses endpoints são protegidos por JWT (Bearer token no Authorization header).
 * O WebSocket em /ws é separado e não passa por esses endpoints.
 *
 * TODO [FASE 2 - GET SESSIONS]: Adicionar GET /sessions/{id}/leaderboard para
 *   consulta do ranking atual sem precisar estar conectado via WebSocket
 *   (útil para o app recuperar estado após reconexão).
 *
 * TODO [FASE 3 - HISTÓRICO]: Adicionar GET /sessions?userId={id} para listar
 *   o histórico de sessões do usuário com resultados.
 *   Referência: TrainSessionRepository.findByUserId()
 */
@RestController
@RequestMapping("/sessions")
class TrainSessionController(
    private val trainSessionService: TrainSessionService
) {

    /**
     * Inicia uma nova sessão de treino.
     * Cria o registro no PostgreSQL e inicializa os keys Redis da sessão.
     * Retorna o sessionId que o app React Native deve incluir em cada mensagem WebSocket.
     */
    @PostMapping("/iniciar")
    fun startSession(
        @RequestBody request: StartSessionRequest,
        authentication: Authentication
    ): ResponseEntity<StartSessionResponse> {
        val response = trainSessionService.startSession(authentication.name, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Encerra a sessão: lê ranking final do Redis, persiste no PostgreSQL e
     * reduz TTL dos keys Redis para 1h.
     */
    @PostMapping("/{sessionId}/encerrar")
    fun endSession(
        @PathVariable sessionId: String,
        authentication: Authentication
    ): ResponseEntity<Void> {
        trainSessionService.endSession(sessionId)
        return ResponseEntity.noContent().build()
    }
}
