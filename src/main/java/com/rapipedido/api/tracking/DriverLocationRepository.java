package com.rapipedido.api.tracking;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {
    List<DriverLocation> findByOrderIdOrderByRecordedAtAsc(String orderId);
}