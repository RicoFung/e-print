package com.eprint.server.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PrintWebSocketHandler extends TextWebSocketHandler {

    private final PrintClientSessionRegistry sessionRegistry;

    public PrintWebSocketHandler(PrintClientSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String clientId = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("clientId");

        if (clientId == null || clientId.trim().isEmpty()) {
            session.close(CloseStatus.BAD_DATA.withReason("clientId is required"));
            return;
        }

        sessionRegistry.register(clientId, session);
        session.sendMessage(new TextMessage("{\"type\":\"CONNECTED\",\"clientId\":\"" + clientId + "\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
    }
}
