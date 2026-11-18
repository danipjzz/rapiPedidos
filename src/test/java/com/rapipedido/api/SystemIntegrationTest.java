package com.rapipedido.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.rapipedido.api.tracking.DriverLocationRepository;
import com.rapipedido.api.tracking.LocationUpdate;
import com.rapipedido.api.tracking.TrackingService;

/**
 * Pruebas de integración de todo el sistema: levantan la aplicación completa (con H2)
 * y llaman a los endpoints reales de cada módulo.
 */
@SpringBootTest
@ActiveProfiles("h2")
class SistemaIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired TrackingService trackingService;
    @Autowired DriverLocationRepository locationRepository;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // ------------------------------------------------------------ utilidades

    private MockHttpServletResponse call(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse();
    }

    private MockHttpServletResponse get(String url) throws Exception {
        return call(MockMvcRequestBuilders.get(url));
    }

    private MockHttpServletResponse post(String url, String json) throws Exception {
        return call(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private MockHttpServletResponse patch(String url, String json) throws Exception {
        return call(MockMvcRequestBuilders.patch(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String body(MockHttpServletResponse response) throws Exception {
        return response.getContentAsString(StandardCharsets.UTF_8);
    }

    private String text(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        assertTrue(m.find(), "No se encontró el campo " + field + " en: " + json);
        return m.group(1);
    }

    private BigDecimal number(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*([0-9.]+)").matcher(json);
        assertTrue(m.find(), "No se encontró el campo " + field + " en: " + json);
        return new BigDecimal(m.group(1));
    }

    private String newUser() {
        return "usr_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static final String ITEMS_NORMALES = """
            [{"dishId":101,"quantity":2,"unitPrice":25000.0},
             {"dishId":102,"quantity":1,"unitPrice":8500.0}]""";

    private String orderJson(String clientId, String items) {
        return """
                {"clientId":"%s","restaurantId":1,"deliveryAddress":"Calle Falsa 123, Bogotá",
                 "paymentMethod":"CREDIT_CARD","items":%s}""".formatted(clientId, items);
    }

    /** Crea un pedido y devuelve el orderId. */
    private String createOrder(String clientId) throws Exception {
        MockHttpServletResponse r = post("/api/orders", orderJson(clientId, ITEMS_NORMALES));
        assertEquals(201, r.getStatus(), body(r));
        return text(body(r), "orderId");
    }

    // ------------------------------------------------------------ catálogo

    @Test
    void catalogo_devuelve_el_menu_del_restaurante() throws Exception {
        MockHttpServletResponse r = get("/api/catalog/restaurants/1/menu");

        assertEquals(200, r.getStatus());
        String json = body(r);
        assertTrue(json.contains("\"status\":\"SUCCESS\""));
        assertTrue(json.contains("Hamburguesa Artesanal"));
        assertTrue(json.contains("\"dishId\":101"));
        assertTrue(json.contains("\"dishId\":102"));
    }

    // ------------------------------------------------------------ pedidos

    @Test
    void pedido_se_crea_con_el_total_calculado() throws Exception {
        MockHttpServletResponse r = post("/api/orders", orderJson(newUser(), ITEMS_NORMALES));

        assertEquals(201, r.getStatus());
        String json = body(r);
        assertTrue(json.contains("\"status\":\"SUCCESS\""));
        assertTrue(text(json, "orderId").startsWith("ord_"));
        assertEquals(0, number(json, "totalAmount").compareTo(new BigDecimal("58500")));
        assertEquals("CREATED", text(json, "orderStatus"));
    }

    @Test
    void pedido_sin_platos_es_rechazado() throws Exception {
        MockHttpServletResponse r = post("/api/orders", orderJson(newUser(), "[]"));

        assertEquals(400, r.getStatus());
        assertTrue(body(r).contains("EMPTY_ORDER"));
    }

    @Test
    void pedido_con_un_plato_que_no_existe_es_rechazado() throws Exception {
        MockHttpServletResponse r = post("/api/orders",
                orderJson(newUser(), "[{\"dishId\":999,\"quantity\":1,\"unitPrice\":1000.0}]"));

        assertEquals(400, r.getStatus());
        assertTrue(body(r).contains("DISH_NOT_FOUND"));
    }

    @Test
    void estado_invalido_o_pedido_inexistente_es_rechazado() throws Exception {
        String orderId = createOrder(newUser());

        MockHttpServletResponse invalid = patch("/api/orders/" + orderId + "/status", "{\"status\":\"FOO\"}");
        MockHttpServletResponse missing = patch("/api/orders/ord_no_existe/status", "{\"status\":\"READY\"}");

        assertEquals(400, invalid.getStatus());
        assertTrue(body(invalid).contains("INVALID_STATUS"));
        assertEquals(400, missing.getStatus());
        assertTrue(body(missing).contains("ORDER_NOT_FOUND"));
    }

    // ------------------------------------------------------------ pagos

    @Test
    void pago_exitoso_devuelve_aprobado() throws Exception {
        String orderId = createOrder(newUser());

        MockHttpServletResponse r = post("/api/payments/process",
                "{\"orderId\":\"" + orderId + "\",\"amount\":58500,\"paymentMethod\":\"CREDIT_CARD\"}");

        assertEquals(200, r.getStatus(), body(r));
        assertTrue(body(r).contains("\"status\":\"SUCCESS\""));
        assertTrue(body(r).contains("APPROVED"));
    }

    @Test
    void pagar_dos_veces_el_mismo_pedido_no_genera_un_cobro_nuevo() throws Exception {
        String orderId = createOrder(newUser());
        String payment = "{\"orderId\":\"" + orderId + "\",\"amount\":58500,\"paymentMethod\":\"CREDIT_CARD\"}";

        String first = body(post("/api/payments/process", payment));
        String second = body(post("/api/payments/process", payment));

        assertEquals(text(first, "paymentId"), text(second, "paymentId"));
    }

    // ------------------------------------------------------------ notificaciones

    @Test
    void crear_un_pedido_genera_la_notificacion_ORDER_CREATED() throws Exception {
        String user = newUser();
        createOrder(user);

        String json = body(get("/api/notifications/user/" + user));

        assertTrue(json.contains("ORDER_CREATED"), json);
    }

    @Test
    void cambiar_el_estado_del_pedido_genera_las_notificaciones_de_listo_y_entregado() throws Exception {
        String user = newUser();
        String orderId = createOrder(user);

        MockHttpServletResponse ready = patch("/api/orders/" + orderId + "/status", "{\"status\":\"READY\"}");
        MockHttpServletResponse delivered = patch("/api/orders/" + orderId + "/status", "{\"status\":\"DELIVERED\"}");

        assertEquals(200, ready.getStatus(), body(ready));
        assertEquals(200, delivered.getStatus(), body(delivered));
        assertTrue(body(delivered).contains("DELIVERED"));
        String json = body(get("/api/notifications/user/" + user));
        assertTrue(json.contains("ORDER_READY"), json);
        assertTrue(json.contains("ORDER_DELIVERED"), json);
    }

    @Test
    void una_notificacion_se_puede_marcar_como_leida() throws Exception {
        String user = newUser();
        createOrder(user);
        String notificationId = text(body(get("/api/notifications/user/" + user)), "notificationId");

        MockHttpServletResponse r = patch("/api/notifications/" + notificationId + "/read", "{}");

        assertEquals(200, r.getStatus(), body(r));
        assertTrue(body(r).contains("\"read\":true"));
    }

    @Test
    void marcar_como_leida_una_notificacion_inexistente_responde_404() throws Exception {
        MockHttpServletResponse r = patch("/api/notifications/ntf_999999/read", "{}");

        assertEquals(404, r.getStatus());
        assertTrue(body(r).contains("NOTIFICATION_NOT_FOUND"));
    }

    // ------------------------------------------------------------ administración

    @Test
    void un_ticket_de_soporte_se_crea_y_se_cierra() throws Exception {
        MockHttpServletResponse created = post("/api/admin/support-tickets",
                "{\"userId\":\"" + newUser() + "\",\"orderId\":null,\"subject\":\"Demora\",\"description\":\"Prueba\"}");

        assertEquals(201, created.getStatus(), body(created));
        String ticketId = text(body(created), "ticketId");
        assertEquals("OPEN", text(body(created), "ticketStatus"));

        MockHttpServletResponse closed = patch("/api/admin/support-tickets/" + ticketId + "/close", "{}");

        assertEquals(200, closed.getStatus());
        assertEquals("CLOSED", text(body(closed), "ticketStatus"));
    }

    @Test
    void un_ticket_sin_asunto_es_rechazado() throws Exception {
        MockHttpServletResponse r = post("/api/admin/support-tickets",
                "{\"userId\":\"" + newUser() + "\",\"subject\":\"\",\"description\":\"x\"}");

        assertEquals(400, r.getStatus());
        assertTrue(body(r).contains("INVALID_REQUEST"));
    }

    @Test
    void cerrar_un_ticket_inexistente_responde_404() throws Exception {
        MockHttpServletResponse r = patch("/api/admin/support-tickets/tkt_999999/close", "{}");

        assertEquals(404, r.getStatus());
        assertTrue(body(r).contains("TICKET_NOT_FOUND"));
    }

    @Test
    void una_disputa_se_crea_se_resuelve_y_no_se_puede_resolver_dos_veces() throws Exception {
        MockHttpServletResponse created = post("/api/admin/disputes",
                "{\"orderId\":\"ord_1\",\"clientId\":\"" + newUser() + "\",\"reason\":\"Pedido incompleto\"}");
        assertEquals(201, created.getStatus(), body(created));
        String disputeId = text(body(created), "disputeId");
        assertEquals("OPEN", text(body(created), "disputeStatus"));

        MockHttpServletResponse resolved = patch("/api/admin/disputes/" + disputeId + "/resolve",
                "{\"resolution\":\"Reembolso parcial\"}");
        MockHttpServletResponse again = patch("/api/admin/disputes/" + disputeId + "/resolve",
                "{\"resolution\":\"Otra vez\"}");

        assertEquals(200, resolved.getStatus());
        assertEquals("RESOLVED", text(body(resolved), "disputeStatus"));
        assertEquals(409, again.getStatus());
        assertTrue(body(again).contains("DISPUTE_ALREADY_RESOLVED"));
    }

    // ------------------------------------------------------------ seguimiento

    @Test
    void sin_ubicaciones_el_seguimiento_responde_404() throws Exception {
        MockHttpServletResponse r = get("/api/tracking/orders/ord_sin_ubicacion/location");

        assertEquals(404, r.getStatus());
        assertTrue(body(r).contains("LOCATION_NOT_FOUND"));
    }

    @Test
    void la_ultima_ubicacion_del_repartidor_se_puede_consultar_y_se_guarda_en_la_base() throws Exception {
        String orderId = createOrder(newUser());

        trackingService.publish(new LocationUpdate("rep_334455", orderId, 4.60971, -74.08175, Instant.now()));

        MockHttpServletResponse r = get("/api/tracking/orders/" + orderId + "/location");
        assertEquals(200, r.getStatus(), body(r));
        assertTrue(body(r).contains("\"latitude\":4.60971"));

        // La persistencia es asíncrona: se espera hasta 3 s a que aparezca la fila.
        boolean saved = false;
        for (int i = 0; i < 30 && !saved; i++) {
            saved = !locationRepository.findByOrderIdOrderByRecordedAtAsc(orderId).isEmpty();
            if (!saved) Thread.sleep(100);
        }
        assertTrue(saved, "La ubicación no se guardó en la base de datos");
    }

    @Test
    void al_entregar_el_pedido_se_libera_el_seguimiento() throws Exception {
        String orderId = createOrder(newUser());
        trackingService.publish(new LocationUpdate("rep_334455", orderId, 4.60971, -74.08175, Instant.now()));
        assertNotNull(body(get("/api/tracking/orders/" + orderId + "/location")));
        assertEquals(200, get("/api/tracking/orders/" + orderId + "/location").getStatus());

        patch("/api/orders/" + orderId + "/status", "{\"status\":\"DELIVERED\"}");

        assertEquals(404, get("/api/tracking/orders/" + orderId + "/location").getStatus());
    }
}