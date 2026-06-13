package com.inquiro.ai;

import java.util.List;

public record OpenAiResponse(
        List<Choice> choices
) {
}
