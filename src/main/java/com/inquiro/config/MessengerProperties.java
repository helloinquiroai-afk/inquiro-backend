package com.inquiro.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "messenger")
public class MessengerProperties {

    private String verifyToken;

    private String pageAccessToken;
}
