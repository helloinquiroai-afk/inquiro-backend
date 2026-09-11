package com.inquiro.knowledge;

import com.inquiro.InquiroBackendApplication;
import com.inquiro.business.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeRestartTest {
    @TempDir Path directory;

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(InquiroBackendApplication.class).web(WebApplicationType.NONE).run(
                "--spring.datasource.url=jdbc:h2:file:" + directory.resolve("knowledge").toString().replace('\\', '/'),
                "--spring.jpa.hibernate.ddl-auto=update",
                "--messenger.worker-enabled=false", "--inquiro.seed-default-business=false",
                "--spring.jmx.enabled=false", "--spring.main.banner-mode=off", "--logging.level.root=ERROR");
    }

    @Test void knowledgeAndApprovedFaqSurviveApplicationRestart() {
        try (var first = start()) {
            first.getBean(BusinessAccountRepository.class).save(new BusinessAccount("restart-biz", "Paris Hotel", KnowledgeFixtures.profile()));
            first.getBean(BusinessKnowledgeService.class).review("restart-biz", BusinessKnowledgeService.Decision.APPROVE,
                    new FaqSuggestion("Can I park?", "Parking is free for guests."));
        }
        try (var second = start()) {
            var reloaded = second.getBean(BusinessKnowledgeService.class).get("restart-biz");
            assertEquals(KnowledgeFixtures.profile().knowledge().facts(), reloaded.facts());
            assertEquals(KnowledgeFixtures.profile().knowledge().boundaries(), reloaded.boundaries());
            assertEquals(2, reloaded.faqs().size());
            assertTrue(reloaded.faqs().contains("Q: Can I park?\nA: Parking is free for guests."));
            assertEquals(reloaded, second.getBean(BusinessKnowledgeStore.class).findByBusinessId("restart-biz").knowledge());
        }
    }
}