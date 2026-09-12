package com.inquiro.business;

import lombok.RequiredArgsConstructor;
import com.inquiro.auth.TenantAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/business/accounts/{businessId}/channels")
@RequiredArgsConstructor
public class BusinessChannelController {

    private final BusinessAccountRepository businessAccountRepository;

    private final BusinessChannelRepository businessChannelRepository;
    private final TenantAuthorizationService tenantAuthorization;


    /*
     * =========================================================
     * GET ALL CHANNELS FOR A BUSINESS
     * =========================================================
     */

    @GetMapping
    public List<BusinessChannel> getChannels(
            @PathVariable String businessId) {

        tenantAuthorization.requireBusinessAccess(businessId);

        validateBusinessExists(
                businessId
        );

        return businessChannelRepository
                .findByBusinessId(
                        businessId
                );
    }


    /*
     * =========================================================
     * ADD CHANNEL
     * =========================================================
     */

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessChannel addChannel(
            @PathVariable String businessId,
            @RequestBody CreateBusinessChannelRequest request) {

        tenantAuthorization.requireBusinessAccess(businessId);

        validateBusinessExists(
                businessId
        );

        validateRequest(
                request
        );

        BusinessChannel existing =
                businessChannelRepository
                        .findByTypeAndExternalId(
                                request.type(),
                                request.externalId()
                        );

        if (existing != null) {

            throw new IllegalArgumentException(
                    "Channel is already connected: "
                            + request.type()
                            + " / "
                            + request.externalId()
            );
        }

        BusinessChannel channel =
                new BusinessChannel(
                        "channel_"
                                + UUID.randomUUID(),
                        businessId,
                        request.type(),
                        request.externalId(),
                        request.enabled() == null
                                || request.enabled()
                );

        businessChannelRepository.save(
                channel
        );

        return channel;
    }


    /*
     * =========================================================
     * VALIDATE BUSINESS
     * =========================================================
     */

    private void validateBusinessExists(
            String businessId) {

        if (businessId == null
                || businessId.isBlank()) {

            throw new IllegalArgumentException(
                    "Business ID cannot be null or blank"
            );
        }

        BusinessAccount account =
                businessAccountRepository.findByBusinessId(
                        businessId
                );

        if (account == null) {

            throw new IllegalArgumentException(
                    "Business not found: "
                            + businessId
            );
        }
    }


    /*
     * =========================================================
     * VALIDATE CHANNEL REQUEST
     * =========================================================
     */

    private void validateRequest(
            CreateBusinessChannelRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Request cannot be null"
            );
        }

        if (request.type() == null) {

            throw new IllegalArgumentException(
                    "Channel type is required"
            );
        }

        if (request.externalId() == null
                || request.externalId().isBlank()) {

            throw new IllegalArgumentException(
                    "External ID is required"
            );
        }
    }


    /*
     * =========================================================
     * CREATE CHANNEL REQUEST
     * =========================================================
     */

    public record CreateBusinessChannelRequest(

            BusinessChannelType type,

            String externalId,

            Boolean enabled

    ) {
    }
}
