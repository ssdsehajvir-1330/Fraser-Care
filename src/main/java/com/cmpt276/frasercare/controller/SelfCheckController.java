package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.model.SelfCheckQuestion;
import com.cmpt276.frasercare.service.SelfCheckService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/self-check")
public class SelfCheckController {

    private final SelfCheckService selfCheckService;

    public SelfCheckController(SelfCheckService selfCheckService) {
        this.selfCheckService = selfCheckService;
    }

    @GetMapping("/questions")
    public List<SelfCheckQuestion> getQuestions() {
        return selfCheckService.getQuestions();
    }
}
