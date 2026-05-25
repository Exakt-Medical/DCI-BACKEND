package com.exakt.vvip.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "vvs.api")
public class VVSApiProperties {
    private String baseUrl;
    private String providerCode;
    private String apiKey;
    private int connectTimeout = 5000;
    private int readTimeout    = 10000;
    private String pfxPath;
    private String pfxPassword;
}