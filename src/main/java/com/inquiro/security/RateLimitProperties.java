package com.inquiro.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inquiro.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private int requestsPerMinute = 60;
    private int authenticatedRequestsPerMinute = 120;
    private int publicAiRequestsPerMinute = 20;
    private int authRequestsPerMinute = 10;
    private int webhookRequestsPerMinute = 120;
    private int maxRequestBodyBytes = 1_048_576;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getRequestsPerMinute() { return requestsPerMinute; }
    public void setRequestsPerMinute(int v) { this.requestsPerMinute = v; }
    public int getAuthenticatedRequestsPerMinute() { return authenticatedRequestsPerMinute; }
    public void setAuthenticatedRequestsPerMinute(int v) { this.authenticatedRequestsPerMinute = v; }
    public int getPublicAiRequestsPerMinute() { return publicAiRequestsPerMinute; }
    public void setPublicAiRequestsPerMinute(int v) { this.publicAiRequestsPerMinute = v; }
    public int getAuthRequestsPerMinute() { return authRequestsPerMinute; }
    public void setAuthRequestsPerMinute(int v) { this.authRequestsPerMinute = v; }
    public int getWebhookRequestsPerMinute() { return webhookRequestsPerMinute; }
    public void setWebhookRequestsPerMinute(int v) { this.webhookRequestsPerMinute = v; }
    public int getMaxRequestBodyBytes() { return maxRequestBodyBytes; }
    public void setMaxRequestBodyBytes(int v) { this.maxRequestBodyBytes = v; }
}
