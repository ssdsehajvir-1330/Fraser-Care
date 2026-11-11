package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.PharmacyService;
import com.cmpt276.frasercare.model.RecommendedService;
import com.cmpt276.frasercare.model.SelfCheckResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cmpt276.frasercare.service.SelfCheckService.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SelfCheckServiceTest {

    private final PharmacyCatalogService catalog = mock(PharmacyCatalogService.class);
    private final SelfCheckService service = serviceOn("2026-11-15");
    private final SelfCheckService summerService = serviceOn("2026-07-15");

    private SelfCheckService serviceOn(String date) {
        ZoneId vancouver = ZoneId.of("America/Vancouver");
        return new SelfCheckService(catalog, Clock.fixed(Instant.parse(date + "T12:00:00Z"), vancouver));
    }

    @BeforeEach
    void seedCatalogue() {
        when(catalog.getActiveServices()).thenReturn(List.of(
                catalogEntry(1L, FLU_VACCINATION),
                catalogEntry(2L, MEDICATION_REVIEW),
                catalogEntry(3L, BLOOD_PRESSURE_CHECK),
                catalogEntry(5L, GENERAL_CONSULTATION)));
    }

    private PharmacyService catalogEntry(Long id, String name) {
        PharmacyService service = new PharmacyService(name, "", 15);
        service.setServiceId(id);
        return service;
    }

    private List<String> names(SelfCheckResult result) {
        return result.recommendedServices().stream().map(RecommendedService::name).toList();
    }

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
        assertEquals(List.of(BLOOD_PRESSURE_CHECK), names(result));
    }

    @Test
    void followUpAnswersAddServicesWithoutDuplicates() {
        SelfCheckResult result = service.evaluate(answers("vaccine", "yes", "not-sure", "no"));

        assertEquals(List.of(FLU_VACCINATION, MEDICATION_REVIEW, BLOOD_PRESSURE_CHECK), names(result));
    }

    @Test
    void otherReasonFallsBackToGeneralConsultation() {
        SelfCheckResult result = service.evaluate(answers("other", "no", "yes", "yes"));

        assertEquals(List.of(GENERAL_CONSULTATION), names(result));
    }

    @Test
    void recommendationsCarryCatalogueIds() {
        SelfCheckResult result = service.evaluate(answers("vaccine", "yes", "no", "yes"));

        assertEquals(List.of(new RecommendedService(FLU_VACCINATION, 1L), new RecommendedService(MEDICATION_REVIEW, 2L)),
                result.recommendedServices());
    }

    @Test
    void vaccineRequestAfterFluShotSuggestsConsultation() {
        SelfCheckResult result = service.evaluate(answers("vaccine", "no", "yes", "yes"));

        assertEquals(List.of(GENERAL_CONSULTATION), names(result));
    }

    @Test
    void vaccineRequestWhenUnsureAboutFluShotSuggestsFluVaccination() {
        assertEquals(List.of(FLU_VACCINATION), names(service.evaluate(answers("vaccine", "no", "not-sure", "yes"))));
        assertEquals(List.of(FLU_VACCINATION), names(service.evaluate(answers("vaccine", "no", "no", "yes"))));
    }

    @Test
    void serviceMissingFromCatalogueHasNoId() {
        SelfCheckResult result = service.evaluate(answers("minor-ailment", "no", "yes", "yes"));

        assertEquals(List.of(new RecommendedService(MINOR_AILMENT_CONSULTATION, null)), result.recommendedServices());
    }

    @Test
    void fluSeasonRunsOctoberToApril() {
        assertTrue(serviceOn("2026-10-01").isFluSeason());
        assertTrue(serviceOn("2027-01-20").isFluSeason());
        assertTrue(serviceOn("2027-04-30").isFluSeason());
        assertFalse(serviceOn("2027-05-01").isFluSeason());
        assertFalse(serviceOn("2026-09-30").isFluSeason());
    }

    @Test
    void fluShotQuestionIsOnlyAskedInSeason() {
        assertTrue(service.getQuestions().stream().anyMatch(q -> q.id().equals("flu-shot")));
        assertTrue(summerService.getQuestions().stream().noneMatch(q -> q.id().equals("flu-shot")));
    }

    @Test
    void offSeasonNeverSuggestsFluShot() {
        Map<String, String> answers = answers("blood-pressure", "no", "no", "yes");
        answers.remove("flu-shot");

        SelfCheckResult result = summerService.evaluate(answers);

        assertEquals(List.of(BLOOD_PRESSURE_CHECK), names(result));
        assertNull(result.note());
    }

    @Test
    void offSeasonVaccineRequestGoesToPharmacistWithNote() {
        Map<String, String> answers = answers("vaccine", "no", "no", "yes");
        answers.remove("flu-shot");

        SelfCheckResult result = summerService.evaluate(answers);

        assertEquals(List.of(GENERAL_CONSULTATION), names(result));
        assertEquals(FLU_OFF_SEASON_NOTE, result.note());
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
