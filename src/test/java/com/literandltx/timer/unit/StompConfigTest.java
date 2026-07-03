package com.literandltx.timer.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.literandltx.timer.config.StompConfig;
import com.literandltx.timer.config.env.WebPropertiesConfig;
import com.literandltx.timer.security.JwtUtil;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class StompConfigTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserDetailsService userDetailsService;
    @Mock private WebPropertiesConfig webPropertiesConfig;
    @Mock private MessageChannel messageChannel;

    @InjectMocks
    private StompConfig stompConfig;

    private ChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        // 1. Arrange
        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> captor = ArgumentCaptor.forClass(ChannelInterceptor.class);

        // 2. Act
        stompConfig.configureClientInboundChannel(registration);

        // 3. Assert / Capture
        verify(registration).interceptors(captor.capture());
        interceptor = captor.getValue();
    }

    @Test
    void preSend_ValidJwt_AuthenticatesUser() {
        // 1. Arrange
        String token = "valid.jwt.token";
        String username = "testuser";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        UserDetails userDetails = new User(username, "password", Collections.emptyList());

        when(jwtUtil.isValidToken(token)).thenReturn(true);
        when(jwtUtil.getUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        // 2. Act
        Message<?> resultMessage = interceptor.preSend(message, messageChannel);

        // 3. Assert
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.getAccessor(resultMessage, StompHeaderAccessor.class);
        assertNotNull(resultAccessor.getUser());
        assertTrue(resultAccessor.getUser() instanceof UsernamePasswordAuthenticationToken);
        assertEquals(username, resultAccessor.getUser().getName());
    }

    @Test
    void preSend_MissingJwt_ThrowsException() {
        // 1. Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // 2. Act
        Executable action = () -> interceptor.preSend(message, messageChannel);

        // 3. Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertEquals("Missing Authentication: No Session or JWT Token provided", exception.getMessage());
    }

    @Test
    void preSend_InvalidJwt_ThrowsException() {
        // 1. Arrange
        String token = "invalid.token";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtil.isValidToken(token)).thenReturn(false);

        // 2. Act
        Executable action = () -> interceptor.preSend(message, messageChannel);

        // 3. Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertEquals("Invalid JWT Token", exception.getMessage());
    }

    @Test
    void preSend_AlreadyAuthenticated_SkipsValidation() {
        // 1. Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        UsernamePasswordAuthenticationToken existingAuth =
                new UsernamePasswordAuthenticationToken("user", "pass", Collections.emptyList());
        accessor.setUser(existingAuth);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // 2. Act
        Message<?> resultMessage = interceptor.preSend(message, messageChannel);

        // 3. Assert
        verifyNoInteractions(jwtUtil, userDetailsService);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.getAccessor(resultMessage, StompHeaderAccessor.class);
        assertEquals(existingAuth, resultAccessor.getUser());
    }

    @Test
    void preSend_NotConnectCommand_SkipsValidation() {
        // 1. Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // 2. Act
        Message<?> resultMessage = interceptor.preSend(message, messageChannel);

        // 3. Assert
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    @Test
    void preSend_MalformedAuthHeader_ThrowsException() {
        // 1. Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "just.the.token.no.bearer"); // Missing "Bearer "
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // 2. Act
        Executable action = () -> interceptor.preSend(message, messageChannel);

        // 3. Assert
        assertThrows(Exception.class, action);
    }

    @Test
    void preSend_ValidJwtButUserNotFound_ThrowsException() {
        // 1. Arrange
        String token = "valid.jwt.token";
        String username = "deleteduser";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtil.isValidToken(token)).thenReturn(true);
        when(jwtUtil.getUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username))
                .thenThrow(new UsernameNotFoundException("User not found"));

        // 2. Act
        Executable action = () -> interceptor.preSend(message, messageChannel);

        // 3. Assert
        assertThrows(UsernameNotFoundException.class, action);
    }

    @Test
    void preSend_EmptyAuthHeader_ThrowsException() {
        // 1. Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "");
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // 2. Act
        Executable action = () -> interceptor.preSend(message, messageChannel);

        // 3. Assert
        assertThrows(IllegalArgumentException.class, action);
    }
}
