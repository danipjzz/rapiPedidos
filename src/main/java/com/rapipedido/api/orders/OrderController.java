package com.rapipedido.api.orders;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody OrderRequest request) {
        OrderResponse data = orderService.createOrder(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "SUCCESS");
        body.put("message", "Pedido creado correctamente");
        body.put("data", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @ExceptionHandler(OrderValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(OrderValidationException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ERROR");
        body.put("errorCode", exception.getErrorCode());
        body.put("message", exception.getMessage());
        return ResponseEntity.badRequest().body(body);
    }
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable String orderId, @RequestBody Map<String, String> request) {
        OrderResponse data = orderService.updateStatus(orderId, request.get("status"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "SUCCESS");
        body.put("message", "Estado actualizado");
        body.put("data", data);
        return ResponseEntity.ok(body);
    }
}
