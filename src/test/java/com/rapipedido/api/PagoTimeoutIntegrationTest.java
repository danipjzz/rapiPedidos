package com.rapipedido.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.rapipedido.api.payments.PaymentGateway;

/**
 * Escenario de calidad de disponibilidad: la pasarela externa no responde.
 * El sistema debe cortar a los 5 s, informar un pago pendiente y abrir el circuit breaker.
 */
@SpringBootTest
@ActiveProfiles("h2")
class PagoTimeoutIntegrationTest {

    @Autowired WebApplicationContext context;
    @MockitoBean PaymentGateway gateway;

    private MockHttpServletResponse pay(MockMvc mvc, String orderId) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"" + orderId + "\",\"amount\":58500,\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andReturn().getResponse();
    }

    @Test
    void si_la_pasarela_no_responde_el_pago_queda_pendiente_y_el_circuito_se_abre() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        when(gateway.processPayment(any())).thenAnswer(invocation -> {
            Thread.sleep(8000);   // la pasarela "se cuelga"
            return Map.of();
        });

        long start = System.currentTimeMillis();
        MockHttpServletResponse first = pay(mvc, "ord_timeout_1");
        long firstMs = System.currentTimeMillis() - start;

        assertEquals(504, first.getStatus());
        String json = first.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(json.contains("PAYMENT_GATEWAY_TIMEOUT"), json);
        assertTrue(json.contains("pendiente"), json);
        assertTrue(firstMs >= 4500 && firstMs < 7000,
                "Debía cortar cerca de los 5 s y tardó " + firstMs + " ms");

        // Con el circuito abierto, el siguiente pago responde de inmediato sin esperar a la pasarela.
        start = System.currentTimeMillis();
        MockHttpServletResponse second = pay(mvc, "ord_timeout_2");
        long secondMs = System.currentTimeMillis() - start;

        assertEquals(504, second.getStatus());
        assertTrue(secondMs < 1000, "Con el circuito abierto debía responder al instante y tardó " + secondMs + " ms");
    }
}