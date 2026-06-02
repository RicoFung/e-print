package com.eprint.server.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PrintWebSocketHandler printWebSocketHandler;

    public WebSocketConfig(PrintWebSocketHandler printWebSocketHandler) {
        this.printWebSocketHandler = printWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(printWebSocketHandler, "/ws/print")
                .setAllowedOrigins("*");
    }
}
