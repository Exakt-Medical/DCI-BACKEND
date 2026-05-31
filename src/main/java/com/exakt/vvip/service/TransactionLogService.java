package com.exakt.vvip.service;

import com.exakt.vvip.dto.DashboardResponseDto;
import com.exakt.vvip.dto.DashboardStatsDto;
import com.exakt.vvip.dto.RecentTransactionDto;
import com.exakt.vvip.dto.TransactionLogDTO;
import com.exakt.vvip.entity.Purchase;
import com.exakt.vvip.entity.User;
import com.exakt.vvip.entity.VerificationRequest;
import com.exakt.vvip.entity.VoucherTransferEntity;
import com.exakt.vvip.repository.DciCertificateRepository;
import com.exakt.vvip.repository.PurchaseRepository;
import com.exakt.vvip.repository.UserRepository;
import com.exakt.vvip.repository.VerificationRequestRepository;
import com.exakt.vvip.repository.VoucherTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionLogService {

    private final VerificationRequestRepository verificationRepo;
    private final DciCertificateRepository dciCertificateRepo;
    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;
    private final VoucherTransferRepository voucherTransferRepository;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM. dd, yyyy hh:mm a");

    // ==================== TRANSACTION LOGS METHODS ====================

    public Map<String, Object> getTransactionLogs(String status, String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Object[]> requestPage = verificationRepo.findTransactionLogs(status, searchTerm, pageable);

        var transactions = requestPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Map<String, Long> stats = getStatistics();

        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactions);
        response.put("currentPage", page);
        response.put("totalPages", requestPage.getTotalPages());
        response.put("totalElements", requestPage.getTotalElements());
        response.put("stats", stats);

        return response;
    }

    // ==================== DASHBOARD METHODS ====================

    public DashboardResponseDto getDashboardData(int page, int size) {
        // Get paginated transactions for recent transactions table
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreated").descending());
        Page<VerificationRequest> requestPage = verificationRepo.findAll(pageable);

        // Calculate all stats
        Long totalAgents = getTotalAgents();
        Long totalTransactions = verificationRepo.count();
        Long lastWeekAuthenticated = getLastWeekAuthenticatedCount();
        Long todayAuthenticated = getTodayAuthenticatedCount();
        Long todayPurchasedVouchers = getTodayPurchasedVouchersCount();
        Long availableVouchers = getAvailableVouchersCount();
        Long agentsCount = getAgentsCount();
        Long subagentsCount = getSubagentsCount();

        // Convert to DTOs for dashboard
        List<RecentTransactionDto> recentTransactions = requestPage.getContent().stream()
                .map(this::convertToRecentTransactionDtoFromEntity)
                .collect(Collectors.toList());

        log.info("Dashboard stats: totalAgents={}, totalTransactions={}, lastWeekAuth={}, todayAuth={}, todayVouchers={}, availableVouchers={}, agents={}, subagents={}",
                totalAgents, totalTransactions, lastWeekAuthenticated, todayAuthenticated,
                todayPurchasedVouchers, availableVouchers, agentsCount, subagentsCount);

        return DashboardResponseDto.builder()
                .stats(DashboardStatsDto.builder()
                        .totalAgents(totalAgents != null ? totalAgents : 0L)
                        .totalTransactions(totalTransactions)
                        .lastWeekAuthenticated(lastWeekAuthenticated != null ? lastWeekAuthenticated : 0L)
                        .todayAuthenticated(todayAuthenticated != null ? todayAuthenticated : 0L)
                        .todayPurchasedVouchers(todayPurchasedVouchers != null ? todayPurchasedVouchers : 0L)
                        .availableVouchers(availableVouchers != null ? availableVouchers : 0L)
                        .agentsCount(agentsCount != null ? agentsCount : 0L)
                        .subagentsCount(subagentsCount != null ? subagentsCount : 0L)
                        .build())
                .recentTransactions(recentTransactions)
                .totalPages(requestPage.getTotalPages())
                .currentPage(page)
                .totalItems(requestPage.getTotalElements())
                .build();
    }

    private Long getTotalAgents() {
        try {
            long agents = userRepository.countByRole(User.UserRole.AGENT);
            long subagents = userRepository.countByRole(User.UserRole.SUBAGENT);
            return agents + subagents;
        } catch (Exception e) {
            log.error("Error counting total agents: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getLastWeekAuthenticatedCount() {
        try {
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
            return verificationRepo.countAuthenticatedSince(oneWeekAgo);
        } catch (Exception e) {
            log.error("Error counting last week authenticated: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getTodayAuthenticatedCount() {
        try {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            return verificationRepo.countAuthenticatedSince(todayStart);
        } catch (Exception e) {
            log.error("Error counting today authenticated: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Get count of vouchers purchased today from the purchases table
     */
    private Long getTodayPurchasedVouchersCount() {
        try {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime todayEnd = LocalDate.now().atTime(23, 59, 59);

            List<Purchase> todayPurchases = purchaseRepository.findAll().stream()
                    .filter(p -> p.getPurchaseDate() != null)
                    .filter(p -> p.getPurchaseDate().isAfter(todayStart) && p.getPurchaseDate().isBefore(todayEnd))
                    .collect(Collectors.toList());

            long totalVouchers = todayPurchases.size();
            log.debug("Today's purchased vouchers count: {}", totalVouchers);
            return totalVouchers;
        } catch (Exception e) {
            log.error("Error counting today purchased vouchers: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Get count of available vouchers from the vouchers table
     * Available vouchers = vouchers with status 'AVAILABLE' that are not expired
     */
    private Long getAvailableVouchersCount() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Get all vouchers with status AVAILABLE
            List<VoucherTransferEntity> availableVouchers = voucherTransferRepository.findByStatus("AVAILABLE");

            // Filter out expired ones
            long count = availableVouchers.stream()
                    .filter(v -> v.getExpiresAt() == null || v.getExpiresAt().isAfter(now))
                    .count();

            log.debug("Available vouchers count: {}", count);
            return count;
        } catch (Exception e) {
            log.error("Error counting available vouchers: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getAgentsCount() {
        try {
            return userRepository.countByRole(User.UserRole.AGENT);
        } catch (Exception e) {
            log.error("Error counting agents: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getSubagentsCount() {
        try {
            return userRepository.countByRole(User.UserRole.SUBAGENT);
        } catch (Exception e) {
            log.error("Error counting subagents: {}", e.getMessage());
            return 0L;
        }
    }

    private RecentTransactionDto convertToRecentTransactionDtoFromEntity(VerificationRequest vr) {
        try {
            String account = "System";
            String company = "N/A";

            if (vr.getRequestedBy() != null) {
                try {
                    User user = userRepository.findById(vr.getRequestedBy()).orElse(null);
                    if (user != null) {
                        String fullName = String.join(" ",
                                user.getFirstName() != null ? user.getFirstName() : "",
                                user.getMiddleInitial() != null ? user.getMiddleInitial() : "",
                                user.getLastName() != null ? user.getLastName() : ""
                        ).trim();
                        account = fullName.isEmpty() ? user.getUsername() : fullName;

                        if (user.getBranch() != null) {
                            String comp = "";
                            try {
                                if (user.getBranch().getCompany() != null) {
                                    comp = user.getBranch().getCompany().getCompanyName();
                                }
                                if (user.getBranch().getBranchName() != null && !user.getBranch().getBranchName().isEmpty()) {
                                    if (!comp.isEmpty()) {
                                        comp += " - " + user.getBranch().getBranchName();
                                    } else {
                                        comp = user.getBranch().getBranchName();
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Error getting branch/company info: {}", e.getMessage());
                            }
                            company = comp.isEmpty() ? "N/A" : comp;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error fetching user {}: {}", vr.getRequestedBy(), e.getMessage());
                }
            }

            String certificateNo = null;
            try {
                var certOpt = dciCertificateRepo.findByVerificationId(vr.getId());
                if (certOpt.isPresent()) {
                    certificateNo = certOpt.get().getCertificateNo();
                }
            } catch (Exception e) {
                log.warn("Error fetching certificate for {}: {}", vr.getId(), e.getMessage());
            }

            String dciAuthCode = certificateNo != null ? certificateNo : generateAuthCode();
            String dateCreated = vr.getDateCreated() != null ? vr.getDateCreated().format(DATE_FORMATTER) : "";

            String plateNo = vr.getPlateNumber() != null && !vr.getPlateNumber().isEmpty() ? vr.getPlateNumber() : "N/A";
            String mvFileNo = vr.getMvFileNumber() != null && !vr.getMvFileNumber().isEmpty() ? vr.getMvFileNumber() : "N/A";
            String chassisNo = vr.getChassisNumber() != null && !vr.getChassisNumber().isEmpty() ? vr.getChassisNumber() : "N/A";
            String engineNo = vr.getEngineNumber() != null && !vr.getEngineNumber().isEmpty() ? vr.getEngineNumber() : "N/A";

            return RecentTransactionDto.builder()
                    .id(vr.getId())
                    .agent(account)
                    .company(company)
                    .dciAuthCode(dciAuthCode)
                    .plateNo(plateNo)
                    .mvFile(mvFileNo)
                    .chassisNo(chassisNo)
                    .engineNo(engineNo)
                    .dateCreated(dateCreated)
                    .build();

        } catch (Exception e) {
            log.error("Error converting transaction {}: {}", vr.getId(), e.getMessage());
            return RecentTransactionDto.builder()
                    .id(vr.getId())
                    .agent("Error loading")
                    .company("N/A")
                    .dciAuthCode("N/A")
                    .plateNo(vr.getPlateNumber() != null ? vr.getPlateNumber() : "N/A")
                    .mvFile("N/A")
                    .chassisNo("N/A")
                    .engineNo("N/A")
                    .dateCreated(vr.getDateCreated() != null ? vr.getDateCreated().format(DATE_FORMATTER) : "")
                    .build();
        }
    }

    private RecentTransactionDto convertToRecentTransactionDto(Object[] row) {
        Long id = ((Number) row[0]).longValue();
        String referenceNo = (String) row[1];
        String account = (String) row[2];
        String company = (String) row[3];
        String verificationStatus = (String) row[4];
        String plateNumber = (String) row[5];
        String failureReason = (String) row[6];
        String certificateNo = (String) row[7];
        Object dateObj = row[8];

        String mvFileNo = "";
        String chassisNo = "";
        String engineNo = "";

        if (row.length > 9 && row[9] != null) {
            mvFileNo = (String) row[9];
        }
        if (row.length > 10 && row[10] != null) {
            chassisNo = (String) row[10];
        }
        if (row.length > 11 && row[11] != null) {
            engineNo = (String) row[11];
        }

        String dciAuthCode = certificateNo != null ? certificateNo : generateAuthCode();

        String dateCreated = "";
        if (dateObj != null) {
            if (dateObj instanceof java.sql.Timestamp) {
                dateCreated = ((java.sql.Timestamp) dateObj).toLocalDateTime().format(DATE_FORMATTER);
            } else if (dateObj instanceof java.time.LocalDateTime) {
                dateCreated = ((java.time.LocalDateTime) dateObj).format(DATE_FORMATTER);
            }
        }

        return RecentTransactionDto.builder()
                .id(id)
                .agent(account != null ? account : "System")
                .company(company != null ? company : "N/A")
                .dciAuthCode(dciAuthCode)
                .plateNo(plateNumber != null ? plateNumber : "N/A")
                .mvFile(mvFileNo != null && !mvFileNo.isEmpty() ? mvFileNo : "N/A")
                .chassisNo(chassisNo != null && !chassisNo.isEmpty() ? chassisNo : "N/A")
                .engineNo(engineNo != null && !engineNo.isEmpty() ? engineNo : "N/A")
                .dateCreated(dateCreated)
                .build();
    }

    private TransactionLogDTO convertToDTO(Object[] row) {
        Long id = ((Number) row[0]).longValue();
        String referenceNo = (String) row[1];
        String account = (String) row[2];
        String company = (String) row[3];
        String verificationStatus = (String) row[4];
        String plateNumber = (String) row[5];
        String failureReason = (String) row[6];
        String certificateNo = (String) row[7];
        Object dateObj = row[8];

        String description;
        if ("COMPLETED".equals(verificationStatus)) {
            description = "SUBMIT VEHICLE WITH THE FOLLOWING RN#" + referenceNo;
        } else {
            String plate = plateNumber != null ? plateNumber : "UNKNOWN";
            description = "INITIATED VEHICLE VERIFICATION VIA WEB (PLATE NO: " + plate + ")";
        }

        String response;
        if ("COMPLETED".equals(verificationStatus)) {
            response = "SUCCESSFULLY AUTHENTICATED USING AUTHENTICATION CODE #" +
                    (certificateNo != null ? certificateNo : "UNKNOWN");
        } else if ("VERIFIED".equals(verificationStatus)) {
            response = "VEHICLE HAS BEEN VERIFIED";
        } else {
            if (failureReason != null && failureReason.contains("already been verified")) {
                response = "THIS VEHICLE HAS ALREADY BEEN VERIFIED AND HAS AN EXISTING CERTIFICATE.";
            } else {
                response = "VEHICLE NOT FOUND!";
            }
        }

        String displayStatus;
        if ("COMPLETED".equals(verificationStatus)) {
            displayStatus = "Authenticated";
        } else if ("VERIFIED".equals(verificationStatus)) {
            displayStatus = "Verified";
        } else {
            displayStatus = "Failed";
        }

        String dateCreated = "";
        if (dateObj != null) {
            if (dateObj instanceof java.sql.Timestamp) {
                dateCreated = ((java.sql.Timestamp) dateObj).toLocalDateTime().format(DATE_FORMATTER);
            } else if (dateObj instanceof java.time.LocalDateTime) {
                dateCreated = ((java.time.LocalDateTime) dateObj).format(DATE_FORMATTER);
            }
        }

        return TransactionLogDTO.builder()
                .id(id)
                .referenceNo(referenceNo)
                .account(account != null ? account : "System")
                .company(company != null ? company : "N/A")
                .description(description)
                .response(response)
                .origin("WEB")
                .status(displayStatus)
                .dateCreated(dateCreated)
                .build();
    }

    private Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("authenticated", verificationRepo.countAuthenticated());
        stats.put("verified", verificationRepo.countVerified());
        stats.put("failed", verificationRepo.countFailed());
        return stats;
    }

    private String generateAuthCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                int index = (int) (Math.random() * chars.length());
                sb.append(chars.charAt(index));
            }
            if (i < 3) sb.append("-");
        }
        return sb.toString();
    }   
}