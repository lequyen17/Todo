package com.example.todo_api.infrastructure.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.example.todo_api.core.security.AccessTokenAuthenticator;
import com.example.todo_api.core.security.AuthPrincipal;

class StompAuthInterceptorTest {

    private AccessTokenAuthenticator authenticator;
    private StompAuthInterceptor interceptor;
    private final MessageChannel channel = mock(MessageChannel.class);

    @BeforeEach
    void setUp() {
        authenticator = mock(AccessTokenAuthenticator.class);
        interceptor = new StompAuthInterceptor(authenticator);
        AuthPrincipal principal = new AuthPrincipal(7L, "a@example.com", "USER", "jti", Instant.MAX);
        when(authenticator.authenticate("good")).thenReturn(
                Optional.of(new UsernamePasswordAuthenticationToken(principal, null, List.of())));
        when(authenticator.authenticate("bad")).thenReturn(Optional.empty());
    }

    @Test
    void connectWithValidToken_setsUserIdAsPrincipalName() {
        StompHeaderAccessor accessor = frame(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer good");

        interceptor.preSend(message(accessor), channel);

        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("7");
    }

    @Test
    void connectWithoutOrWithInvalidToken_isRejected() {
        assertThatThrownBy(() -> interceptor.preSend(message(frame(StompCommand.CONNECT)), channel))
                .isInstanceOf(MessageDeliveryException.class);

        StompHeaderAccessor invalid = frame(StompCommand.CONNECT);
        invalid.setNativeHeader("Authorization", "Bearer bad");
        assertThatThrownBy(() -> interceptor.preSend(message(invalid), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    /** FR-06.1 AC1: không subscribe thẳng kênh nội bộ của broker để nghe trộm sự kiện của người khác. */
    @Test
    void subscribe_onlyOwnUserDestinations() {
        StompHeaderAccessor own = frame(StompCommand.SUBSCRIBE);
        own.setDestination("/user/queue/events");
        interceptor.preSend(message(own), channel);

        StompHeaderAccessor raw = frame(StompCommand.SUBSCRIBE);
        raw.setDestination("/queue/events-user123");
        assertThatThrownBy(() -> interceptor.preSend(message(raw), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void clientSend_isRejected() {
        StompHeaderAccessor send = frame(StompCommand.SEND);
        send.setDestination("/app/anything");

        assertThatThrownBy(() -> interceptor.preSend(message(send), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    private static StompHeaderAccessor frame(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private static Message<byte[]> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
