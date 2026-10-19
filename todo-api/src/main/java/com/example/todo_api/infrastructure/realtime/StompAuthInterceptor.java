package com.example.todo_api.infrastructure.realtime;

import java.security.Principal;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.example.todo_api.core.security.AccessTokenAuthenticator;
import com.example.todo_api.core.security.AuthPrincipal;

/**
 * Bảo vệ kết nối realtime:
 * <ul>
 * <li>CONNECT phải kèm header "Authorization: Bearer &lt;access token&gt;", kiểm tra như request HTTP.</li>
 * <li>Chỉ được subscribe kênh /user/..., tức kênh của chính mình (FR-06.1: mỗi user chỉ nhận sự kiện của mình).</li>
 * <li>Client không gửi message lên server: realtime chỉ một chiều, server đẩy xuống.</li>
 * </ul>
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER = "Bearer ";

    private final AccessTokenAuthenticator authenticator;

    public StompAuthInterceptor(AccessTokenAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT -> accessor.setUser(authenticate(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION)));
            case SUBSCRIBE -> {
                String destination = accessor.getDestination();
                if (destination == null || !destination.startsWith("/user/")) {
                    throw new MessageDeliveryException("Only /user/ destinations can be subscribed");
                }
            }
            case SEND -> throw new MessageDeliveryException("Sending messages is not supported");
            default -> {
            }
        }
        return message;
    }

    private Principal authenticate(String header) {
        if (header == null || !header.startsWith(BEARER)) {
            throw new MessageDeliveryException("Authentication required");
        }
        AuthPrincipal principal = authenticator.authenticate(header.substring(BEARER.length()))
                .map(auth -> (AuthPrincipal) auth.getPrincipal())
                .orElseThrow(() -> new MessageDeliveryException("Invalid or expired access token"));
        return new UserIdPrincipal(String.valueOf(principal.userId()));
    }

    /** Tên của principal là userId: RealtimePublisher gửi tới user theo đúng tên này. */
    record UserIdPrincipal(String name) implements Principal {

        @Override
        public String getName() {
            return name;
        }
    }
}
