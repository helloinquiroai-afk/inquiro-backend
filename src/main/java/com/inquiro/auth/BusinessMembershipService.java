package com.inquiro.auth;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BusinessMembershipService {
    private final BusinessMembershipJpaRepository memberships;
    private final UserAccountJpaRepository users;
    private final TenantAuthorizationService tenantAuthorization;

    @Transactional(readOnly = true)
    public List<MembershipResponse> list(String businessId) {
        tenantAuthorization.requireOwner(businessId);
        return memberships.findByBusinessId(businessId).stream()
                .map(membership -> new MembershipResponse(membership.getUserId(), membership.getRole(),
                        membership.getCreatedAt()))
                .toList();
    }

    @Transactional
    public MembershipResponse addAdmin(String businessId, String email) {
        tenantAuthorization.requireOwner(businessId);
        String normalizedEmail = AuthService.normalizeEmailForLookup(email);
        UserAccountEntity user = users.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User account is disabled");
        }
        if (memberships.findByUserIdAndBusinessId(user.getUserId(), businessId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this business");
        }
        BusinessMembershipEntity membership = memberships.save(new BusinessMembershipEntity(
                "mem_" + UUID.randomUUID(), user.getUserId(), businessId, BusinessMembershipRole.ADMIN, Instant.now()));
        return new MembershipResponse(membership.getUserId(), membership.getRole(), membership.getCreatedAt());
    }

    public record MembershipResponse(String userId, BusinessMembershipRole role, Instant createdAt) { }
}
