package com.cmpt276.frasercare.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DpdStatus(
        @JsonProperty("status") String status,
        @JsonProperty("history_date") String historyDate) {
}
