package com.rapipedido.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Prueba el WebSocket de seguimiento con conexiones reales a un servidor levantado
 * en un puerto aleatorio: un repartidor y un cliente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class WebSocketRealTest {

    @Value("${local.server.port}")
    int port;

    private WebSocketSession connect(BlockingQueue<String> inbox) throws Exception {
        TextWebSocketHandler handler = new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                inbox.add(message.getPayload());
            }
        };
        return new StandardWebSocketClient()
                .execute(handler, "ws://localhost:" + port + "/ws/tracking")
                .get(5, TimeUnit.SECONDS);
    }

    private static String location(String orderId, double lat, double lon) {
        return "{\"deliveryPersonId\":\"rep_334455\",\"orderId\":\"" + orderId + "\",\"latitude\":" + lat
                + ",\"longitude\":" + lon + ",\"timestamp\":\"2026-11-07T10:30:05Z\"}";
    }

    @Test
    void la_ubicacion_del_repartidor_llega_en_tiempo_real_al_cliente() throws Exception {
        BlockingQueue<String> driverInbox = new LinkedBlockingQueue<>();
        BlockingQueue<String> clientInbox = new LinkedBlockingQueue<>();
        WebSocketSession driver = connect(driverInbox);
        WebSocketSession client = connect(clientInbox);
        try {
            client.sendMessage(new TextMessage("{\"action\":\"SUBSCRIBE\",\"orderId\":\"ord_ws_1\"}"));

            // El repartidor envía su posición hasta que el cliente la recibe (máx. ~5 s).
            String received = null;
            for (int i = 0; i < 10 && received == null; i++) {
                driver.sendMessage(new TextMessage(location("ord_ws_1", 4.60971 + i * 0.0001, -74.08175)));
                received = clientInbox.poll(500, TimeUnit.MILLISECONDS);
            }

            assertNotNull(received, "El cliente no recibió la ubicación del repartidor");
            assertTrue(received.contains("\"orderId\":\"ord_ws_1\""));
            assertTrue(received.contains("\"latitude\":4.6"));
        } finally {
            driver.close();
            client.close();
        }
    }

    @Test
    void un_cliente_no_recibe_ubicaciones_de_otro_pedido() throws Exception {
        BlockingQueue<String> driverInbox = new LinkedBlockingQueue<>();
        BlockingQueue<String> clientInbox = new LinkedBlockingQueue<>();
        WebSocketSession driver = connect(driverInbox);
        WebSocketSession client = connect(clientInbox);
        try {
            client.sendMessage(new TextMessage("{\"action\":\"SUBSCRIBE\",\"orderId\":\"ord_ws_A\"}"));
            Thread.sleep(500);

            driver.sendMessage(new TextMessage(location("ord_ws_B", 4.7, -74.1)));

            assertTrue(clientInbox.poll(1500, TimeUnit.MILLISECONDS) == null,
                    "El cliente recibió la ubicación de un pedido que no sigue");
        } finally {
            driver.close();
            client.close();
        }
    }

    @Test
    void un_mensaje_que_no_es_json_responde_error_sin_cerrar_la_conexion() throws Exception {
        BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        WebSocketSession driver = connect(inbox);
        try {
            driver.sendMessage(new TextMessage("hola, esto no es json"));

            String answer = inbox.poll(5, TimeUnit.SECONDS);
            assertNotNull(answer);
            assertTrue(answer.contains("INVALID_JSON"));
            assertTrue(driver.isOpen(), "La conexión no debería cerrarse por un mensaje inválido");
        } finally {
            driver.close();
        }
    }

    @Test
    void una_latitud_fuera_de_rango_responde_error() throws Exception {
        BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        WebSocketSession driver = connect(inbox);
        try {
            driver.sendMessage(new TextMessage(location("ord_ws_1", 999, -74.08)));

            String answer = inbox.poll(5, TimeUnit.SECONDS);
            assertNotNull(answer);
            assertTrue(answer.contains("INVALID_COORDINATES"));
        } finally {
            driver.close();
        }
    }
}