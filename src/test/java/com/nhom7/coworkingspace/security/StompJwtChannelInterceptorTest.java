package com.nhom7.coworkingspace.security;

import com.nhom7.coworkingspace.service.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("StompJwtChannelInterceptor - Unit Tests")
class StompJwtChannelInterceptorTest {

    private static final String TOKEN = "valid.jwt.token";
    private static final String EMAIL = "user@coworking.test";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private StompJwtChannelInterceptor interceptor;

    @Test
    @DisplayName("CONNECT with a valid access token sets the STOMP Principal")
    void validConnectSetsPrincipal() {
        Date issuedAt = new Date(1000L);
        UserDetails user = User.withUsername(EMAIL).password("password").roles("USER").build();
        given(jwtTokenProvider.validateToken(TOKEN)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(TOKEN)).willReturn(true);
        given(jwtTokenProvider.extractUsername(TOKEN)).willReturn(EMAIL);
        given(jwtTokenProvider.extractIssuedAt(TOKEN)).willReturn(issuedAt);
        given(tokenBlacklistService.isBlacklisted(TOKEN)).willReturn(false);
        given(tokenBlacklistService.isUserTokenRevoked(EMAIL, issuedAt)).willReturn(false);
        given(userDetailsService.loadUserByUsername(EMAIL)).willReturn(user);

        Message<byte[]> message = connectMessage("Bearer " + TOKEN);
        interceptor.preSend(message, org.mockito.Mockito.mock(org.springframework.messaging.MessageChannel.class));

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("CONNECT without Authorization header is rejected")
    void missingTokenIsRejected() {
        Message<byte[]> message = connectMessage(null);

        assertThatThrownBy(() -> interceptor.preSend(
                message,
                org.mockito.Mockito.mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("auth.token.missing");
    }

    @Test
    @DisplayName("CONNECT with a refresh token is rejected")
    void refreshTokenIsRejected() {
        given(jwtTokenProvider.validateToken(TOKEN)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(TOKEN)).willReturn(false);
        Message<byte[]> message = connectMessage("Bearer " + TOKEN);

        assertThatThrownBy(() -> interceptor.preSend(
                message,
                org.mockito.Mockito.mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("auth.token.invalid");
    }

    private Message<byte[]> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
