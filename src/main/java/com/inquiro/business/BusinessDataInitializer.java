package com.inquiro.business;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BusinessDataInitializer implements CommandLineRunner {

    private final BusinessAccountRepository businessAccountRepository;
    private final BusinessChannelRepository businessChannelRepository;
    private final BusinessProfileProvider businessProfileProvider;

    @Override
    public void run(String... args) {

        String businessId = "biz_001";

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

        BusinessChannel existingChannel =
                businessChannelRepository
                        .findByTypeAndExternalId(
                                BusinessChannelType.MESSENGER,
                                "1138575329350155"
                        );

        if (existingChannel == null) {

            businessChannelRepository.save(
                    new BusinessChannel(
                            "channel_messenger_001",
                            businessId,
                            BusinessChannelType.MESSENGER,
                            "1138575329350155",
                            true
                    )
            );

            System.out.println(
                    "Created default Messenger channel"
            );
        }
    }
}