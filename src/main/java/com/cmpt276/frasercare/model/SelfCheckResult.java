package com.cmpt276.frasercare.model;

import java.util.List;

public record SelfCheckResult(boolean urgent, String message, List<String> recommendedServices, String disclaimer) {

    public static final String DISCLAIMER = "This self-check is not a medical assessment. It only suggests pharmacy "
            + "services you may want to book. For health advice, speak with a pharmacist or call HealthLink BC at 8-1-1.";

    public static SelfCheckResult urgentCare() {
        return new SelfCheckResult(true,
                "Please call 9-1-1 or go to the nearest emergency department now. Do not wait for a pharmacy appointment.",
                List.of(), DISCLAIMER);
    }

    public static SelfCheckResult recommend(List<String> services) {
        return new SelfCheckResult(false, "Based on your answers, these services may be helpful:", services, DISCLAIMER);
    }
}
