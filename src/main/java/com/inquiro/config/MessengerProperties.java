package com.inquiro.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "messenger")
public class MessengerProperties {
    private String verifyToken = "";
    private String pageAccessToken = "";
    private String pageId = "";
    private String appSecret = "";
    private String graphApiVersion = "v26.0";
    private int connectTimeoutSeconds = 5;
    private int readTimeoutSeconds = 20;
    private int maxSendAttempts = 5;
    private int maxPayloadBytes = 1048576;
}