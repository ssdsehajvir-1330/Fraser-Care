package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.SelfCheckOption;
import com.cmpt276.frasercare.model.SelfCheckQuestion;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SelfCheckService {

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

    public List<SelfCheckQuestion> getQuestions() {
        return QUESTIONS;
    }
}
