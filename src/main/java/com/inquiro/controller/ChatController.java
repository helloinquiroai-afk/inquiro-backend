package com.inquiro.controller;

import com.inquiro.ai.ChatRequest;
import com.inquiro.inquiry.InquiryOrchestrator;
import com.inquiro.inquiry.InquiryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final InquiryOrchestrator inquiryOrchestrator;

    @PostMapping
    public InquiryResponse chat(
            @RequestBody ChatRequest request) {

        return inquiryOrchestrator.process(
                request.message()
        );
    }
}