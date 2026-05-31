package com.exakt.vvip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrdersRequest {

    // ── Order row fields (written to `orders` table) ─────────────────────────

    @JsonProperty("company_id")
    private Long companyId;

    @JsonProperty("company_code")
    private String companyCode;

    @JsonProperty("voucher_fee")
    private BigDecimal voucherFee;

    @JsonProperty("voucher_count")
    private Integer voucherCount;

    // ── TLPE payment-link payload ─────────────────────────────────────────────

    private Customer customer;
    private Payment payment;
    private Route route;

    // Hidden from Swagger but still serialized for outbound TLPE requests.
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @Schema(hidden = true)
    private String key;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Customer {
        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        @JsonProperty("billing_address")
        private BillingAddress billingAddress;

        private Contact contact;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BillingAddress {
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

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Contact {
        private String email;
        private String mobile;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Payment {
        private String description;
        private String amount;
        private String currency;

        @JsonProperty("merchant_reference_id")
        private String merchantReferenceId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Route {
        @JsonProperty("callback_url")
        private String callbackUrl;

        @JsonProperty("notify_user")
        private Boolean notifyUser;
    }
}
