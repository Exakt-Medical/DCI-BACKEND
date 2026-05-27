package com.exakt.vvip.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "tlpe.api")
public class TlpeApiProperties {
    private String payment;
    private String authKey;
    private int connectTimeout = 5000;
    private int readTimeout = 10000;
}
