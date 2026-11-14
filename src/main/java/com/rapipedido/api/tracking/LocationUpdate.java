package com.rapipedido.api.tracking;

import java.time.Instant;

public record LocationUpdate(
        String deliveryPersonId,
        String orderId,
        double latitude,
        double longitude,
        Instant timestamp
) {}