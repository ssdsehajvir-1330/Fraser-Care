package com.cmpt276.frasercare.model;

import java.util.List;

public record DrugProductDetails(
        long drugCode,
        List<ActiveIngredient> activeIngredients,
        List<String> dosageForms,
        List<String> routes,
        List<String> schedules,
        String status) {
}
