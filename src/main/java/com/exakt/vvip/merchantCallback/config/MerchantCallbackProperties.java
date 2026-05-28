package com.exakt.vvip.merchantCallback.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "merchant-callback")
public class MerchantCallbackProperties {

    private String tlpeApiBaseUrl;
    private String integratorToken;
    private String billeroApiBaseUrl;
    private String billeroToken;
    private Http http = new Http();

    @Getter
    @Setter
    public static class Http {
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 10000;
    }
}