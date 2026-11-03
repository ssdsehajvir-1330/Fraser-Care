package com.cmpt276.frasercare.repository;

import com.cmpt276.frasercare.model.PharmacyService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PharmacyServiceRepository extends JpaRepository<PharmacyService, Long> {

    List<PharmacyService> findByActiveTrue();

}
