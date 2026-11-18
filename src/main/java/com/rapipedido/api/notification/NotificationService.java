package com.rapipedido.api.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String ID_PREFIX = "ntf_";

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        Notification.Type type = toType(event.newStatus());
        if (type == null) {
            return;
        }
        Notification notification = new Notification(
                event.clientId(),
                event.orderId(),
                type,
                Notification.Channel.PUSH,
                messageFor(type, event.orderId()));
        send(notification);
    }

    public List<NotificationResponse> getByUser(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public Optional<NotificationResponse> markAsRead(String notificationId) {
        return parseId(notificationId)
                .flatMap(repository::findById)
                .map(n -> {
                    n.markRead();
                    return NotificationResponse.from(repository.save(n));
                });
    }

    private void send(Notification notification) {
        try {
            // Aquí iría la llamada real a FCM / SMTP. Por ahora se simula con un log.
            log.info("Notificación {} para usuario {} (pedido {}): {}",
                    notification.getType(), notification.getUserId(),
                    notification.getOrderId(), notification.getMessage());
            notification.setStatus(Notification.Status.SENT);
        } catch (Exception e) {
            log.error("No se pudo enviar la notificación del pedido {}", notification.getOrderId(), e);
            notification.setStatus(Notification.Status.FAILED);
        }
        repository.save(notification);
    }

    private Notification.Type toType(String newStatus) {
        if (newStatus == null) {
            return null;
        }
        return switch (newStatus.toUpperCase()) {
            case "CREATED" -> Notification.Type.ORDER_CREATED;
            case "PAYMENT_PENDING" -> Notification.Type.PAYMENT_PENDING;
            case "PAYMENT_FAILED" -> Notification.Type.PAYMENT_FAILED;
            case "READY" -> Notification.Type.ORDER_READY;
            case "DELIVERED" -> Notification.Type.ORDER_DELIVERED;
            default -> null;
        };
    }

    private String messageFor(Notification.Type type, String orderId) {
        return switch (type) {
            case ORDER_CREATED -> "Tu pedido " + orderId + " fue creado";
            case PAYMENT_PENDING -> "El pago del pedido " + orderId + " está pendiente";
            case PAYMENT_FAILED -> "El pago del pedido " + orderId + " falló";
            case ORDER_READY -> "Tu pedido " + orderId + " está listo";
            case ORDER_DELIVERED -> "Tu pedido " + orderId + " fue entregado";
        };
    }

    private Optional<Long> parseId(String notificationId) {
        if (notificationId == null || !notificationId.startsWith(ID_PREFIX)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(notificationId.substring(ID_PREFIX.length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
