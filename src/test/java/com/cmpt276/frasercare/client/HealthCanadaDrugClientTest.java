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

    private MockRestServiceServer server;
    private HealthCanadaDrugClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(HealthCanadaDrugClient.BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HealthCanadaDrugClient(builder);
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
    void serverErrorIsThrown() {
        server.expect(queryParam("brandname", "advil")).andRespond(withServerError());

        assertThrows(RestClientException.class, () -> client.searchByBrandName("advil"));
    }
}
