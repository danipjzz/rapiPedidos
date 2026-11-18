package com.rapipedido.api.tracking;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "driver_locations",
        indexes = @Index(name = "idx_driver_locations_order", columnList = "orderId"))
public class DriverLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deliveryPersonId;
    private String orderId;
    private double latitude;
    private double longitude;
    private Instant recordedAt;

    protected DriverLocation() {}

    public DriverLocation(String deliveryPersonId, String orderId,
                          double latitude, double longitude, Instant recordedAt) {
        this.deliveryPersonId = deliveryPersonId;
        this.orderId = orderId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public String getDeliveryPersonId() { return deliveryPersonId; }
    public String getOrderId() { return orderId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Instant getRecordedAt() { return recordedAt; }
}