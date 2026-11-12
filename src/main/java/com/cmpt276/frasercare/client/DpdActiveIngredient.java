package com.cmpt276.frasercare.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DpdActiveIngredient(
        @JsonProperty("ingredient_name") String ingredientName,
        @JsonProperty("strength") String strength,
        @JsonProperty("strength_unit") String strengthUnit,
        @JsonProperty("dosage_value") String dosageValue,
        @JsonProperty("dosage_unit") String dosageUnit) {
}
