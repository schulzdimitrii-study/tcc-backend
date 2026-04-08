package br.inatel.tcc.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * Configuração do broker STOMP sobre WebSocket.
 *
 * Fluxo de mensagens:
 *   Galaxy Watch → App React Native → STOMP /app/treino/dados → BiometricWebSocketController
 *   Backend → STOMP /topic/session/{id}/leaderboard → App React Native (broadcast)
 *
 * TODO [FASE 6 - AUTENTICAÇÃO WS]: Adicionar HandshakeInterceptor para validar
 * o JWT token durante o handshake WebSocket, passando o usuário autenticado
 * ao contexto da mensagem via StompHeaderAccessor.
 */
 
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        // Canal de broadcast para clientes inscritos (ex: /topic/session/{id}/leaderboard)
        config.enableSimpleBroker("/topic")

        // Prefixo para mensagens que chegam ao backend e são roteadas para @MessageMapping
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // Ponto de conexão WebSocket — o app React Native conecta aqui via ws://host/ws
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
    }
}
