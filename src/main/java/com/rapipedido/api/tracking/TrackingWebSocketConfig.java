package com.rapipedido.api.tracking;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class TrackingWebSocketConfig implements WebSocketConfigurer {

    private final TrackingWebSocketHandler handler;

    public TrackingWebSocketConfig(TrackingWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/tracking")
                .setAllowedOriginPatterns("*");
    }
}