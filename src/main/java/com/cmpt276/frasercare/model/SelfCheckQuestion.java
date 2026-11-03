package com.cmpt276.frasercare.model;

import java.util.List;

public record SelfCheckQuestion(String id, String text, List<SelfCheckOption> options) {

    public boolean accepts(String answer) {
        return options.stream().anyMatch(option -> option.value().equals(answer));
    }
}
