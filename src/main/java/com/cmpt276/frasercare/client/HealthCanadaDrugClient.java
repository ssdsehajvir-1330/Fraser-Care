package com.cmpt276.frasercare.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;

@Component
public class HealthCanadaDrugClient {

    static final String BASE_URL = "https://health-products.canada.ca/api/drug";

    private final RestClient restClient;

    @Autowired
    public HealthCanadaDrugClient(@Value("${dpd.api.base-url:" + BASE_URL + "}") String baseUrl) {
        this(RestClient.builder().baseUrl(baseUrl));
    }

    HealthCanadaDrugClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public List<DpdDrugProduct> searchByBrandName(String brandName) {
        return fetchProducts("brandname", brandName);
    }

    public List<DpdDrugProduct> findByDin(String din) {
        return fetchProducts("din", din);
    }

    public List<DpdActiveIngredient> getActiveIngredients(long drugCode) {
        return fetchList("/activeingredient/", drugCode, DpdActiveIngredient[].class);
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

    private List<DpdDrugProduct> fetchProducts(String param, String value) {
        DpdDrugProduct[] products = restClient.get()
                .uri(uri -> uri.path("/drugproduct/")
                        .queryParam(param, "{value}")
                        .queryParam("lang", "en")
                        .queryParam("type", "json")
                        .build(value))
                .retrieve()
                .body(DpdDrugProduct[].class);
        return products == null ? List.of() : List.of(products);
    }
}
