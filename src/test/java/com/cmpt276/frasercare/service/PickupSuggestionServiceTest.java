package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.DrugProductSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

class PickupSuggestionServiceTest {

    private DrugSearchService drugSearchService;
    private PickupSuggestionService suggestionService;

    @BeforeEach
    void setUp() {
        drugSearchService = Mockito.mock(DrugSearchService.class);
        suggestionService = new PickupSuggestionService(drugSearchService);
    }

    // Helper to make a search result with a brand name
    private DrugProductSummary product(String brandName) {
        return new DrugProductSummary(1L, "00000001", brandName, "Company", "Human", null);
    }

    // Short text doesn't search Health Canada at all
    @Test
    void suggestItemNames_shortText_returnsNothing() {
        assertTrue(suggestionService.suggestItemNames("am").isEmpty());

        Mockito.verify(drugSearchService, Mockito.never()).searchByBrandName(anyString());
    }

    // Duplicates are removed and names starting with the text come first
    @Test
    void suggestItemNames_removesDuplicatesAndSorts() {
        Mockito.when(drugSearchService.searchByBrandName("amox"))
                .thenReturn(List.of(
                        product("PMS-AMOXICILLIN"),
                        product("AMOXICILLIN"),
                        product("Amoxicillin"),
                        product("AMOXIL")));

        List<String> result = suggestionService.suggestItemNames("amox");

        assertEquals(List.of("AMOXICILLIN", "AMOXIL", "PMS-AMOXICILLIN"), result);
    }

    // No more than 8 suggestions come back
    @Test
    void suggestItemNames_limitsToEight() {
        List<DrugProductSummary> products = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {
            products.add(product("TYLENOL " + (char) ('A' + i)));
        }

        Mockito.when(drugSearchService.searchByBrandName("tylenol")).thenReturn(products);

        assertEquals(8, suggestionService.suggestItemNames("tylenol").size());
    }

    // Bad text or Health Canada being down just gives no suggestions
    @Test
    void suggestItemNames_searchFails_returnsNothing() {
        Mockito.when(drugSearchService.searchByBrandName("refill: amox"))
                .thenThrow(new IllegalArgumentException("Bad brand name"));
        Mockito.when(drugSearchService.searchByBrandName("advil"))
                .thenThrow(new RestClientException("Health Canada down"));

        assertTrue(suggestionService.suggestItemNames("refill: amox").isEmpty());
        assertTrue(suggestionService.suggestItemNames("advil").isEmpty());
    }
}