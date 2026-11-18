package com.rapipedido.api.admin;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private static final String TICKET_PREFIX = "tkt_";
    private static final String DISPUTE_PREFIX = "dsp_";

    /** Error de negocio del módulo admin; el controller lo convierte en la respuesta ERROR. */
    public static class AdminException extends RuntimeException {
        private final String errorCode;
        private final int httpStatus;

        public AdminException(String errorCode, String message, int httpStatus) {
            super(message);
            this.errorCode = errorCode;
            this.httpStatus = httpStatus;
        }

        public String getErrorCode() { return errorCode; }
        public int getHttpStatus() { return httpStatus; }
    }

    private final SupportTicketRepository ticketRepository;
    private final DisputeRepository disputeRepository;

    public AdminService(SupportTicketRepository ticketRepository, DisputeRepository disputeRepository) {
        this.ticketRepository = ticketRepository;
        this.disputeRepository = disputeRepository;
    }

    // ---------- Tickets de soporte ----------

    public AdminResponses.TicketResponse createTicket(AdminRequests.TicketRequest request) {
        if (isBlank(request.userId()) || isBlank(request.subject())) {
            throw invalid("userId y subject son obligatorios");
        }
        SupportTicket ticket = new SupportTicket(
                request.userId(), request.orderId(), request.subject(), request.description());
        return AdminResponses.TicketResponse.from(ticketRepository.save(ticket));
    }

    public List<AdminResponses.TicketResponse> getTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AdminResponses.TicketResponse::from)
                .toList();
    }

    public AdminResponses.TicketResponse closeTicket(String ticketId) {
        SupportTicket ticket = ticketRepository.findById(parseId(ticketId, TICKET_PREFIX, "TICKET_NOT_FOUND", "el ticket"))
                .orElseThrow(() -> notFound("TICKET_NOT_FOUND", "el ticket", ticketId));
        ticket.close();
        return AdminResponses.TicketResponse.from(ticketRepository.save(ticket));
    }

    // ---------- Disputas ----------

    public AdminResponses.DisputeResponse createDispute(AdminRequests.DisputeRequest request) {
        if (isBlank(request.orderId()) || isBlank(request.clientId()) || isBlank(request.reason())) {
            throw invalid("orderId, clientId y reason son obligatorios");
        }
        Dispute dispute = new Dispute(request.orderId(), request.clientId(), request.reason());
        return AdminResponses.DisputeResponse.from(disputeRepository.save(dispute));
    }

    public List<AdminResponses.DisputeResponse> getDisputes() {
        return disputeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AdminResponses.DisputeResponse::from)
                .toList();
    }

    public AdminResponses.DisputeResponse resolveDispute(String disputeId, AdminRequests.ResolveRequest request) {
        if (isBlank(request.resolution())) {
            throw invalid("resolution es obligatorio");
        }
        Dispute dispute = disputeRepository.findById(parseId(disputeId, DISPUTE_PREFIX, "DISPUTE_NOT_FOUND", "la disputa"))
                .orElseThrow(() -> notFound("DISPUTE_NOT_FOUND", "la disputa", disputeId));
        if (dispute.getStatus() == Dispute.Status.RESOLVED) {
            throw new AdminException("DISPUTE_ALREADY_RESOLVED",
                    "La disputa " + disputeId + " ya fue resuelta", 409);
        }
        dispute.resolve(request.resolution());
        return AdminResponses.DisputeResponse.from(disputeRepository.save(dispute));
    }

    // ---------- Utilidades ----------

    private Long parseId(String id, String prefix, String errorCode, String label) {
        if (id == null || !id.startsWith(prefix)) {
            throw notFound(errorCode, label, id);
        }
        try {
            return Long.parseLong(id.substring(prefix.length()));
        } catch (NumberFormatException e) {
            throw notFound(errorCode, label, id);
        }
    }

    private AdminException notFound(String errorCode, String label, String id) {
        return new AdminException(errorCode, "No existe " + label + " " + id, 404);
    }

    private AdminException invalid(String message) {
        return new AdminException("INVALID_REQUEST", message, 400);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
