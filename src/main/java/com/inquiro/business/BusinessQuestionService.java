package com.inquiro.business;

import com.inquiro.ai.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessQuestionService {

    private final AiService aiService;

    public String answer(
            String customerQuestion,
            BusinessProfile businessProfile) {

        return aiService.answerBusinessQuestion(
                customerQuestion,
                businessProfile
        );
    }
}
