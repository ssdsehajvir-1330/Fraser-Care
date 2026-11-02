package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.PharmacyService;
import com.cmpt276.frasercare.repository.PharmacyServiceRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PharmacyCatalogService {

    private final PharmacyServiceRepository pharmacyServiceRepository;

    public PharmacyCatalogService(PharmacyServiceRepository pharmacyServiceRepository) {
        this.pharmacyServiceRepository = pharmacyServiceRepository;
    }

    public List<PharmacyService> getActiveServices() {
        return pharmacyServiceRepository.findByActiveTrue();
    }

    public Optional<PharmacyService> getServiceById(Long serviceId) {
        return pharmacyServiceRepository.findById(serviceId);
    }
}
