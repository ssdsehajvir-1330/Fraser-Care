package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.DrugProductSummary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

@Service
public class PickupSuggestionService {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_SUGGESTIONS = 8;

    private final DrugSearchService drugSearchService;

    public PickupSuggestionService(DrugSearchService drugSearchService) {
        this.drugSearchService = drugSearchService;
    }

    // Unique Health Canada brand names that match what the customer typed
    public List<String> suggestItemNames(String query) {

        String text = query == null ? "" : query.trim();

        if (text.length() < MIN_LENGTH) {
            return List.of();
        }

        List<DrugProductSummary> products;

        try {
            products = drugSearchService.searchByBrandName(text);

        } catch (IllegalArgumentException | RestClientException e) {
            // Suggestions are optional, so just show none
            return List.of();
        }

        String typed = text.toLowerCase();

        // Remove duplicates, ignoring upper/lower case
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (DrugProductSummary product : products) {
            String name = product.brandName();

            if (name != null && !name.isBlank()) {
                names.add(name.trim());
            }
        }

        // Names that start with the typed text come first
        return names.stream()
                .sorted(Comparator
                        .comparing((String name) -> !name.toLowerCase().startsWith(typed))
                        .thenComparing(String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_SUGGESTIONS)
                .toList();
    }
}