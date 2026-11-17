
package com.rapipedido.api.payments;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PaymentGateway {

    private final Map<String, Map<String, Object>> processedPayments =
            new ConcurrentHashMap<>();

    public Map<String, Object> processPayment(PaymentRequest request)
            throws InterruptedException {

        // Simulamos la comunicación con la pasarela externa
        Thread.sleep(1000);

        // Evitamos procesar dos veces el mismo pedido
        return processedPayments.computeIfAbsent(
                request.getOrderId(),
                orderId -> {

                    Map<String, Object> result = new HashMap<>();

                    result.put("paymentId",
                            "pay_" + System.currentTimeMillis());
                    result.put("orderId", orderId);
                    result.put("amount", request.getAmount());
                    result.put("paymentMethod",
                            request.getPaymentMethod());
                    result.put("paymentStatus", "APPROVED");

                    return result;
                }
        );
    }
}
