package com.rapipedido.api.admin;

import java.time.Instant;

public final class AdminResponses {

    private AdminResponses() {
    }

    public record TicketResponse(
            String ticketId,
            String userId,
            String orderId,
            String subject,
            String description,
            SupportTicket.Status ticketStatus,
            Instant createdAt) {

        public static TicketResponse from(SupportTicket t) {
            return new TicketResponse(
                    "tkt_" + t.getId(),
                    t.getUserId(),
                    t.getOrderId(),
                    t.getSubject(),
                    t.getDescription(),
                    t.getStatus(),
                    t.getCreatedAt());
        }
    }

    public record DisputeResponse(
            String disputeId,
            String orderId,
            String clientId,
            String reason,
            Dispute.Status disputeStatus,
            String resolution,
            Instant createdAt,
            Instant resolvedAt) {

        public static DisputeResponse from(Dispute d) {
            return new DisputeResponse(
                    "dsp_" + d.getId(),
                    d.getOrderId(),
                    d.getClientId(),
                    d.getReason(),
                    d.getStatus(),
                    d.getResolution(),
                    d.getCreatedAt(),
                    d.getResolvedAt());
        }
    }
}
