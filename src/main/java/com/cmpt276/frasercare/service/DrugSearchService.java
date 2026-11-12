package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.client.*;
import com.cmpt276.frasercare.model.ActiveIngredient;
import com.cmpt276.frasercare.model.DrugProductDetails;
import com.cmpt276.frasercare.model.DrugProductSummary;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public DrugProductDetails getDetails(long drugCode) {
        if (drugCode <= 0) {
            throw new IllegalArgumentException("Drug code must be a positive number");
        }
        List<ActiveIngredient> ingredients = drugClient.getActiveIngredients(drugCode).stream()
                .map(i -> new ActiveIngredient(i.ingredientName(), formatStrength(i)))
                .toList();
        DpdStatus status = drugClient.getStatus(drugCode);
        if (ingredients.isEmpty() && (status == null || status.status() == null)) {
            throw new NoSuchElementException("No product found for that drug code");
        }
        return new DrugProductDetails(drugCode, ingredients,
                names(drugClient.getDosageForms(drugCode), DpdDosageForm::name),
                names(drugClient.getRoutes(drugCode), DpdRoute::name),
                names(drugClient.getSchedules(drugCode), DpdSchedule::name),
                status == null ? null : status.status());
    }

    // e.g. "200 MG", or "100 MG / 5 ML" for liquids
    static String formatStrength(DpdActiveIngredient ingredient) {
        String strength = joinNonBlank(ingredient.strength(), ingredient.strengthUnit());
        String per = joinNonBlank(ingredient.dosageValue(), ingredient.dosageUnit());
        return per.isEmpty() ? strength : strength + " / " + per;
    }

    private static String joinNonBlank(String... parts) {
        return Stream.of(parts).filter(p -> p != null && !p.isBlank()).map(String::trim).collect(Collectors.joining(" "));
    }

    private static <T> List<String> names(List<T> items, Function<T, String> name) {
        return items.stream().map(name).filter(Objects::nonNull).distinct().toList();
    }

    private List<DrugProductSummary> toSummaries(List<DpdDrugProduct> products) {
        return products.stream()
                .limit(MAX_RESULTS)
                .map(p -> new DrugProductSummary(p.drugCode(), p.din(), p.brandName(),
                        p.companyName(), p.className(), p.lastUpdateDate()))
                .toList();
    }
}
