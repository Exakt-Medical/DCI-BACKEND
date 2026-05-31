package com.exakt.vvip.service;

import com.exakt.vvip.dto.VoucherTransferDTO;
import com.exakt.vvip.dto.VoucherTransferRequest;
import com.exakt.vvip.entity.VoucherTransferEntity;
import com.exakt.vvip.repository.VoucherTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherTransferService {

    private final VoucherTransferRepository voucherRepository;
    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    // ─── Mapper ───────────────────────────────────────────────────────────────

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

        toTransfer.forEach(v -> {
            v.setCurrentUserId(request.getToUserId());
            v.setStatus("TRANSFERRED");
            v.setUpdatedAt(LocalDateTime.now(MANILA));
        });

        return voucherRepository.saveAll(toTransfer).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}