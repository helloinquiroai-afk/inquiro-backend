package com.inquiro.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppProperties {

    private String accessToken;

    private String phoneNumberId;

    private String verifyToken;

}
