package com.inquiro.ai;

public record ConversationIntentAnalysis(
        String intent,
        Double confidence,
        java.util.List<String> knowledgeQuestions
) {
    public ConversationIntentAnalysis(String intent, Double confidence) {
        this(intent, confidence, java.util.List.of());
    }

    public ConversationIntentAnalysis {
        knowledgeQuestions = RequestAnalysis.questions(knowledgeQuestions);
    }
}
