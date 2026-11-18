package com.rapipedido.api.notification;

import java.time.Instant;

public record NotificationResponse(
        String notificationId,
        String userId,
        String orderId,
        Notification.Type type,
        Notification.Channel channel,
        String message,
        Notification.Status notificationStatus,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                "ntf_" + n.getId(),
                n.getUserId(),
                n.getOrderId(),
                n.getType(),
                n.getChannel(),
                n.getMessage(),
                n.getStatus(),
                n.isRead(),
                n.getCreatedAt());
    }
}
