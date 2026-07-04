package com.inquiro.communication.whatsapp;

import com.inquiro.communication.common.ChannelAdapter;
import com.inquiro.communication.common.IncomingMessage;
import com.inquiro.conversation.ConversationService;
import com.inquiro.inquiry.InquiryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WhatsAppAdapter implements ChannelAdapter {

    private final ConversationService conversationService;
    private final WhatsAppSender whatsAppSender;

    @Override
    public void receive(IncomingMessage message) {

        InquiryResponse response =
                conversationService.process(
                        message.sessionId(),
                        message.message()
                );

        whatsAppSender.send(
                message.senderId(),
                response.reply()
        );
    }
}
