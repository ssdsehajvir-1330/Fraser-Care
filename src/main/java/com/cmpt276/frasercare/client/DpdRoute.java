package com.cmpt276.frasercare.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DpdRoute(@JsonProperty("route_of_administration_name") String name) {
}
