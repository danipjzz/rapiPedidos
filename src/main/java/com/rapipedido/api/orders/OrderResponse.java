package com.rapipedido.api.orders;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(String orderId, BigDecimal totalAmount, String orderStatus, Instant createdAt) {
}
