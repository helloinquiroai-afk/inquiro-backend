package com.inquiro.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FaqSuggestion(
        @NotBlank @Size(max = 500) String question,
        @NotBlank @Size(max = 4000) String answer) {
    public String asApprovedFaq() {
        return "Q: " + question.strip() + "\nA: " + answer.strip();
    }
}
