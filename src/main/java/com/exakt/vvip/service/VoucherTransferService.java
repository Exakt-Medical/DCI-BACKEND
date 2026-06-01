package com.exakt.vvip.service;

import com.exakt.vvip.dto.TransferHistoryDTO;
import com.exakt.vvip.dto.VoucherTransferDTO;
import com.exakt.vvip.dto.VoucherTransferRequest;
import com.exakt.vvip.entity.User;
import com.exakt.vvip.entity.VoucherTransferEntity;
import com.exakt.vvip.entity.VoucherTransferLog;
import com.exakt.vvip.repository.UserRepository;
import com.exakt.vvip.repository.VoucherTransferLogRepository;
import com.exakt.vvip.repository.VoucherTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherTransferService {

    private final VoucherTransferRepository voucherRepository;
    private final VoucherTransferLogRepository transferLogRepository;
    private final UserRepository userRepository;

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final long VOUCHER_VALUE = 60L;

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private VoucherTransferDTO mapToResponse(VoucherTransferEntity v) {
        return VoucherTransferDTO.builder()
                .id(v.getId())
                .voucherCode(v.getVoucherCode())
                .companyId(v.getCompanyId())
                .companyCode(v.getCompanyCode())
                .orderId(v.getOrderId())
                .tlpeTransactionId(v.getTlpeTransactionId())
                .merchantReference(v.getMerchantReference())
                .paymentReference(v.getPaymentReference())
                .originalUserId(v.getOriginalUserId())
                .currentUserId(v.getCurrentUserId())
                .status(v.getStatus())
                .voucherReference(v.getVoucherReference())
                .redeemedAt(v.getRedeemedAt() != null ? v.getRedeemedAt().toString() : null)
                .createdAt(v.getCreatedAt() != null ? v.getCreatedAt().toString() : null)
                .updatedAt(v.getUpdatedAt() != null ? v.getUpdatedAt().toString() : null)
                .expiresAt(v.getExpiresAt() != null ? v.getExpiresAt().toString() : null)
                .build();
    }

    private VoucherTransferLog mapToLog(VoucherTransferEntity v, Long fromUserId,
                                        Long toUserId, String toAgentName,
                                        String referenceNumber) {
        return VoucherTransferLog.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .toAgentName(toAgentName)
                .voucherId(v.getId())
                .voucherCode(v.getVoucherCode())
                .referenceNumber(referenceNumber)
                .build();
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    public List<VoucherTransferDTO> getAll() {
        return voucherRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<VoucherTransferDTO> getByCurrentUser(Long userId) {
        return voucherRepository.findByCurrentUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<VoucherTransferDTO> getByCurrentUserAndStatus(Long userId, String status) {
        return voucherRepository.findByCurrentUserIdAndStatus(userId, status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long countByCurrentUser(Long userId) {
        return voucherRepository.countByCurrentUserId(userId);
    }

    public long countByCurrentUserAndStatus(Long userId, String status) {
        return voucherRepository.countByCurrentUserIdAndStatus(userId, status);
    }

    public Page<VoucherTransferDTO> getAvailablePaginated(Long userId, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return voucherRepository
                .findAvailableByUserIdPaginated(userId, search == null ? "" : search, pageable)
                .map(this::mapToResponse);
    }

    /**
     * ✅ Batch count — returns { userId -> availableCount } map in a single DB query.
     * Used by the frontend agents list to avoid N individual count calls.
     */
    public Map<Long, Long> countAvailableByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        List<Object[]> rows = voucherRepository.countAvailableByUserIds(userIds);
        return rows.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> ((Number) row[1]).longValue()
        ));
    }

    // ─── Transfer History ─────────────────────────────────────────────────────

    public List<TransferHistoryDTO> getTransferHistory(Long fromUserId) {
        List<VoucherTransferLog> logs = transferLogRepository
                .findByFromUserIdOrderByTransferredAtDesc(fromUserId);

        Map<String, List<VoucherTransferLog>> grouped = logs.stream()
                .collect(Collectors.groupingBy(VoucherTransferLog::getReferenceNumber));

        return logs.stream()
                .map(VoucherTransferLog::getReferenceNumber)
                .distinct()
                .map(ref -> {
                    List<VoucherTransferLog> batch = grouped.get(ref);
                    VoucherTransferLog first = batch.get(0);
                    int qty = batch.size();
                    return TransferHistoryDTO.builder()
                            .referenceNumber(ref)
                            .fromUserId(first.getFromUserId())
                            .toUserId(first.getToUserId())
                            .toAgentName(first.getToAgentName())
                            .quantity(qty)
                            .totalValue(qty * VOUCHER_VALUE)
                            .voucherCodes(batch.stream()
                                    .map(VoucherTransferLog::getVoucherCode)
                                    .collect(Collectors.toList()))
                            .transferredAt(first.getTransferredAt().toString())
                            .status("Completed")
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─── Transfer ─────────────────────────────────────────────────────────────

    @Transactional
    public List<VoucherTransferDTO> transfer(Long fromUserId, VoucherTransferRequest request) {
        List<VoucherTransferEntity> toTransfer;

        if (request.getVoucherIds() != null && !request.getVoucherIds().isEmpty()) {
            toTransfer = voucherRepository.findByIdInAndCurrentUserIdAndStatus(
                    request.getVoucherIds(), fromUserId, "AVAILABLE");

            if (toTransfer.size() != request.getVoucherIds().size()) {
                throw new RuntimeException(
                        "Some vouchers are not available or do not belong to this user.");
            }
        } else {
            List<VoucherTransferEntity> available = voucherRepository
                    .findByCurrentUserIdAndStatus(fromUserId, "AVAILABLE");

            if (available.size() < request.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient vouchers. Available: " + available.size() +
                                ", Requested: " + request.getQuantity());
            }

            toTransfer = available.stream()
                    .limit(request.getQuantity())
                    .collect(Collectors.toList());
        }

        User agent = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new RuntimeException("Agent not found: " + request.getToUserId()));

        String toAgentName = ((agent.getFirstName() != null ? agent.getFirstName() : "") +
                " " + (agent.getLastName() != null ? agent.getLastName() : "")).trim();

        String referenceNumber = "TRF-" + String.valueOf(System.currentTimeMillis()).substring(5);
        LocalDateTime now = LocalDateTime.now(MANILA);

        toTransfer.forEach(v -> {
            v.setCurrentUserId(request.getToUserId());
            v.setStatus("AVAILABLE");
            v.setUpdatedAt(now);
        });

        List<VoucherTransferEntity> saved = voucherRepository.saveAll(toTransfer);

        List<VoucherTransferLog> logs = saved.stream()
                .map(v -> mapToLog(v, fromUserId, request.getToUserId(), toAgentName, referenceNumber))
                .collect(Collectors.toList());

        transferLogRepository.saveAll(logs);

        return saved.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}