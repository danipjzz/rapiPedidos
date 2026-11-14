package com.rapipedido.api.tracking;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/orders/{orderId}/location")
    public ResponseEntity<Map<String, Object>> lastLocation(@PathVariable String orderId) {
        Map<String, Object> body = new LinkedHashMap<>();
        return trackingService.getLastLocation(orderId)
                .map(loc -> {
                    body.put("status", "SUCCESS");
                    body.put("message", "Última ubicación del repartidor");
                    body.put("data", loc);
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> {
                    body.put("status", "ERROR");
                    body.put("errorCode", "LOCATION_NOT_FOUND");
                    body.put("message", "Aún no hay ubicación para el pedido " + orderId);
                    return ResponseEntity.status(404).body(body);
                });
    }
}