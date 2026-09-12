package com.inquiro.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Owner-only administration. Invite delivery and ownership transfer are deliberately separate future workflows. */
@RestController
@RequestMapping("/api/business/accounts/{businessId}/members")
@RequiredArgsConstructor
public class BusinessMembershipController {
    private final BusinessMembershipService memberships;

    @GetMapping
    public List<BusinessMembershipService.MembershipResponse> list(@PathVariable String businessId) {
        return memberships.list(businessId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessMembershipService.MembershipResponse addAdmin(@PathVariable String businessId,
                                                                  @Valid @RequestBody AddAdminRequest request) {
        return memberships.addAdmin(businessId, request.email());
    }

    public record AddAdminRequest(@NotBlank @Email String email) { }
}
