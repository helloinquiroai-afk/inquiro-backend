package com.inquiro.auth;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantAuthorizationService {

    private final BusinessMembershipJpaRepository memberships;

    public BusinessMembershipRole requireBusinessAccess(String businessId) {

        if (isOperator()) {
            return BusinessMembershipRole.OWNER;
        }

        String userId = requireUser().userId();

        return memberships.findByUserIdAndBusinessId(userId, businessId)
                .map(BusinessMembershipEntity::getRole)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "You do not have access to this business"));
    }

    public void createOwnerMembershipForCurrentUser(String businessId) {

        if (isOperator()) {
            return;
        }

        AuthenticatedUser user = requireUser();

        if (memberships.findByUserIdAndBusinessId(
                user.userId(), businessId).isPresent()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Business membership already exists");
        }

        memberships.save(
                new BusinessMembershipEntity(
                        "mem_" + UUID.randomUUID(),
                        user.userId(),
                        businessId,
                        BusinessMembershipRole.OWNER,
                        Instant.now()));
    }

    public void requireOwner(String businessId) {

        if (isOperator()) {
            return;
        }

        if (requireBusinessAccess(businessId)
                != BusinessMembershipRole.OWNER) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Owner access is required");
        }
    }

    public boolean isOperator() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_OPERATOR".equals(
                                authority.getAuthority()));
    }

    private AuthenticatedUser requireUser() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal()
                instanceof AuthenticatedUser user) {

            return user;
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authentication required");
    }
}