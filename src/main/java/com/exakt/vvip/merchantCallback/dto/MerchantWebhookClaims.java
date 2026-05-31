package com.exakt.vvip.merchantCallback.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MerchantWebhookClaims {

    private String iss;
    private String sub;
    private Object aud;
    private Long exp;
    private Long iat;
    private String jti;
    private Data data;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {

        private Customer customer;
        private Payment payment;
        private Result result;
        @JsonProperty("custom_parameters")
        private CustomParameters customParameters;
        @JsonProperty("transaction_id")
        private String transactionId;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Customer {

        @JsonProperty("first_name")
        private String firstName;
        @JsonProperty("last_name")
        private String lastName;
        @JsonProperty("billing_address")
        private Address billingAddress;
        @JsonProperty("shipping_address")
        private Address shippingAddress;
        private Contact contact;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {

        private String line1;
        private String line2;
        @JsonProperty("city_municipality")
        private String cityMunicipality;
        private String zip;
        @JsonProperty("state_province_region")
        private String stateProvinceRegion;
        @JsonProperty("country_code")
        private String countryCode;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contact {

        private String email;
        private String mobile;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payment {

        private String description;
        private BigDecimal amount;
        private String currency;
        private String option;
        @JsonProperty("merchant_reference_id")
        private String merchantReferenceId;
        @JsonProperty("processor_reference_id")
        private String processorReferenceId;
        @JsonProperty("other_references")
        private List<String> otherReferences;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {

        @JsonProperty("statusCode")
        private String statusCode;
        private String message;
        private String timestamp;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomParameters {

        @JsonProperty("custom_param1")
        private String customParam1;
        @JsonProperty("custom_param2")
        private String customParam2;
        @JsonProperty("custom_param3")
        private String customParam3;
        @JsonProperty("original_amount")
        private BigDecimal originalAmount;
        @JsonProperty("company_code")
        private String companyCode;
        @JsonProperty("company_name")
        private String companyName;
        @JsonProperty("voucher_count")
        private Integer voucherCount;
        @JsonProperty("voucher_fee")
        private BigDecimal voucherFee;
        @JsonProperty("epl_add_on_fee")
        private BigDecimal eplAddOnFee;
    }
}