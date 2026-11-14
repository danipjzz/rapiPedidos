package com.rapipedido.api.tracking;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class TrackingWebSocketHandler extends TextWebSocketHandler {

    private final TrackingService trackingService;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public TrackingWebSocketHandler(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = mapper.readTree(message.getPayload());

        if (json.has("action")) {
            // Es un cliente que quiere suscribirse
            String orderId = json.get("orderId").asText();
            trackingService.subscribe(orderId, session);
        } else {
            // Es el repartidor enviando su ubicación
            LocationUpdate update = new LocationUpdate(
                    json.get("deliveryPersonId").asText(),
                    json.get("orderId").asText(),
                    json.get("latitude").asDouble(),
                    json.get("longitude").asDouble(),
                    Instant.parse(json.get("timestamp").asText())
            );
            trackingService.publish(update);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        trackingService.unsubscribeAll(session);
    }
}