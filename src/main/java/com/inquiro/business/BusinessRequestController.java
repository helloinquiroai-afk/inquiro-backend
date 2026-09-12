package com.inquiro.business;

import com.inquiro.auth.TenantAuthorizationService;
import com.inquiro.communication.messenger.CustomerNotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/business/requests")
@RequiredArgsConstructor
public class BusinessRequestController {

    private final BusinessRequestRepository businessRequestRepository;

    private final BusinessRequestService businessRequestService;

    private final CustomerNotificationService notifications;

    private final TenantAuthorizationService tenantAuthorization;


    @GetMapping
    public ResponseEntity<List<BusinessRequest>> getRequests(
            @RequestParam String businessId) {

        tenantAuthorization.requireBusinessAccess(businessId);

        return ResponseEntity.ok(
                businessRequestRepository.findByBusinessId(
                        businessId
                )
        );
    }


    @GetMapping("/pending")
    public ResponseEntity<List<BusinessRequest>> getPendingRequests(
            @RequestParam String businessId) {

        tenantAuthorization.requireBusinessAccess(businessId);

        return ResponseEntity.ok(
                businessRequestRepository.findPendingByBusinessId(
                        businessId
                )
        );
    }


    @PostMapping("/{requestId}/confirm")
    public ResponseEntity<BusinessRequest> confirmRequest(
            @PathVariable String requestId) {

        BusinessRequest existingRequest =
                findRequest(requestId);

        tenantAuthorization.requireBusinessAccess(
                existingRequest.businessId()
        );

        BusinessRequest request =
                businessRequestService.confirm(requestId);

        notifications.sendConfirmation(request);

        return ResponseEntity.ok(request);
    }


    @PostMapping("/{requestId}/reject")
    public ResponseEntity<BusinessRequest> rejectRequest(
            @PathVariable String requestId) {

        BusinessRequest existingRequest =
                findRequest(requestId);

        tenantAuthorization.requireBusinessAccess(
                existingRequest.businessId()
        );

        BusinessRequest request =
                businessRequestService.reject(requestId);

        notifications.sendRejection(request);

        return ResponseEntity.ok(request);
    }


    private BusinessRequest findRequest(
            String requestId) {

        BusinessRequest request =
                businessRequestRepository.findByRequestId(
                        requestId
                );

        if (request == null) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Business request not found: "
                            + requestId
            );
        }

        return request;
    }


    private String readableService(
            String service) {

        if (service == null || service.isBlank()) {
            return "business";
        }

        return service
                .toLowerCase()
                .replace('_', ' ');
    }
}