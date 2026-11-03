package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.client.DpdDrugProduct;
import com.cmpt276.frasercare.client.HealthCanadaDrugClient;
import com.cmpt276.frasercare.model.DrugProductSummary;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class DrugSearchService {

    static final int MAX_RESULTS = 50;
    private static final Pattern BRAND_NAME = Pattern.compile("[\\p{L}\\p{N} .,'&+()/-]{3,60}");
    private static final Pattern DIN = Pattern.compile("\\d{1,8}");

    private final HealthCanadaDrugClient drugClient;

    public DrugSearchService(HealthCanadaDrugClient drugClient) {
        this.drugClient = drugClient;
    }

    public List<DrugProductSummary> searchByBrandName(String brandName) {
        String query = brandName == null ? "" : brandName.trim();
        if (!BRAND_NAME.matcher(query).matches()) {
            throw new IllegalArgumentException("Brand name must be 3 to 60 characters (letters, numbers, spaces, or basic punctuation)");
        }
        return toSummaries(drugClient.searchByBrandName(query));
    }

    public List<DrugProductSummary> findByDin(String din) {
        String query = din == null ? "" : din.trim();
        if (!DIN.matcher(query).matches()) {
            throw new IllegalArgumentException("DIN must be up to 8 digits");
        }
        return toSummaries(drugClient.findByDin("0".repeat(8 - query.length()) + query));
    }

    private List<DrugProductSummary> toSummaries(List<DpdDrugProduct> products) {
        return products.stream()
                .limit(MAX_RESULTS)
                .map(p -> new DrugProductSummary(p.drugCode(), p.din(), p.brandName(),
                        p.companyName(), p.className(), p.lastUpdateDate()))
                .toList();
    }
}
