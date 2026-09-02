package com.inquiro.controller;

import com.inquiro.ai.AiService;
import com.inquiro.ai.RequestAnalysis;
import com.inquiro.ai.RequestAnalysisValidator;
import com.inquiro.business.BusinessProfile;
import com.inquiro.business.BusinessProfileProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class RequestAnalysisController {

    private final AiService aiService;
    private final RequestAnalysisValidator validator;
    private final BusinessProfileProvider businessProfileProvider;

    /*@PostMapping("/request-analysis")
    public RequestAnalysis analyze(
            @RequestBody Map<String, String> body) {

        return aiService.analyzeRequest(
                body.get("message")
        );
    }*/


    @PostMapping("/request-analysis")
    public Object analyze(
            @RequestBody Map<String, String> body) {

        RequestAnalysis analysis =
                aiService.analyzeRequest(
                        body.get("message"),
                        businessProfileProvider.get()
                );

        if (validator.needsClarification(analysis)) {

            BusinessProfile businessProfile =
                    businessProfileProvider.get();

            return Map.of(
                    "status",
                    "NEEDS_CLARIFICATION",
                    "reply",
                    "I can currently help with "
                            + String.join(
                            " and ",
                            businessProfile.knowledge().services()
                    )
                            + ". How can I help?"
            );
        }

        return analysis;
    }
}
