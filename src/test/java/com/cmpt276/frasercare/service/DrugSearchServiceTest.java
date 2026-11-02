package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.client.DpdDrugProduct;
import com.cmpt276.frasercare.client.HealthCanadaDrugClient;
import com.cmpt276.frasercare.model.DrugProductSummary;
import org.junit.jupiter.api.Test;
import java.util.List;
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
}
