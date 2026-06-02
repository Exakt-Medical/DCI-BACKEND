package com.dci.clearance.controller;

import com.dci.clearance.dto.MyRequestSummaryDto;
import com.dci.clearance.entity.ClearanceRequest;
import com.dci.clearance.entity.ClearanceRequest.ClearanceStatus;
import com.dci.clearance.entity.ClearanceRequest.RequestType;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.ClearanceRequestRepository;
import com.dci.clearance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/my-requests")
@RequiredArgsConstructor
public class MyRequestController {

    private final ClearanceRequestRepository clearanceRepo;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getMyRequests(Authentication auth) {
        Long userId = getUserId(auth);
        User user = userRepository.findById(userId).orElseThrow();

        List<ClearanceRequest> vouchers = clearanceRepo
                .findByUserIdAndRequestTypeOrderByDateCreatedDesc(userId, RequestType.VOUCHER_REQUEST);

        List<ClearanceRequest> clearances = clearanceRepo
                .findByUserIdAndRequestTypeOrderByDateCreatedDesc(userId, RequestType.CLEARANCE_REQUEST);

        Map<Long, ClearanceRequest> clearanceByVoucherId = clearances.stream()
                .filter(c -> c.getVoucherRequestId() != null)
                .collect(Collectors.toMap(ClearanceRequest::getVoucherRequestId, c -> c, (a, b) -> a));

        List<MyRequestSummaryDto> result = new ArrayList<>();
        for (ClearanceRequest v : vouchers) {
            ClearanceRequest c = clearanceByVoucherId.get(v.getId());
            result.add(toSummary(v, c));
        }

        for (ClearanceRequest c : clearances) {
            boolean alreadyPaired = result.stream()
                    .anyMatch(r -> r.getClearanceRequestId() != null && r.getClearanceRequestId().equals(c.getId()));
            if (!alreadyPaired) {
                result.add(toSummary(null, c));
            }
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/agent")
    public ResponseEntity<?> getAgentRequests(Authentication auth) {
        Long agentId = getUserId(auth);

        List<ClearanceRequest> vouchers = clearanceRepo
                .findByAgentFixerIdAndRequestTypeOrderByDateCreatedDesc(agentId, RequestType.VOUCHER_REQUEST);

        List<ClearanceRequest> clearances = clearanceRepo
                .findByAgentFixerIdAndRequestTypeOrderByDateCreatedDesc(agentId, RequestType.CLEARANCE_REQUEST);

        Map<Long, ClearanceRequest> clearanceByVoucherId = clearances.stream()
                .filter(c -> c.getVoucherRequestId() != null)
                .collect(Collectors.toMap(ClearanceRequest::getVoucherRequestId, c -> c, (a, b) -> a));

        List<MyRequestSummaryDto> result = new ArrayList<>();
        for (ClearanceRequest v : vouchers) {
            ClearanceRequest c = clearanceByVoucherId.get(v.getId());
            result.add(toSummary(v, c));
        }
        for (ClearanceRequest c : clearances) {
            boolean alreadyPaired = result.stream()
                    .anyMatch(r -> r.getClearanceRequestId() != null && r.getClearanceRequestId().equals(c.getId()));
            if (!alreadyPaired) {
                result.add(toSummary(null, c));
            }
        }

        return ResponseEntity.ok(result);
    }

    private MyRequestSummaryDto toSummary(ClearanceRequest v, ClearanceRequest c) {
        return MyRequestSummaryDto.builder()
                .voucherRequestId(v != null ? v.getId() : (c != null ? c.getVoucherRequestId() : null))
                .voucherReferenceNo(v != null ? v.getReferenceNo() : null)
                .voucherStatus(v != null ? v.getStatus().name() : null)
                .clearanceRequestId(c != null ? c.getId() : null)
                .clearanceReferenceNo(c != null ? c.getReferenceNo() : null)
                .clearanceStatus(c != null ? c.getStatus().name() : null)
                .certificateNo(c != null ? c.getCertificateNo() : null)
                .plateNumber(v != null ? v.getPlateNumber() : (c != null ? c.getPlateNumber() : null))
                .mvFileNumber(v != null ? v.getMvFileNumber() : (c != null ? c.getMvFileNumber() : null))
                .ownerName(v != null ? v.getOwnerName() : (c != null ? c.getOwnerName() : null))
                .vehicleMake(v != null ? v.getVehicleMake() : (c != null ? c.getVehicleMake() : null))
                .vehicleSeries(v != null ? v.getVehicleSeries() : (c != null ? c.getVehicleSeries() : null))
                .clientName(v != null ? v.getUser().getFirstName() + " " + v.getUser().getLastName()
                        : (c != null ? c.getUser().getFirstName() + " " + c.getUser().getLastName() : null))
                .dateCreated(v != null ? v.getDateCreated() : (c != null ? c.getDateCreated() : null))
                .build();
    }

    private Long getUserId(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username))
                .getId();
    }
}
