package com.cmpt276.frasercare.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DpdSchedule(@JsonProperty("schedule_name") String name) {
}
