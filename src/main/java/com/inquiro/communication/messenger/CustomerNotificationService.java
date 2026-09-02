package com.inquiro.communication.messenger;

import com.inquiro.business.BusinessRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerNotificationService {

    private final MessengerSendService messengerSendService;

    public void sendConfirmation(
            BusinessRequest request) {

        String message =
                "Your "
                        + formatService(request.service())
                        + " request has been confirmed. "
                        + "Thank you for choosing us.";

        messengerSendService.sendText(
                request.customerId(),
                message
        );
    }

    public void sendRejection(
            BusinessRequest request) {

        String message =
                "Unfortunately, your "
                        + formatService(request.service())
                        + " request could not be confirmed. "
                        + "Please contact us if you would like an alternative.";

        messengerSendService.sendText(
                request.customerId(),
                message
        );
    }

    private String formatService(
            String service) {

        if (service == null || service.isBlank()) {
            return "service";
        }

        return service.toLowerCase()
                .replace("_", " ");
    }
}
