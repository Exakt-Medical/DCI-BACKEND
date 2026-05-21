package com.exakt.vvip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class VVSRestTemplateConfig {

    private final VVSApiProperties props;

    public VVSRestTemplateConfig(VVSApiProperties props) {
        this.props = props;
    }

    @Bean("vvsRestTemplate")
    public RestTemplate vvsRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeout());
        factory.setReadTimeout(props.getReadTimeout());
        return new RestTemplate(factory);
    }
}