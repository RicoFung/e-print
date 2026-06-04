package com.eprint.server.websocket;

import com.eprint.server.module.task.model.Task;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PrintClientSessionRegistry {

    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public PrintClientSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(String clientId, WebSocketSession session) {
        sessions.put(clientId, session);
    }

    public void unregister(WebSocketSession session) {
        sessions.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
    }

    public boolean sendPrintTask(String clientId, Task task) {
        WebSocketSession session = sessions.get(clientId);
        if (session == null || !session.isOpen()) {
            return false;
        }

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "print-task");
        message.put("payload", task);

        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int count() {
        return sessions.size();
    }
}
