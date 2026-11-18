package com.rapipedido.api.admin;

public final class AdminRequests {

    private AdminRequests() {
    }

    public record TicketRequest(
            String userId,
            String orderId,
            String subject,
            String description) {
    }

    public record DisputeRequest(
            String orderId,
            String clientId,
            String reason) {
    }

    public record ResolveRequest(
            String resolution) {
    }
}
