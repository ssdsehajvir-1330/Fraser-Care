package com.cmpt276.frasercare.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DpdDrugProduct(
        @JsonProperty("drug_code") Long drugCode,
        @JsonProperty("drug_identification_number") String din,
        @JsonProperty("brand_name") String brandName,
        @JsonProperty("descriptor") String descriptor,
        @JsonProperty("class_name") String className,
        @JsonProperty("number_of_ais") String numberOfActiveIngredients,
        @JsonProperty("company_name") String companyName,
        @JsonProperty("last_update_date") String lastUpdateDate) {
}
