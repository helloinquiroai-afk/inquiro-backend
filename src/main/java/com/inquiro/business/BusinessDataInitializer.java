package com.inquiro.business;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.inquiro.config.MessengerProperties;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "inquiro.seed-default-business", havingValue = "true", matchIfMissing = true)
public class BusinessDataInitializer implements CommandLineRunner {

    private final BusinessAccountRepository businessAccountRepository;
    private final BusinessChannelRepository businessChannelRepository;
    private final BusinessProfileProvider businessProfileProvider;
    private final MessengerProperties messengerProperties;

    @Value("${inquiro.default-business-id:biz_001}")
    private String defaultBusinessId;

    @Value("${inquiro.website-channel-id:website-default}")
    private String websiteChannelId;

    @Override
    public void run(String... args) {

        String businessId = defaultBusinessId;

        BusinessAccount existing =
                businessAccountRepository.findByBusinessId(
                        businessId
                );

        if (existing == null) {

            BusinessProfile profile =
                    businessProfileProvider.get();

            businessAccountRepository.save(
                    new BusinessAccount(
                            businessId,
                            profile.businessName(),
                            profile
                    )
            );

            System.out.println(
                    "Created default business: " + businessId
            );
        }

        if (businessChannelRepository.findByTypeAndExternalId(BusinessChannelType.WEBSITE, websiteChannelId) == null) {
            businessChannelRepository.save(new BusinessChannel("channel_website_" + businessId, businessId,
                    BusinessChannelType.WEBSITE, websiteChannelId, true));
        }
        if (messengerProperties.getPageId() == null || messengerProperties.getPageId().isBlank()) return;
        BusinessChannel existingChannel =
                businessChannelRepository
                        .findByTypeAndExternalId(
                                BusinessChannelType.MESSENGER,
                                messengerProperties.getPageId()
                        );

        if (existingChannel == null) {

            businessChannelRepository.save(
                    new BusinessChannel(
                            "channel_messenger_" + messengerProperties.getPageId(),
                            businessId,
                            BusinessChannelType.MESSENGER,
                            messengerProperties.getPageId(),
                            true
                    )
            );

            System.out.println(
                    "Created default Messenger channel"
            );
        }
    }
}
