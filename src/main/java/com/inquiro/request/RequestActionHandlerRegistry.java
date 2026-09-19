package com.inquiro.request;

import com.inquiro.business.BusinessAccount;
import com.inquiro.inquiry.InquiryResponse;
import com.inquiro.inquiry.InquiryResult;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class RequestActionHandlerRegistry {

    private final Map<RequestActionType, RequestActionHandler> handlers =
            new EnumMap<>(RequestActionType.class);

    public RequestActionHandlerRegistry(List<RequestActionHandler> actionHandlers) {
        for (RequestActionHandler handler : actionHandlers) {
            RequestActionHandler previous = handlers.put(handler.actionType(), handler);
            if (previous != null) {
                throw new IllegalStateException(
                        "Multiple request action handlers configured for " + handler.actionType());
            }
        }
    }

    public RequestActionHandler handlerFor(RequestDefinition definition) {
        if (definition == null || definition.actionType() == null) {
            return null;
        }
        return handlers.get(definition.actionType());
    }

    public InquiryResponse handle(
            RequestDefinition definition,
            BusinessAccount businessAccount,
            String sessionId,
            InquiryResult inquiry) {

        RequestActionHandler handler = handlerFor(definition);
        if (handler == null) {
            throw new IllegalStateException(
                    "No request action handler configured for " + definition.actionType());
        }
        return handler.handle(businessAccount, sessionId, inquiry);
    }

    public List<String> actionRequiredFields(RequestDefinition definition) {
        if (definition == null || definition.actionType() != RequestActionType.BOOKING) {
            return List.of();
        }
        if (definition.actionRequiredSlots() != null && !definition.actionRequiredSlots().isEmpty()) {
            return definition.actionRequiredSlots();
        }
        return List.of();
    }
}
