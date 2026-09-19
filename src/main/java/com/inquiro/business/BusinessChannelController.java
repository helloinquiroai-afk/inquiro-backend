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
    private final ChannelCredentialService channelCredentials;


    /*
     * =========================================================
     * GET ALL CHANNELS FOR A BUSINESS
     * =========================================================
     */

    @GetMapping
    public List<BusinessChannel> getChannels(
            @PathVariable String businessId) {

        tenantAuthorization.requireBusinessWriteAccess(businessId);

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

        tenantAuthorization.requireBusinessWriteAccess(businessId);

        validateBusinessExists(
                businessId
        );

        validateRequest(
                request
        );

        BusinessChannel existing =
                businessChannelRepository
                        .findByBusinessIdAndTypeAndExternalId(
                                businessId,
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


    @PutMapping("/{channelId}")
    public BusinessChannel updateChannel(
            @PathVariable String businessId,
            @PathVariable String channelId,
            @RequestBody UpdateBusinessChannelRequest request) {

        tenantAuthorization.requireBusinessAccess(businessId);
        validateBusinessExists(businessId);

        if (channelId == null || channelId.isBlank()) {
            throw new IllegalArgumentException("Channel ID cannot be blank");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        BusinessChannel existing = businessChannelRepository
                .findByBusinessId(businessId)
                .stream()
                .filter(channel -> channel != null && channelId.equals(channel.channelId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Channel not found: " + channelId));

        BusinessChannel updated = new BusinessChannel(
                existing.channelId(),
                existing.businessId(),
                existing.type(),
                existing.externalId(),
                request.enabled() == null
                        ? existing.enabled()
                        : request.enabled()
        );

        businessChannelRepository.save(updated);
        return updated;
    }

    public record UpdateBusinessChannelRequest(Boolean enabled) {}

    @PutMapping("/{channelId}/credentials")
    public CredentialStatus replaceCredentials(
            @PathVariable String businessId,
            @PathVariable String channelId,
            @RequestBody CredentialRequest request) {
        tenantAuthorization.requireBusinessWriteAccess(businessId);
        validateBusinessExists(businessId);
        BusinessChannel channel = businessChannelRepository.findByBusinessId(businessId).stream()
                .filter(item -> item != null && channelId.equals(item.channelId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));
        if (channel.type() != BusinessChannelType.MESSENGER) {
            throw new IllegalArgumentException("Credentials are currently supported for Messenger channels only");
        }
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        channelCredentials.save(channelId, request.pageAccessToken(), request.appSecret(), request.verifyToken());
        return new CredentialStatus(true);
    }

    @DeleteMapping("/{channelId}/credentials")
    public CredentialStatus revokeCredentials(
            @PathVariable String businessId,
            @PathVariable String channelId) {
        tenantAuthorization.requireBusinessWriteAccess(businessId);
        validateBusinessExists(businessId);
        BusinessChannel channel = businessChannelRepository.findByBusinessId(businessId).stream()
                .filter(item -> item != null && channelId.equals(item.channelId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));
        channelCredentials.delete(channelId);
        return new CredentialStatus(false);
    }

    @GetMapping("/{channelId}/credentials")
    public CredentialStatus credentialStatus(
            @PathVariable String businessId,
            @PathVariable String channelId) {
        tenantAuthorization.requireBusinessAccess(businessId);
        validateBusinessExists(businessId);
        if (businessChannelRepository.findByBusinessId(businessId).stream()
                .noneMatch(item -> item != null && channelId.equals(item.channelId()))) {
            throw new IllegalArgumentException("Channel not found: " + channelId);
        }
        return new CredentialStatus(channelCredentials.hasCredentials(channelId));
    }

    public record CredentialRequest(String pageAccessToken, String appSecret, String verifyToken) {}
    public record CredentialStatus(boolean configured) {}


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
