package com.cmpt276.frasercare.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Optional;

@Component
public class HealthCanadaDrugClient {

    static final String BASE_URL = "https://health-products.canada.ca/api/drug";
    static final int MARKETED_STATUS = 2;

    private final RestClient restClient;

    @Autowired
    public HealthCanadaDrugClient(@Value("${dpd.api.base-url:" + BASE_URL + "}") String baseUrl) {
        this(RestClient.builder().baseUrl(baseUrl));
    }

    HealthCanadaDrugClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    @Cacheable("drugsByBrand")
    public List<DpdDrugProduct> searchByBrandName(String brandName) {
        return fetchProducts("brandname", brandName, null);
    }

    @Cacheable("marketedDrugsByBrand")
    public List<DpdDrugProduct> searchMarketedByBrandName(String brandName) {
        return fetchProducts("brandname", brandName, MARKETED_STATUS);
    }

    @Cacheable("drugsByDin")
    public List<DpdDrugProduct> findByDin(String din) {
        return fetchProducts("din", din, null);
    }

    @Cacheable("activeIngredients")
    public List<DpdActiveIngredient> getActiveIngredients(long drugCode) {
        return fetchList("/activeingredient/", drugCode, DpdActiveIngredient[].class);
    }

    @Cacheable("dosageForms")
    public List<DpdDosageForm> getDosageForms(long drugCode) {
        return fetchList("/form/", drugCode, DpdDosageForm[].class);
    }

    @Cacheable("routes")
    public List<DpdRoute> getRoutes(long drugCode) {
        return fetchList("/route/", drugCode, DpdRoute[].class);
    }

    @Cacheable("schedules")
    public List<DpdSchedule> getSchedules(long drugCode) {
        return fetchList("/schedule/", drugCode, DpdSchedule[].class);
    }

    // Unlike the other endpoints, status/?id= returns a single object
    @Cacheable("drugStatus")
    public DpdStatus getStatus(long drugCode) {
        return getByDrugCode("/status/", drugCode).body(DpdStatus.class);
    }

    private <T> List<T> fetchList(String path, long drugCode, Class<T[]> type) {
        T[] items = getByDrugCode(path, drugCode).body(type);
        return items == null ? List.of() : List.of(items);
    }

    private RestClient.ResponseSpec getByDrugCode(String path, long drugCode) {
        return restClient.get()
                .uri(uri -> uri.path(path)
                        .queryParam("id", drugCode)
                        .queryParam("lang", "en")
                        .queryParam("type", "json")
                        .build())
                .retrieve();
    }

    private List<DpdDrugProduct> fetchProducts(String param, String value, Integer status) {
        DpdDrugProduct[] products = restClient.get()
                .uri(uri -> uri.path("/drugproduct/")
                        .queryParam(param, "{value}")
                        .queryParamIfPresent("status", Optional.ofNullable(status))
                        .queryParam("lang", "en")
                        .queryParam("type", "json")
                        .build(value))
                .retrieve()
                .body(DpdDrugProduct[].class);
        return products == null ? List.of() : List.of(products);
    }
}
