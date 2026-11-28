package com.cmpt276.frasercare.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringJUnitConfig
class HealthCanadaDrugClientCacheTest {

    private static final String ADVIL_URL = HealthCanadaDrugClient.BASE_URL + "/drugproduct/?brandname=advil&lang=en&type=json";
    private static final String MARKETED_ADVIL_URL = HealthCanadaDrugClient.BASE_URL + "/drugproduct/?brandname=advil&status=2&lang=en&type=json";
    private static final String ADVIL_JSON = """
            [{"drug_code":13452,"drug_identification_number":"01933531","brand_name":"ADVIL CAPLETS"}]
            """;

    @Configuration
    @EnableCaching
    static class Config {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder().baseUrl(HealthCanadaDrugClient.BASE_URL);
        }

        @Bean
        MockRestServiceServer server(RestClient.Builder restClientBuilder) {
            return MockRestServiceServer.bindTo(restClientBuilder).build();
        }

        // The mock server has to be bound to the builder before the client builds its RestClient
        @Bean
        @DependsOn("server")
        HealthCanadaDrugClient client(RestClient.Builder restClientBuilder) {
            return new HealthCanadaDrugClient(restClientBuilder);
        }
    }

    @Autowired
    private HealthCanadaDrugClient client;
    @Autowired
    private MockRestServiceServer server;
    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void reset() {
        server.reset();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void repeatedSearchCallsHealthCanadaOnce() {
        server.expect(ExpectedCount.once(), requestTo(ADVIL_URL))
                .andRespond(withSuccess(ADVIL_JSON, MediaType.APPLICATION_JSON));

        assertEquals(client.searchByBrandName("advil"), client.searchByBrandName("advil"));
        server.verify();
    }

    @Test
    void marketedAndFullSearchesAreCachedSeparately() {
        server.expect(requestTo(ADVIL_URL)).andRespond(withSuccess(ADVIL_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(MARKETED_ADVIL_URL)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(1, client.searchByBrandName("advil").size());
        assertTrue(client.searchMarketedByBrandName("advil").isEmpty());
        server.verify();
    }

    @Test
    void statusIsCachedPerDrugCode() {
        server.expect(ExpectedCount.once(), requestTo(HealthCanadaDrugClient.BASE_URL + "/status/?id=13452&lang=en&type=json"))
                .andRespond(withSuccess("{\"status\":\"Marketed\"}", MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(HealthCanadaDrugClient.BASE_URL + "/status/?id=13625&lang=en&type=json"))
                .andRespond(withSuccess("{\"status\":\"Dormant\"}", MediaType.APPLICATION_JSON));

        assertEquals("Marketed", client.getStatus(13452).status());
        assertEquals("Dormant", client.getStatus(13625).status());
        assertEquals("Marketed", client.getStatus(13452).status());
        server.verify();
    }

    @Test
    void failedCallIsNotCached() {
        server.expect(requestTo(ADVIL_URL)).andRespond(withServerError());
        server.expect(requestTo(ADVIL_URL)).andRespond(withSuccess(ADVIL_JSON, MediaType.APPLICATION_JSON));

        assertThrows(RestClientException.class, () -> client.searchByBrandName("advil"));
        assertEquals(1, client.searchByBrandName("advil").size());
        server.verify();
    }
}
