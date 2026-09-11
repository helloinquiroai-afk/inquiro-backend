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

        try {
            String answer = aiService.answerBusinessQuestion(customerQuestion, businessProfile);
            return answer == null || answer.isBlank() ? com.inquiro.ai.BusinessQuestionPrompt.MISSING_INFORMATION : answer;
        } catch (RuntimeException exception) {
            return "I'm sorry, I couldn't check that information right now. Please contact the business or try again shortly.";
        }
    }
}
