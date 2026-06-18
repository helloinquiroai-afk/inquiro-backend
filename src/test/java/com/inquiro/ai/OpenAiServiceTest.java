package com.inquiro.ai;

import com.inquiro.request.SlotFillingEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class OpenAiServiceTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private SlotFillingEngine slotFillingEngine;

    @Test
    void shouldFindMissingSlots() {

        RequestAnalysis analysis =
                aiService.analyzeRequest(
                        "need a room in Paris"
                );

        List<String> missing =
                slotFillingEngine.findMissingSlots(
                        analysis
                );

        System.out.println();
        System.out.println("Analysis = " + analysis);
        System.out.println("Missing = " + missing);
        System.out.println();
    }
}