package com.cmpt276.frasercare.model;

import java.util.Map;

public record SelfCheckRequest(Map<String, String> answers) {
}
