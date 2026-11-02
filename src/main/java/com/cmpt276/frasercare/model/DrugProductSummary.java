package com.cmpt276.frasercare.model;

public record DrugProductSummary(
        Long drugCode,
        String din,
        String brandName,
        String company,
        String productClass,
        String lastUpdated) {
}
