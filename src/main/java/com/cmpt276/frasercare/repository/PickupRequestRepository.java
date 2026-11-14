package com.cmpt276.frasercare.repository;

import com.cmpt276.frasercare.model.PickupRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PickupRequestRepository extends JpaRepository<PickupRequest, Long> {

    List<PickupRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<PickupRequest> findAllByOrderByCreatedAtDesc();

    List<PickupRequest> findByStatusOrderByCreatedAtDesc(String status);
}