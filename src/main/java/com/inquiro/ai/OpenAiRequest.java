package com.inquiro.ai;

import java.util.List;

public record OpenAiRequest(
        String model,
        List<Message> messages
) {
}
