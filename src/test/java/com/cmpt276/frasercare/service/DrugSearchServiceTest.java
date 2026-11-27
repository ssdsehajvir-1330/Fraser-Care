package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.client.*;
import com.cmpt276.frasercare.model.ActiveIngredient;
import com.cmpt276.frasercare.model.DrugProductDetails;
import com.cmpt276.frasercare.model.DrugProductSummary;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DrugSearchServiceTest {

    private final HealthCanadaDrugClient client = mock(HealthCanadaDrugClient.class);
    private final DrugSearchService service = new DrugSearchService(client);

    private DpdDrugProduct product(long code) {
        return new DpdDrugProduct(code, "0000000" + (code % 10), "PRODUCT " + code, "",
                "Human", "1", "TEST PHARMA", "2026-01-01");
    }

    @Test
    void brandSearchMapsToSummary() {
        when(client.searchMarketedByBrandName("advil")).thenReturn(List.of(product(1)));

        DrugProductSummary summary = service.searchByBrandName("  advil ", false).get(0);

        assertEquals("PRODUCT 1", summary.brandName());
        assertEquals("TEST PHARMA", summary.company());
        assertEquals("Human", summary.productClass());
    }

    @Test
    void defaultSearchOnlyShowsMarketedProducts() {
        when(client.searchMarketedByBrandName("advil")).thenReturn(List.of(product(1), product(2)));

        List<DrugProductSummary> results = service.searchByBrandName("advil", false);

        assertEquals(List.of("Marketed", "Marketed"), results.stream().map(DrugProductSummary::status).toList());
        verify(client, never()).searchByBrandName(any());
        verify(client, never()).getStatus(anyLong());
    }

    @Test
    void includingCancelledListsMarketedFirstWithEachStatus() {
        when(client.searchMarketedByBrandName("advil")).thenReturn(List.of(product(2)));
        when(client.searchByBrandName("advil")).thenReturn(List.of(product(1), product(2), product(3)));
        when(client.getStatus(1)).thenReturn(new DpdStatus("Cancelled Post Market", "2020-01-01"));
        when(client.getStatus(3)).thenReturn(new DpdStatus("Dormant", "2021-01-01"));

        List<DrugProductSummary> results = service.searchByBrandName("advil", true);

        assertEquals(List.of(2L, 1L, 3L), results.stream().map(DrugProductSummary::drugCode).toList());
        assertEquals(List.of("Marketed", "Cancelled Post Market", "Dormant"),
                results.stream().map(DrugProductSummary::status).toList());
        verify(client, never()).getStatus(2);
    }

    @Test
    void oneArgumentSearchHidesCancelledProducts() {
        when(client.searchMarketedByBrandName("advil")).thenReturn(List.of(product(1)));

        assertEquals("Marketed", service.searchByBrandName("advil").get(0).status());
        verify(client, never()).searchByBrandName(any());
    }

    @Test
    void brandQueryIsLowercasedSoCaseVariantsShareACacheEntry() {
        service.searchByBrandName("ADVIL", false);

        verify(client).searchMarketedByBrandName("advil");
    }

    @Test
    void shortOrInvalidBrandIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.searchByBrandName("ad", false));
        assertThrows(IllegalArgumentException.class, () -> service.searchByBrandName("{advil}", false));
        assertThrows(IllegalArgumentException.class, () -> service.searchByBrandName(null, true));
        verifyNoInteractions(client);
    }

    @Test
    void dinIsPaddedToEightDigits() {
        when(client.findByDin("01933531")).thenReturn(List.of(product(1)));

        assertEquals(1, service.findByDin("1933531").size());
        verify(client).findByDin("01933531");
    }

    @Test
    void dinLookupStillShowsCancelledProducts() {
        when(client.findByDin("00000001")).thenReturn(List.of(product(1)));
        when(client.getStatus(1)).thenReturn(new DpdStatus("Cancelled Post Market", "2020-01-01"));

        assertEquals("Cancelled Post Market", service.findByDin("1").get(0).status());
    }

    @Test
    void nonNumericDinIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.findByDin("ABC123"));
        assertThrows(IllegalArgumentException.class, () -> service.findByDin("123456789"));
    }

    @Test
    void resultsAreCapped() {
        List<DpdDrugProduct> many = LongStream.range(0, 120).mapToObj(this::product).toList();
        when(client.searchMarketedByBrandName("vitamin")).thenReturn(many);

        assertEquals(DrugSearchService.MAX_RESULTS, service.searchByBrandName("vitamin", false).size());
    }

    @Test
    void statusLookupsAreCappedToo() {
        when(client.searchByBrandName("vitamin")).thenReturn(LongStream.range(0, 120).mapToObj(this::product).toList());

        service.searchByBrandName("vitamin", true);

        verify(client, times(DrugSearchService.MAX_RESULTS)).getStatus(anyLong());
    }

    @Test
    void healthCanadaErrorDuringStatusLookupIsPassedOn() {
        when(client.findByDin("00000001")).thenReturn(List.of(product(1)));
        when(client.getStatus(1)).thenThrow(new RestClientException("down"));

        assertThrows(RestClientException.class, () -> service.findByDin("1"));
    }

    @Test
    void detailsCombineIngredientsFormRouteScheduleAndStatus() {
        when(client.getActiveIngredients(13452)).thenReturn(List.of(new DpdActiveIngredient("IBUPROFEN", "200", "MG", "", "")));
        when(client.getStatus(13452)).thenReturn(new DpdStatus("Marketed", "2026-09-09"));
        when(client.getDosageForms(13452)).thenReturn(List.of(new DpdDosageForm("Tablet")));
        when(client.getRoutes(13452)).thenReturn(List.of(new DpdRoute("Oral")));
        when(client.getSchedules(13452)).thenReturn(List.of(new DpdSchedule("NON-PRESCRIPTION DRUGS")));

        DrugProductDetails details = service.getDetails(13452);

        assertEquals(List.of(new ActiveIngredient("IBUPROFEN", "200 MG")), details.activeIngredients());
        assertEquals(List.of("Tablet"), details.dosageForms());
        assertEquals(List.of("Oral"), details.routes());
        assertEquals(List.of("NON-PRESCRIPTION DRUGS"), details.schedules());
        assertEquals("Marketed", details.status());
    }

    @Test
    void liquidStrengthIncludesVolume() {
        assertEquals("100 MG / 5 ML", DrugSearchService.formatStrength(new DpdActiveIngredient("IBUPROFEN", "100", "MG", "5", "ML")));
        assertEquals("0.5 %", DrugSearchService.formatStrength(new DpdActiveIngredient("HYDROCORTISONE", "0.5", "%", null, "")));
    }

    @Test
    void unknownDrugCodeIsNotFound() {
        when(client.getActiveIngredients(99999999)).thenReturn(List.of());
        when(client.getStatus(99999999)).thenReturn(new DpdStatus(null, null));

        assertThrows(NoSuchElementException.class, () -> service.getDetails(99999999));
    }

    @Test
    void nonPositiveDrugCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.getDetails(0));
        verifyNoInteractions(client);
    }
}
