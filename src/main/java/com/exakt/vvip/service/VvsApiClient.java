package com.exakt.vvip.service;

import com.exakt.vvip.config.VVSApiProperties;
import com.exakt.vvip.dto.VvsConfirmRequest;
import com.exakt.vvip.dto.VvsEngineChassisRequest;
import com.exakt.vvip.dto.VvsMvPlateRequest;
import com.exakt.vvip.dto.VvsVehicleData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class VvsApiClient {

    private final RestTemplate     restTemplate;
    private final VVSApiProperties props;
    private final ObjectMapper     objectMapper;

    public VvsApiClient(@Qualifier("vvsRestTemplate") RestTemplate restTemplate,
                        VVSApiProperties props,
                        ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.props        = props;
        this.objectMapper = objectMapper;
    }

    public String getToken() {
        String url = props.getBaseUrl() + "/getToken?ProviderCode=" + props.getProviderCode();
        log.debug("Fetching VVS token from: {}", url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("API-KEY", props.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            String token = extractToken(response.getBody());
            if (token == null || token.isBlank())
                throw new VvsApiException("VVS returned empty token");
            return token;
        } catch (VvsApiException e) {
            throw e;
        } catch (Exception e) {
            throw new VvsApiException("VVS getToken failed: " + e.getMessage(), e);
        }
    }

    public String getByMvAndPlate(String token, String mvFileNo, String plateNo) {
        String url = props.getBaseUrl() + "/GetDetailsByMVFileNoAndPlateNo";
        return post(url, buildEntity(token, new VvsMvPlateRequest(mvFileNo, plateNo)));
    }

    public String getByEngineAndChassis(String token, String engineNo, String chassisNo) {
        String url = props.getBaseUrl() + "/GetDetailsByEngineNoAndChassisNo";
        return post(url, buildEntity(token, new VvsEngineChassisRequest(engineNo, chassisNo)));
    }

    public String confirmRequest(String token, String requestId, String vvipRef, String expiryDate) {
        String url = props.getBaseUrl() + "/ConfirmRequest";
        try {
            return post(url, buildEntity(token, new VvsConfirmRequest(requestId, vvipRef, expiryDate)));
        } catch (Exception e) {
            log.warn("VVS ConfirmRequest failed (non-blocking): {}", e.getMessage());
            return null;
        }
    }

    public VvsVehicleData parseVehicleData(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return null;
        try {
            return objectMapper.readValue(rawJson, VvsVehicleData.class);
        } catch (Exception e) {
            log.warn("Could not parse VVS vehicle data: {}", e.getMessage());
            return null;
        }
    }

    private <T> HttpEntity<T> buildEntity(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private String post(String url, HttpEntity<?> entity) {
        try {
            return restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
        } catch (Exception e) {
            throw new VvsApiException("VVS call to [" + url + "] failed: " + e.getMessage(), e);
        }
    }

    private String extractToken(String body) {
        if (body == null) return null;
        String trimmed = body.trim();
        if (!trimmed.startsWith("{")) return trimmed.replace("\"", "");
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            if (node.has("token"))        return node.get("token").asText();
            if (node.has("access_token")) return node.get("access_token").asText();
        } catch (Exception ignored) {}
        return trimmed;
    }

    public static class VvsApiException extends RuntimeException {
        public VvsApiException(String msg)                  { super(msg); }
        public VvsApiException(String msg, Throwable cause) { super(msg, cause); }
    }
}