package com.cmpt276.frasercare.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HealthCanadaDrugClientTest {

    private static final String ADVIL_JSON = """
            [{"drug_code":13452,"class_name":"Human","drug_identification_number":"01933531",
              "brand_name":"ADVIL CAPLETS","descriptor":"","number_of_ais":"1","ai_group_no":"0108883004",
              "company_name":"HALEON CANADA ULC","last_update_date":"2026-09-24"}]
            """;

    private static final String IBUPROFEN_JSON = """
            [{"dosage_unit":"","dosage_value":"","drug_code":13452,"ingredient_name":"IBUPROFEN",
              "strength":"200","strength_unit":"MG"}]
            """;

    private MockRestServiceServer server;
    private HealthCanadaDrugClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(HealthCanadaDrugClient.BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HealthCanadaDrugClient(builder);
    }

    private String url(String path) {
        return HealthCanadaDrugClient.BASE_URL + path + "?id=13452&lang=en&type=json";
    }

    @Test
    void brandNameSearchMapsFields() {
        server.expect(requestTo(HealthCanadaDrugClient.BASE_URL + "/drugproduct/?brandname=advil&lang=en&type=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(ADVIL_JSON, MediaType.APPLICATION_JSON));

        List<DpdDrugProduct> products = client.searchByBrandName("advil");

        server.verify();
        assertEquals(1, products.size());
        DpdDrugProduct advil = products.get(0);
        assertEquals(13452L, advil.drugCode());
        assertEquals("01933531", advil.din());
        assertEquals("ADVIL CAPLETS", advil.brandName());
        assertEquals("HALEON CANADA ULC", advil.companyName());
    }

    @Test
    void dinLookupWithNoMatchReturnsEmptyList() {
        server.expect(queryParam("din", "99999999"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.findByDin("99999999").isEmpty());
        server.verify();
    }

    @Test
    void activeIngredientsMapStrength() {
        server.expect(requestTo(url("/activeingredient/")))
                .andRespond(withSuccess(IBUPROFEN_JSON, MediaType.APPLICATION_JSON));

        List<DpdActiveIngredient> ingredients = client.getActiveIngredients(13452);

        server.verify();
        assertEquals(List.of(new DpdActiveIngredient("IBUPROFEN", "200", "MG", "", "")), ingredients);
    }

    @Test
    void formRouteAndScheduleAreLists() {
        server.expect(requestTo(url("/form/")))
                .andRespond(withSuccess("""
                        [{"drug_code":13452,"pharmaceutical_form_code":85,"pharmaceutical_form_name":"Tablet"}]
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(url("/route/")))
                .andRespond(withSuccess("""
                        [{"drug_code":13452,"route_of_administration_code":56,"route_of_administration_name":"Oral"}]
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(url("/schedule/")))
                .andRespond(withSuccess("""
                        [{"drug_code":13452,"schedule_name":"NON-PRESCRIPTION DRUGS"}]
                        """, MediaType.APPLICATION_JSON));

        assertEquals(List.of(new DpdDosageForm("Tablet")), client.getDosageForms(13452));
        assertEquals(List.of(new DpdRoute("Oral")), client.getRoutes(13452));
        assertEquals(List.of(new DpdSchedule("NON-PRESCRIPTION DRUGS")), client.getSchedules(13452));
        server.verify();
    }

    @Test
    void statusIsASingleObject() {
        server.expect(requestTo(url("/status/")))
                .andRespond(withSuccess("""
                        {"drug_code":13452,"status":"Marketed","history_date":"2026-09-09",
                         "original_market_date":"1992-12-31","external_status_code":2,"expiration_date":null,"lot_number":""}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("Marketed", client.getStatus(13452).status());
        server.verify();
    }

    @Test
    void serverErrorIsThrown() {
        server.expect(queryParam("brandname", "advil")).andRespond(withServerError());

        assertThrows(RestClientException.class, () -> client.searchByBrandName("advil"));
    }
}
