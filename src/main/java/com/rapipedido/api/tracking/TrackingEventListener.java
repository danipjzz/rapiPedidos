package com.rapipedido.api.tracking;

import com.rapipedido.api.notification.OrderStatusChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TrackingEventListener {

    private final TrackingService trackingService;

    public TrackingEventListener(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if ("DELIVERED".equalsIgnoreCase(event.newStatus())) {
            trackingService.closeOrder(event.orderId());
        }
    }
}