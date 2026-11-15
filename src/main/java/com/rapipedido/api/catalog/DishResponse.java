package com.rapipedido.api.catalog;

public record DishResponse(
        Long dishId,
        String name,
        String description,
        double price,
        boolean available) {
}


