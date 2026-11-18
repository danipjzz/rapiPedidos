package com.rapipedido.api.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    List<Dispute> findAllByOrderByCreatedAtDesc();
}
