package com.inquiro.channel;

import com.inquiro.business.BusinessChannelType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelMessageController {

    private final ChannelMessageRouter router;

    @PostMapping("/messages")
    public ChannelMessageResult receive(@RequestBody ChannelMessageRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request cannot be null");
        }
        try {
            return router.route(new ChannelMessage(
                    request.channelType(),
                    request.externalChannelId(),
                    request.customerId(),
                    request.text()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    public record ChannelMessageRequest(
            BusinessChannelType channelType,
            String externalChannelId,
            String customerId,
            String text
    ) {
    }
}
