package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.client.*;
import com.cmpt276.frasercare.model.ActiveIngredient;
import com.cmpt276.frasercare.model.DrugProductDetails;
import com.cmpt276.frasercare.model.DrugProductSummary;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DrugSearchService {

    static final int MAX_RESULTS = 50;
    static final String MARKETED = "Marketed";
    private static final Pattern BRAND_NAME = Pattern.compile("[\\p{L}\\p{N} .,'&+()/-]{3,60}");
    private static final Pattern DIN = Pattern.compile("\\d{1,8}");

    private final HealthCanadaDrugClient drugClient;

    public DrugSearchService(HealthCanadaDrugClient drugClient) {
        this.drugClient = drugClient;
    }

    public List<DrugProductSummary> searchByBrandName(String brandName) {
        return searchByBrandName(brandName, false);
    }

    public List<DrugProductSummary> searchByBrandName(String brandName, boolean includeCancelled) {
        String query = brandName == null ? "" : brandName.trim();
        if (!BRAND_NAME.matcher(query).matches()) {
            throw new IllegalArgumentException("Brand name must be 3 to 60 characters (letters, numbers, spaces, or basic punctuation)");
        }
        // DPD search is case-insensitive, so "Advil" and "advil" share one cache entry
        query = query.toLowerCase(Locale.ROOT);
        List<DpdDrugProduct> marketed = drugClient.searchMarketedByBrandName(query);
        Set<Long> marketedCodes = marketed.stream().map(DpdDrugProduct::drugCode).collect(Collectors.toSet());
        if (!includeCancelled) {
            return withStatuses(marketed, marketedCodes);
        }
        return withStatuses(drugClient.searchByBrandName(query).stream()
                .sorted(Comparator.comparing(p -> !marketedCodes.contains(p.drugCode())))
                .toList(), marketedCodes);
    }

    // DIN lookups always show the product, even if cancelled, so customers can check what's on their box
    public List<DrugProductSummary> findByDin(String din) {
        String query = din == null ? "" : din.trim();
        if (!DIN.matcher(query).matches()) {
            throw new IllegalArgumentException("DIN must be up to 8 digits");
        }
        return withStatuses(drugClient.findByDin("0".repeat(8 - query.length()) + query), Set.of());
    }

    public DrugProductDetails getDetails(long drugCode) {
        if (drugCode <= 0) {
            throw new IllegalArgumentException("Drug code must be a positive number");
        }
        List<ActiveIngredient> ingredients = drugClient.getActiveIngredients(drugCode).stream()
                .map(i -> new ActiveIngredient(i.ingredientName(), formatStrength(i)))
                .toList();
        String status = statusOf(drugCode);
        if (ingredients.isEmpty() && status == null) {
            throw new NoSuchElementException("No product found for that drug code");
        }
        return new DrugProductDetails(drugCode, ingredients,
                names(drugClient.getDosageForms(drugCode), DpdDosageForm::name),
                names(drugClient.getRoutes(drugCode), DpdRoute::name),
                names(drugClient.getSchedules(drugCode), DpdSchedule::name),
                status);
    }

    // e.g. "200 MG", or "100 MG / 5 ML" for liquids
    static String formatStrength(DpdActiveIngredient ingredient) {
        String strength = joinNonBlank(ingredient.strength(), ingredient.strengthUnit());
        String per = joinNonBlank(ingredient.dosageValue(), ingredient.dosageUnit());
        return per.isEmpty() ? strength : strength + " / " + per;
    }

    private String statusOf(long drugCode) {
        DpdStatus status = drugClient.getStatus(drugCode);
        return status == null ? null : status.status();
    }

    // Status lookups for non-marketed products run in parallel so a long result list doesn't wait on each call in turn
    private List<DrugProductSummary> withStatuses(List<DpdDrugProduct> products, Set<Long> marketedCodes) {
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<DrugProductSummary>> rows = products.stream()
                    .limit(MAX_RESULTS)
                    .map(p -> CompletableFuture.supplyAsync(() -> toSummary(p,
                            marketedCodes.contains(p.drugCode()) ? MARKETED : statusOf(p.drugCode())), pool))
                    .toList();
            return rows.stream().map(CompletableFuture::join).toList();
        } catch (CompletionException e) {
            throw e.getCause() instanceof RuntimeException cause ? cause : e;
        }
    }

    private static DrugProductSummary toSummary(DpdDrugProduct p, String status) {
        return new DrugProductSummary(p.drugCode(), p.din(), p.brandName(),
                p.companyName(), p.className(), p.lastUpdateDate(), status);
    }

    private static String joinNonBlank(String... parts) {
        return Stream.of(parts).filter(p -> p != null && !p.isBlank()).map(String::trim).collect(Collectors.joining(" "));
    }

    private static <T> List<String> names(List<T> items, Function<T, String> name) {
        return items.stream().map(name).filter(Objects::nonNull).distinct().toList();
    }
}
