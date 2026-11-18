package com.rapipedido.api.orders;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
public class Order {
    @Id
    @Column(length = 40)
    private String id;
    @Column(nullable = false)
    private String clientId;
    @Column(nullable = false)
    private Long restaurantId;
    @Column(nullable = false)
    private String deliveryAddress;
    @Column(nullable = false)
    private String paymentMethod;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    @Column(nullable = false)
    private String orderStatus;
    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}

    public Order(String id, String clientId, Long restaurantId, String deliveryAddress,
                 String paymentMethod, BigDecimal totalAmount, String orderStatus, Instant createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.restaurantId = restaurantId;
        this.deliveryAddress = deliveryAddress;
        this.paymentMethod = paymentMethod;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
        this.createdAt = createdAt;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public String getId() { return id; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getOrderStatus() { return orderStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
}
