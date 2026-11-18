package com.rapipedido.api.notification;

/**
 * Evento interno (en memoria) que orders y payments publican con
 * ApplicationEventPublisher cuando cambia el estado de un pedido.
 *
 * newStatus esperado: CREATED, PAYMENT_PENDING, PAYMENT_FAILED, READY, DELIVERED.
 * Cualquier otro valor se ignora.
 */
public record OrderStatusChangedEvent(
        String orderId,
        String clientId,
        String newStatus) {
}
