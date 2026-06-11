package com.dci.clearance;

import com.dci.clearance.merchantCallback.config.MerchantCallbackProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(MerchantCallbackProperties.class)
@EnableScheduling
public class DciClearanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DciClearanceApplication.class, args);
    }
}