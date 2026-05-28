package com.exakt.vvip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TlpeRestTemplateConfig {

    private final TlpeApiProperties props;

    public TlpeRestTemplateConfig(TlpeApiProperties props) {
        this.props = props;
    }

    @Bean("tlpeRestTemplate")
    public RestTemplate tlpeRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeout());
        factory.setReadTimeout(props.getReadTimeout());
        return new RestTemplate(factory);
    }
}
