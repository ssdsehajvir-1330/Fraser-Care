package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.service.DrugSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/drugs")
public class DrugSearchController {

    private final DrugSearchService drugSearchService;

    public DrugSearchController(DrugSearchService drugSearchService) {
        this.drugSearchService = drugSearchService;
    }

    @GetMapping
    public ResponseEntity<?> search(@RequestParam(required = false) String brand,
                                    @RequestParam(required = false) String din) {
        try {
            if (din != null && !din.isBlank()) {
                return ResponseEntity.ok(drugSearchService.findByDin(din));
            }
            return ResponseEntity.ok(drugSearchService.searchByBrandName(brand));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (RestClientException e) {
            return healthCanadaUnavailable();
        }
    }

    @GetMapping("/{drugCode}")
    public ResponseEntity<?> details(@PathVariable long drugCode) {
        try {
            return ResponseEntity.ok(drugSearchService.getDetails(drugCode));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (RestClientException e) {
            return healthCanadaUnavailable();
        }
    }

    private ResponseEntity<String> healthCanadaUnavailable() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("The Health Canada drug database is unavailable right now. Please try again later.");
    }
}
