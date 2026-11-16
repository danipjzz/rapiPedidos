package com.rapipedido.api.catalog;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final DishRepository dishRepository;

    public CatalogService(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    public List<DishResponse> getMenu(Long restaurantId) {
        return dishRepository.findByRestaurantId(restaurantId).stream()
                .map(d -> new DishResponse(
                        d.getId(),
                        d.getName(),
                        d.getDescription(),
                        d.getPrice(),
                        d.isAvailable()))
                .toList();
    }
}