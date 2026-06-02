package com.dci.clearance.merchantCallback.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MerchantCallbackRestTemplateConfig {

    @Bean("merchantCallbackRestTemplate")
    public RestTemplate merchantCallbackRestTemplate(MerchantCallbackProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getHttp().getConnectTimeoutMs());
        factory.setReadTimeout(properties.getHttp().getReadTimeoutMs());
        return new RestTemplate(factory);
    }
}