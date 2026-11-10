package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.PharmacyService;
import com.cmpt276.frasercare.model.RecommendedService;
import com.cmpt276.frasercare.model.SelfCheckOption;
import com.cmpt276.frasercare.model.SelfCheckQuestion;
import com.cmpt276.frasercare.model.SelfCheckResult;
import org.springframework.stereotype.Service;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SelfCheckService {

    static final String FLU_VACCINATION = "Flu Vaccination";
    static final String MEDICATION_REVIEW = "Medication Review";
    static final String BLOOD_PRESSURE_CHECK = "Blood Pressure Check";
    static final String MINOR_AILMENT_CONSULTATION = "Minor Ailment Consultation";
    static final String GENERAL_CONSULTATION = "General Pharmacist Consultation";

    private static final SelfCheckOption YES = new SelfCheckOption("yes", "Yes");
    private static final SelfCheckOption NO = new SelfCheckOption("no", "No");
    private static final SelfCheckOption NOT_SURE = new SelfCheckOption("not-sure", "Not sure");

    private static final List<SelfCheckQuestion> QUESTIONS = List.of(
            new SelfCheckQuestion("urgent",
                    "Are you experiencing any of the following right now: chest pain, trouble breathing, "
                            + "severe bleeding, sudden weakness or confusion, or a severe allergic reaction?",
                    List.of(YES, NO)),
            new SelfCheckQuestion("reason", "What brings you to the pharmacy today?", List.of(
                    new SelfCheckOption("vaccine", "I want a vaccine (e.g. flu shot)"),
                    new SelfCheckOption("medications", "I have questions about my medications"),
                    new SelfCheckOption("blood-pressure", "I want to check my blood pressure"),
                    new SelfCheckOption("minor-ailment", "A minor health concern (e.g. allergies, cold sore, heartburn)"),
                    new SelfCheckOption("other", "Something else"))),
            new SelfCheckQuestion("multiple-meds",
                    "Do you regularly take three or more prescription medications?", List.of(YES, NO)),
            new SelfCheckQuestion("flu-shot",
                    "Have you had a flu shot this season?", List.of(YES, NO, NOT_SURE)),
            new SelfCheckQuestion("bp-checked",
                    "Have you had your blood pressure checked in the last 12 months?", List.of(YES, NO, NOT_SURE)));

    private final PharmacyCatalogService catalogService;

    public SelfCheckService(PharmacyCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public List<SelfCheckQuestion> getQuestions() {
        return QUESTIONS;
    }

    public SelfCheckResult evaluate(Map<String, String> answers) {
        if (answers == null) {
            throw new IllegalArgumentException("Answers are required");
        }
        requireValidAnswer(QUESTIONS.get(0), answers);
        if ("yes".equals(answers.get("urgent"))) {
            return SelfCheckResult.urgentCare();
        }
        QUESTIONS.forEach(question -> requireValidAnswer(question, answers));

        Set<String> services = new LinkedHashSet<>();
        services.add(switch (answers.get("reason")) {
            case "vaccine" -> FLU_VACCINATION;
            case "medications" -> MEDICATION_REVIEW;
            case "blood-pressure" -> BLOOD_PRESSURE_CHECK;
            case "minor-ailment" -> MINOR_AILMENT_CONSULTATION;
            default -> GENERAL_CONSULTATION;
        });
        if ("yes".equals(answers.get("multiple-meds"))) {
            services.add(MEDICATION_REVIEW);
        }
        if (!"yes".equals(answers.get("flu-shot"))) {
            services.add(FLU_VACCINATION);
        }
        if (!"yes".equals(answers.get("bp-checked"))) {
            services.add(BLOOD_PRESSURE_CHECK);
        }
        return SelfCheckResult.recommend(withCatalogueIds(services));
    }

    private List<RecommendedService> withCatalogueIds(Set<String> names) {
        Map<String, Long> ids = catalogService.getActiveServices().stream()
                .collect(Collectors.toMap(PharmacyService::getName, PharmacyService::getServiceId));
        return names.stream().map(name -> new RecommendedService(name, ids.get(name))).toList();
    }

    private void requireValidAnswer(SelfCheckQuestion question, Map<String, String> answers) {
        if (!question.accepts(answers.get(question.id()))) {
            throw new IllegalArgumentException("Missing or invalid answer for question: " + question.id());
        }
    }
}
