package com.inquiro.controller;

import com.inquiro.communication.whatsapp.WhatsAppSender;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final WhatsAppSender whatsAppSender;

    @RequestMapping("test")
    public String test() {

        System.out.println("Entered TestController");

        whatsAppSender.send(
                "94712148907",
                "Hello from Inquiro!"
        );

        System.out.println("Returned from WhatsAppSender");

        return "OK";
    }
}
