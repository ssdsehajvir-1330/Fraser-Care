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
        DpdDrugProduct[] products = restClient.get()
                .uri(uri -> uri.path("/drugproduct/")
                        .queryParam("brandname", brandName)
                        .queryParam("lang", "en")
                        .queryParam("type", "json")
                        .build())
                .retrieve()
                .body(DpdDrugProduct[].class);
        return products == null ? List.of() : List.of(products);
    }
}
