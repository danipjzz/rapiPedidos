package com.rapipedido.api.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {

    public enum Status {
        OPEN, CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String orderId;
    private String subject;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Instant createdAt;

    protected SupportTicket() {
    }

    public SupportTicket(String userId, String orderId, String subject, String description) {
        this.userId = userId;
        this.orderId = orderId;
        this.subject = subject;
        this.description = description;
        this.status = Status.OPEN;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getOrderId() { return orderId; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void close() { this.status = Status.CLOSED; }
}
