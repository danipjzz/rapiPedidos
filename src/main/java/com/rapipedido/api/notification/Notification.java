package com.rapipedido.api.notification;

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
@Table(name = "notifications")
public class Notification {

    public enum Type {
        ORDER_CREATED, PAYMENT_PENDING, PAYMENT_FAILED, ORDER_READY, ORDER_DELIVERED
    }

    public enum Channel {
        PUSH, EMAIL
    }

    public enum Status {
        PENDING, SENT, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String orderId;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "is_read")
    private boolean read;

    private Instant createdAt;

    protected Notification() {
    }

    public Notification(String userId, String orderId, Type type, Channel channel, String message) {
        this.userId = userId;
        this.orderId = orderId;
        this.type = type;
        this.channel = channel;
        this.message = message;
        this.status = Status.PENDING;
        this.read = false;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getOrderId() { return orderId; }
    public Type getType() { return type; }
    public Channel getChannel() { return channel; }
    public String getMessage() { return message; }
    public Status getStatus() { return status; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(Status status) { this.status = status; }
    public void markRead() { this.read = true; }
}
