package com.rapipedido.api.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dishes")
public class Dish {

    @Id
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    private String name;
    private String description;
    private double price;
    private boolean available;

    protected Dish() {}

    //Constructor
    public Dish(Long id, Long restaurantId, String name, String description,
                double price, boolean available) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.available = available;
    }

    public Long getId() { return id; }
    public Long getRestaurantId() { return restaurantId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public boolean isAvailable() { return available; }
}