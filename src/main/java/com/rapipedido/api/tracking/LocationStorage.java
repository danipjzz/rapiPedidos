package com.rapipedido.api.tracking;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class LocationStorage {

    private final DriverLocationRepository repository;

    public LocationStorage(DriverLocationRepository repository) {
        this.repository = repository;
    }

    @Async
    public void save(LocationUpdate u) {
        repository.save(new DriverLocation(
                u.deliveryPersonId(), u.orderId(),
                u.latitude(), u.longitude(), u.timestamp()));
    }
}