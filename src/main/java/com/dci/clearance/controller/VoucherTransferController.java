package com.dci.clearance.controller;

import com.dci.clearance.dto.TransferHistoryDTO;
import com.dci.clearance.dto.VoucherTransferDTO;
import com.dci.clearance.dto.VoucherTransferRequest;
import com.dci.clearance.service.VoucherTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voucher-transfer")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Voucher Transfer", description = "Voucher Transfer Endpoints")
public class VoucherTransferController {

    private final VoucherTransferService voucherService;

    @GetMapping
    @Operation(summary = "Get all vouchers")
    public ResponseEntity<List<VoucherTransferDTO>> getAll() {
        return ResponseEntity.ok(voucherService.getAll());
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "Get vouchers by current user ID")
    public ResponseEntity<List<VoucherTransferDTO>> getByCurrentUser(@PathVariable Long userId) {
        return ResponseEntity.ok(voucherService.getByCurrentUser(userId));
    }

    @GetMapping("/by-user/{userId}/available")
    @Operation(summary = "Get paginated available vouchers for a user with optional search")
    public ResponseEntity<Page<VoucherTransferDTO>> getAvailablePaginated(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "") String search) {
        return ResponseEntity.ok(voucherService.getAvailablePaginated(userId, page, size, search));
    }

    @GetMapping("/by-user/{userId}/status/{status}")
    @Operation(summary = "Get vouchers by current user ID and status")
    public ResponseEntity<List<VoucherTransferDTO>> getByCurrentUserAndStatus(
            @PathVariable Long userId,
            @PathVariable String status) {
        return ResponseEntity.ok(voucherService.getByCurrentUserAndStatus(userId, status));
    }

    @GetMapping("/count/by-user/{userId}")
    @Operation(summary = "Count all vouchers for a user")
    public ResponseEntity<Map<String, Long>> countByUser(@PathVariable Long userId) {
        long total = voucherService.countByCurrentUser(userId);
        long available = voucherService.countByCurrentUserAndStatus(userId, "AVAILABLE");
        long transferred = voucherService.countByCurrentUserAndStatus(userId, "TRANSFERRED");
        long redeemed = voucherService.countByCurrentUserAndStatus(userId, "REDEEMED");
        return ResponseEntity.ok(Map.of(
                "total", total,
                "available", available,
                "transferred", transferred,
                "redeemed", redeemed
        ));
    }

    @PostMapping("/count/batch")
    @Operation(summary = "Batch count available vouchers for multiple users in one query")
    public ResponseEntity<Map<Long, Long>> countBatch(@RequestBody List<Long> userIds) {
        return ResponseEntity.ok(voucherService.countAvailableByUserIds(userIds));
    }

    @PostMapping("/transfer/from/{fromUserId}")
    @Operation(summary = "Transfer vouchers from manager to agent")
    public ResponseEntity<?> transfer(
            @PathVariable Long fromUserId,
            @RequestBody VoucherTransferRequest request) {
        return ResponseEntity.badRequest().body(Map.of("error", "Voucher transfers are currently disabled"));
    }

    @GetMapping("/transfer/history/{fromUserId}")
    @Operation(summary = "Get transfer history for a manager grouped by batch")
    public ResponseEntity<List<TransferHistoryDTO>> getTransferHistory(
            @PathVariable Long fromUserId) {
        return ResponseEntity.ok(voucherService.getTransferHistory(fromUserId));
    }
}