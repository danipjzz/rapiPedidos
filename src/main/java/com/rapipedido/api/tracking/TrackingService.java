package com.rapipedido.api.tracking;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;


import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.Locale;
import org.springframework.web.socket.TextMessage;



@Service
public class TrackingService {

    private final Map<String, Set<WebSocketSession>> subscribers = new ConcurrentHashMap<>();

    // orderId -> última ubicación conocida
    private final Map<String, LocationUpdate> lastLocations = new ConcurrentHashMap<>();

    public void subscribe(String orderId, WebSocketSession session) {
        subscribers
                .computeIfAbsent(orderId, k -> ConcurrentHashMap.newKeySet())
                .add(session);

        // Si ya hay una ubicación guardada, se la mandamos de una
        LocationUpdate last = lastLocations.get(orderId);
        if (last != null) {
            send(session, toJson(last));
        }
    }

    public void unsubscribeAll(WebSocketSession session) {
        subscribers.values().forEach(set -> set.remove(session));
        subscribers.values().removeIf(Set::isEmpty);
    }

    public void closeOrder(String orderId) {
        lastLocations.remove(orderId);
        subscribers.remove(orderId);
    }

    public void publish(LocationUpdate update) {
        // 1. Guardamos como "última ubicación" de ese pedido
        lastLocations.put(update.orderId(), update);

        // 2. Buscamos quién está suscrito a ese pedido
        Set<WebSocketSession> sessions = subscribers.get(update.orderId());
        if (sessions == null) {
            return; // nadie está mirando este pedido, no hay a quién enviar
        }

        // 3. Convertimos la ubicación a JSON y la enviamos a cada uno
        String json = toJson(update);
        sessions.forEach(session -> send(session, json));
    }

    public Optional<LocationUpdate> getLastLocation(String orderId) {
        return Optional.ofNullable(lastLocations.get(orderId));
    }

    private void send(WebSocketSession session, String json) {
        try {
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            System.out.println("No se pudo enviar a " + session.getId());
        }
    }

    private String toJson(LocationUpdate u) {
        return String.format(Locale.US,
                "{\"deliveryPersonId\":\"%s\",\"orderId\":\"%s\",\"latitude\":%s,\"longitude\":%s,\"timestamp\":\"%s\"}",
                u.deliveryPersonId(), u.orderId(), u.latitude(), u.longitude(), u.timestamp());
    }
}
