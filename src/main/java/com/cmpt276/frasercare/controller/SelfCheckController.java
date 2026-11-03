package com.cmpt276.frasercare.controller;

import com.cmpt276.frasercare.model.SelfCheckQuestion;
import com.cmpt276.frasercare.model.SelfCheckRequest;
import com.cmpt276.frasercare.service.SelfCheckService;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody SelfCheckRequest request) {
        try {
            return ResponseEntity.ok(selfCheckService.evaluate(request.answers()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
