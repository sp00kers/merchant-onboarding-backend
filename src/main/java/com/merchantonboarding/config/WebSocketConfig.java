package com.merchantonboarding.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.merchantonboarding.service.JwtService;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker
        // Messages with /topic prefix are sent to all subscribers
        // Messages with /queue prefix are sent to specific users
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Also register without SockJS for native WebSocket clients
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Extract JWT from Authorization header
                    List<String> authHeaders = accessor.getNativeHeader("Authorization");
                    if (authHeaders != null && !authHeaders.isEmpty()) {
                        String authHeader = authHeaders.get(0);
                        if (authHeader.startsWith("Bearer ")) {
                            String token = authHeader.substring(7);
                            try {
                                String username = jwtService.extractUsername(token);
                                if (username != null && jwtService.isTokenValid(token, username)) {
                                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                                    UsernamePasswordAuthenticationToken auth =
                                            new UsernamePasswordAuthenticationToken(
                                                    userDetails, null, userDetails.getAuthorities());
                                    SecurityContextHolder.getContext().setAuthentication(auth);
                                    accessor.setUser(auth);
                                }
                            } catch (Exception e) {
                                System.err.println("WebSocket authentication failed: " + e.getMessage());
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}
