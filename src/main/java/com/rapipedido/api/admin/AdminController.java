package com.rapipedido.api.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ---------- Tickets de soporte ----------

    @PostMapping("/support-tickets")
    public ResponseEntity<Map<String, Object>> createTicket(@RequestBody AdminRequests.TicketRequest request) {
        return ResponseEntity.status(201).body(success("Ticket creado", adminService.createTicket(request)));
    }

    @GetMapping("/support-tickets")
    public ResponseEntity<Map<String, Object>> getTickets() {
        return ResponseEntity.ok(success("Tickets obtenidos", adminService.getTickets()));
    }

    @PatchMapping("/support-tickets/{ticketId}/close")
    public ResponseEntity<Map<String, Object>> closeTicket(@PathVariable String ticketId) {
        return ResponseEntity.ok(success("Ticket cerrado", adminService.closeTicket(ticketId)));
    }

    // ---------- Disputas ----------

    @PostMapping("/disputes")
    public ResponseEntity<Map<String, Object>> createDispute(@RequestBody AdminRequests.DisputeRequest request) {
        return ResponseEntity.status(201).body(success("Disputa creada", adminService.createDispute(request)));
    }

    @GetMapping("/disputes")
    public ResponseEntity<Map<String, Object>> getDisputes() {
        return ResponseEntity.ok(success("Disputas obtenidas", adminService.getDisputes()));
    }

    @PatchMapping("/disputes/{disputeId}/resolve")
    public ResponseEntity<Map<String, Object>> resolveDispute(
            @PathVariable String disputeId,
            @RequestBody AdminRequests.ResolveRequest request) {
        return ResponseEntity.ok(success("Disputa resuelta", adminService.resolveDispute(disputeId, request)));
    }

    // ---------- Errores (solo de este controller) ----------

    @ExceptionHandler(AdminService.AdminException.class)
    public ResponseEntity<Map<String, Object>> handleAdminException(AdminService.AdminException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ERROR");
        body.put("errorCode", e.getErrorCode());
        body.put("message", e.getMessage());
        return ResponseEntity.status(e.getHttpStatus()).body(body);
    }

    private Map<String, Object> success(String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "SUCCESS");
        body.put("message", message);
        body.put("data", data);
        return body;
    }
}
