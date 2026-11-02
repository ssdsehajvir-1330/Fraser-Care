package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.SelfCheckResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cmpt276.frasercare.service.SelfCheckService.*;
import static org.junit.jupiter.api.Assertions.*;

class SelfCheckServiceTest {

    private final SelfCheckService service = new SelfCheckService();

    private Map<String, String> answers(String reason, String meds, String flu, String bp) {
        Map<String, String> answers = new HashMap<>();
        answers.put("urgent", "no");
        answers.put("reason", reason);
        answers.put("multiple-meds", meds);
        answers.put("flu-shot", flu);
        answers.put("bp-checked", bp);
        return answers;
    }

    @Test
    void urgentAnswerStopsAndRecommendsNothing() {
        SelfCheckResult result = service.evaluate(Map.of("urgent", "yes"));

        assertTrue(result.urgent());
        assertTrue(result.recommendedServices().isEmpty());
        assertTrue(result.message().contains("9-1-1"));
    }

    @Test
    void primaryReasonComesFirst() {
        SelfCheckResult result = service.evaluate(answers("blood-pressure", "no", "yes", "no"));

        assertFalse(result.urgent());
        assertEquals(List.of(BLOOD_PRESSURE_CHECK), result.recommendedServices());
    }

    @Test
    void followUpAnswersAddServicesWithoutDuplicates() {
        SelfCheckResult result = service.evaluate(answers("vaccine", "yes", "not-sure", "no"));

        assertEquals(List.of(FLU_VACCINATION, MEDICATION_REVIEW, BLOOD_PRESSURE_CHECK), result.recommendedServices());
    }

    @Test
    void otherReasonFallsBackToGeneralConsultation() {
        SelfCheckResult result = service.evaluate(answers("other", "no", "yes", "yes"));

        assertEquals(List.of(GENERAL_CONSULTATION), result.recommendedServices());
    }

    @Test
    void everyResultHasDisclaimer() {
        assertEquals(SelfCheckResult.DISCLAIMER, service.evaluate(Map.of("urgent", "yes")).disclaimer());
        assertEquals(SelfCheckResult.DISCLAIMER, service.evaluate(answers("medications", "no", "yes", "yes")).disclaimer());
    }

    @Test
    void missingAnswerIsRejected() {
        Map<String, String> answers = answers("vaccine", "no", "yes", "yes");
        answers.remove("bp-checked");

        assertThrows(IllegalArgumentException.class, () -> service.evaluate(answers));
    }

    @Test
    void invalidOptionIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.evaluate(answers("surgery", "no", "yes", "yes")));
        assertThrows(IllegalArgumentException.class, () -> service.evaluate(Map.of("urgent", "maybe")));
        assertThrows(IllegalArgumentException.class, () -> service.evaluate(null));
    }
}
