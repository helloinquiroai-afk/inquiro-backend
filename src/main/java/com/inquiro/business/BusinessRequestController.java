package com.inquiro.business;

import com.inquiro.communication.messenger.MessengerSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business/requests")
@RequiredArgsConstructor
public class BusinessRequestController {

    private final BusinessRequestStore businessRequestStore;
    private final BusinessRequestService businessRequestService;
    private final MessengerSendService messengerSendService;

    /**
     * Get all requests for a business.
     *
     * GET
     * /api/business/requests?businessId=biz_001
     */
    @GetMapping
    public ResponseEntity<List<BusinessRequest>> getRequests(
            @RequestParam String businessId) {

        return ResponseEntity.ok(
                businessRequestStore.findByBusinessId(businessId)
        );
    }

    /**
     * Get pending requests.
     *
     * GET
     * /api/business/requests/pending?businessId=biz_001
     */
    @GetMapping("/pending")
    public ResponseEntity<List<BusinessRequest>> getPendingRequests(
            @RequestParam String businessId) {

        return ResponseEntity.ok(
                businessRequestStore.findPendingByBusinessId(businessId)
        );
    }

    /**
     * Confirm a request.
     *
     * POST
     * /api/business/requests/{requestId}/confirm
     */
    @PostMapping("/{requestId}/confirm")
    public ResponseEntity<BusinessRequest> confirmRequest(
            @PathVariable String requestId) {

        BusinessRequest request =
                businessRequestService.confirm(requestId);

        // Notify customer through Messenger
        messengerSendService.sendText(
                request.customerId(),
                "Your "
                        + readableService(request.service())
                        + " request has been confirmed by the business."
        );

        return ResponseEntity.ok(request);
    }

    /**
     * Reject a request.
     *
     * POST
     * /api/business/requests/{requestId}/reject
     */
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<BusinessRequest> rejectRequest(
            @PathVariable String requestId) {

        BusinessRequest request =
                businessRequestService.reject(requestId);

        // Notify customer through Messenger
        messengerSendService.sendText(
                request.customerId(),
                "Unfortunately, your "
                        + readableService(request.service())
                        + " request could not be confirmed by the business."
        );

        return ResponseEntity.ok(request);
    }

    private String readableService(
            String service) {

        if (service == null || service.isBlank()) {
            return "business";
        }

        return service.toLowerCase()
                .replace('_', ' ');
    }
}
