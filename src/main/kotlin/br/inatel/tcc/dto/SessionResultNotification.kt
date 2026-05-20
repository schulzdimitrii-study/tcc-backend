package br.inatel.tcc.dto

/**
 * Notificação enviada via WebSocket para amigos de um usuário quando sua sessão encerra.
 *
 * Canal: /topic/user/{friendId}/notifications
 *
 * O app React Native subscreve ao canal do próprio usuário para receber
 * atualizações em tempo real sobre atividades dos amigos.
 */
data class SessionResultNotification(
    val type: String = "SESSION_ENDED",
    val userId: String,
    val userName: String,
    val sessionId: String,
    val totalDistanceKm: Double?,
    val rank: Int?
)
