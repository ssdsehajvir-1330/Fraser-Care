package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.model.PharmacyService;
import com.cmpt276.frasercare.service.PharmacyCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pharmacy-services")
public class PharmacyServiceController {

    private final PharmacyCatalogService pharmacyCatalogService;

    public PharmacyServiceController(PharmacyCatalogService pharmacyCatalogService) {
        this.pharmacyCatalogService = pharmacyCatalogService;
    }

    @GetMapping
    public List<PharmacyService> getActiveServices() {
        return pharmacyCatalogService.getActiveServices();
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<PharmacyService> getServiceById(@PathVariable Long serviceId) {

        Optional<PharmacyService> pharmacyService =
                pharmacyCatalogService.getServiceById(serviceId);

        if (pharmacyService.isPresent()) {
            return ResponseEntity.ok(pharmacyService.get());
        }

        return ResponseEntity.notFound().build();
    }
}
