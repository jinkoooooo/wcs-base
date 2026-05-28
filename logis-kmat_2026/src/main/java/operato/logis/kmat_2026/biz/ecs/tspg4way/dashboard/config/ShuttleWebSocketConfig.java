package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 4-Way Shuttle WebSocket STOMP 설정
 * 실시간 설비 상태 및 작업 데이터 전송을 위한 WebSocket 구성
 */
@Configuration
@EnableWebSocketMessageBroker
public class ShuttleWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 prefix (서버 -> 클라이언트)
        config.enableSimpleBroker("/topic", "/queue");
        // 클라이언트가 메시지를 보낼 prefix (클라이언트 -> 서버)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트
        registry.addEndpoint("/ws/shuttle")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // SockJS 없는 순수 WebSocket 연결 (선택적)
        registry.addEndpoint("/ws/shuttle-raw")
                .setAllowedOriginPatterns("*");
    }
}
