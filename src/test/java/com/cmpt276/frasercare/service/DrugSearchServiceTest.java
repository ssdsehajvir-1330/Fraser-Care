package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.client.*;
import com.cmpt276.frasercare.model.ActiveIngredient;
import com.cmpt276.frasercare.model.DrugProductDetails;
import com.cmpt276.frasercare.model.DrugProductSummary;
import org.junit.jupiter.api.Test;
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
        when(client.searchByBrandName("advil")).thenReturn(List.of(product(1)));

        DrugProductSummary summary = service.searchByBrandName("  advil ").get(0);

        assertEquals("PRODUCT 1", summary.brandName());
        assertEquals("TEST PHARMA", summary.company());
        assertEquals("Human", summary.productClass());
    }

    @Test
    void shortOrInvalidBrandIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.searchByBrandName("ad"));
        assertThrows(IllegalArgumentException.class, () -> service.searchByBrandName("{advil}"));
        assertThrows(IllegalArgumentException.class, () -> service.searchByBrandName(null));
        verifyNoInteractions(client);
    }

    @Test
    void dinIsPaddedToEightDigits() {
        when(client.findByDin("01933531")).thenReturn(List.of(product(1)));

        assertEquals(1, service.findByDin("1933531").size());
        verify(client).findByDin("01933531");
    }

    @Test
    void nonNumericDinIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.findByDin("ABC123"));
        assertThrows(IllegalArgumentException.class, () -> service.findByDin("123456789"));
    }

    @Test
    void resultsAreCapped() {
        List<DpdDrugProduct> many = LongStream.range(0, 120).mapToObj(this::product).toList();
        when(client.searchByBrandName("vitamin")).thenReturn(many);

        assertEquals(DrugSearchService.MAX_RESULTS, service.searchByBrandName("vitamin").size());
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
