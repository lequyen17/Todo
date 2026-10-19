package com.example.todo_api.infrastructure.realtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Kết nối realtime (FR-06): STOMP over WebSocket tại /api/ws, broker trong bộ nhớ.
 * Mỗi user nhận sự kiện của mình ở /user/queue/events (FR-06.1 AC1).
 * Broker trong bộ nhớ chỉ đúng khi chạy một instance API; nhiều instance thì cần relay qua Redis / RabbitMQ.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** Nhịp tim 2 chiều để kết nối không bị proxy (nginx) cắt khi rảnh, và phát hiện sớm kết nối đã chết. */
    private static final long[] HEARTBEAT_MS = { 20_000, 20_000 };

    private final StompAuthInterceptor authInterceptor;

    public WebSocketConfig(StompAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Xác thực bằng access token trong frame CONNECT, không dùng cookie, nên kết nối từ origin khác
        // cũng không lấy được gì nếu không có token (không có rủi ro cross-site WebSocket hijacking)
        registry.addEndpoint("/api/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue")
                .setHeartbeatValue(HEARTBEAT_MS)
                .setTaskScheduler(wsHeartbeatScheduler());
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @Bean
    ThreadPoolTaskScheduler wsHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        return scheduler;
    }
}
