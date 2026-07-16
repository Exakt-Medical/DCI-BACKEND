package com.dci.clearance.service;

import com.dci.clearance.dto.DashboardResponseDto;
import com.dci.clearance.dto.DashboardStatsDto;
import com.dci.clearance.dto.RecentTransactionDto;
import com.dci.clearance.dto.TopEmployeeDto;
import com.dci.clearance.dto.TransactionLogDTO;
import com.dci.clearance.entity.TransactionLog;
import com.dci.clearance.entity.User;
import com.dci.clearance.entity.VerificationRequest;
import com.dci.clearance.repository.DciCertificateRepository;
import com.dci.clearance.repository.TransactionLogRepository;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.repository.VerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    private final TransactionLogRepository transactionLogRepository;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM. dd, yyyy hh:mm a");

    // ==================== TRANSACTION LOGS METHODS ====================

    @Transactional
    public void logTransaction(String accountName, String companyName, String description, String response, String origin, String status) {
        TransactionLog log = TransactionLog.builder()
                .accountName(accountName)
                .companyName(companyName)
                .description(description)
                .response(response)
                .origin(origin != null ? origin : "SYSTEM")
                .status(status)
                .build();
        transactionLogRepository.save(log);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
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

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public DashboardResponseDto getDashboardData(int page, int size) {
        // Get paginated transactions for recent transactions table
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreated").descending());
        Page<VerificationRequest> requestPage = verificationRepo.findAll(pageable);

        // Calculate core stats
        Long totalUsers = userRepository.count();
        Long totalTransactions = verificationRepo.countAll();
        Long totalDciCertificates = dciCertificateRepo.count();
        Long totalProcessedVehicles = verificationRepo.countDistinctPlateNumbers();

        // Get top employees
        List<TopEmployeeDto> topDciEmployees = getTopEmployees(User.UserRole.DCI);
        List<TopEmployeeDto> topHpgEmployees = getTopEmployees(User.UserRole.HPG);

        log.info("Dashboard stats: totalUsers={}, totalTransactions={}, totalDciCerts={}, totalVehicles={}, dciTop={}, hpgTop={}",
                totalUsers, totalTransactions, totalDciCertificates, totalProcessedVehicles,
                topDciEmployees.size(), topHpgEmployees.size());

        // Convert to DTOs for recent transactions table
        List<RecentTransactionDto> recentTransactions = requestPage.getContent().stream()
                .map(this::convertToRecentTransactionDtoFromEntity)
                .collect(Collectors.toList());

        return DashboardResponseDto.builder()
                .stats(DashboardStatsDto.builder()
                        .totalUsers(totalUsers)
                        .totalTransactions(totalTransactions)
                        .totalDciCertificates(totalDciCertificates)
                        .totalProcessedVehicles(totalProcessedVehicles)
                        .topDciEmployees(topDciEmployees)
                        .topHpgEmployees(topHpgEmployees)
                        .build())
                .recentTransactions(recentTransactions)
                .totalPages(requestPage.getTotalPages())
                .currentPage(page)
                .totalItems(requestPage.getTotalElements())
                .build();
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    private List<TopEmployeeDto> getTopEmployees(User.UserRole role) {
        try {
            List<User> users = userRepository.findByRole(role);
            if (users.isEmpty()) {
                return java.util.Collections.emptyList();
            }

            List<Long> userIds = users.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());

            List<Object[]> counts = verificationRepo.countByRequestedByIn(userIds);

            // Create a map of userId -> count
            Map<Long, Long> countMap = new HashMap<>();
            for (Object[] row : counts) {
                Long userId = ((Number) row[0]).longValue();
                Long count = ((Number) row[1]).longValue();
                countMap.put(userId, count);
            }

            // Map users to TopEmployeeDto with their counts
            return users.stream()
                    .map(u -> {
                        long count = countMap.getOrDefault(u.getId(), 0L);
                        String name = String.join(" ",
                                u.getFirstName() != null ? u.getFirstName() : "",
                                u.getLastName() != null ? u.getLastName() : ""
                        ).trim();
                        if (name.isEmpty()) name = u.getUsername();
                        return TopEmployeeDto.builder()
                                .name(name)
                                .username(u.getUsername())
                                .count(count)
                                .build();
                    })
                    .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                    .limit(5)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting top employees for role {}: {}", role, e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
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
                                user.getLastName() != null ? user.getLastName() : ""
                        ).trim();
                        account = fullName.isEmpty() ? user.getUsername() : fullName;

                        company = "N/A";
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

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
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

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
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

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
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