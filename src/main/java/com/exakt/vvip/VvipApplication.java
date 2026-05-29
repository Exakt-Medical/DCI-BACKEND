package com.exakt.vvip;

import com.exakt.vvip.merchantCallback.config.MerchantCallbackProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MerchantCallbackProperties.class)
public class VvipApplication {
    public static void main(String[] args) {
        SpringApplication.run(VvipApplication.class, args);
    }
}