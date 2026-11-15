package com.rapipedido.api.catalog;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CatalogDataLoader implements CommandLineRunner {

    private final DishRepository dishRepository;

    public CatalogDataLoader(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @Override
    public void run(String... args) {
        if (dishRepository.count() == 0) {
            dishRepository.save(new Dish(101L, 1L, "Hamburguesa Artesanal",
                    "Doble carne con queso y tocineta", 25000.0, true));
            dishRepository.save(new Dish(102L, 1L, "Papas Fritas Medianas",
                    "Papas rústicas con salsa de ajo", 8500.0, true));
        }
    }
}