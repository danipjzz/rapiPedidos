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
        JsonNode json;
        try {
            json = mapper.readTree(message.getPayload());
        } catch (Exception e) {
            reply(session, "INVALID_JSON", "El mensaje no es un JSON válido");
            return;
        }

        if (json.has("action")) {
            String orderId = json.path("orderId").asText("");
            if (orderId.isBlank()) {
                reply(session, "INVALID_ORDER", "Falta el orderId");
                return;
            }
            trackingService.subscribe(orderId, session);
            return;
        }

        String orderId = json.path("orderId").asText("");
        String driverId = json.path("deliveryPersonId").asText("");
        if (orderId.isBlank() || driverId.isBlank()
                || !json.has("latitude") || !json.has("longitude")) {
            reply(session, "INVALID_LOCATION", "Faltan campos obligatorios");
            return;
        }

        double lat = json.get("latitude").asDouble();
        double lon = json.get("longitude").asDouble();
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            reply(session, "INVALID_COORDINATES", "Coordenadas fuera de rango");
            return;
        }

        Instant ts = json.hasNonNull("timestamp")
                ? Instant.parse(json.get("timestamp").asText())
                : Instant.now();

        trackingService.publish(new LocationUpdate(driverId, orderId, lat, lon, ts));
    }

    private void reply(WebSocketSession session, String code, String msg) throws Exception {
        session.sendMessage(new TextMessage(
                "{\"status\":\"ERROR\",\"errorCode\":\"" + code + "\",\"message\":\"" + msg + "\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        trackingService.unsubscribeAll(session);
    }
}