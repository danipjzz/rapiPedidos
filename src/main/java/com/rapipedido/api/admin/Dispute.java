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
@Table(name = "disputes")
public class Dispute {

    public enum Status {
        OPEN, RESOLVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private String clientId;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(length = 1000)
    private String resolution;

    private Instant createdAt;
    private Instant resolvedAt;

    protected Dispute() {
    }

    public Dispute(String orderId, String clientId, String reason) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.reason = reason;
        this.status = Status.OPEN;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getClientId() { return clientId; }
    public String getReason() { return reason; }
    public Status getStatus() { return status; }
    public String getResolution() { return resolution; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    public void resolve(String resolution) {
        this.status = Status.RESOLVED;
        this.resolution = resolution;
        this.resolvedAt = Instant.now();
    }
}
