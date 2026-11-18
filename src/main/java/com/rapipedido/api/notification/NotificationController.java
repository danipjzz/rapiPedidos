package com.rapipedido.api.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getByUser(@PathVariable String userId) {
        List<NotificationResponse> notifications = notificationService.getByUser(userId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "SUCCESS");
        body.put("message", "Notificaciones obtenidas");
        body.put("data", notifications);
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable String notificationId) {
        Map<String, Object> body = new LinkedHashMap<>();
        return notificationService.markAsRead(notificationId)
                .map(n -> {
                    body.put("status", "SUCCESS");
                    body.put("message", "Notificación marcada como leída");
                    body.put("data", n);
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> {
                    body.put("status", "ERROR");
                    body.put("errorCode", "NOTIFICATION_NOT_FOUND");
                    body.put("message", "No existe la notificación " + notificationId);
                    return ResponseEntity.status(404).body(body);
                });
    }
}
