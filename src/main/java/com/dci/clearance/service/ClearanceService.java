package com.dci.clearance.service;

import com.dci.clearance.dto.ClearanceRequestDto;
import com.dci.clearance.entity.ClearanceRequest;
import com.dci.clearance.entity.ClearanceRequest.ClearanceStatus;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.ClearanceRequestRepository;
import com.dci.clearance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClearanceService {

    private final ClearanceRequestRepository clearanceRepo;
    private final UserRepository userRepository;

    public ClearanceRequestDto createDraft(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ClearanceRequest req = ClearanceRequest.builder()
                .referenceNo(generateReferenceNo())
                .user(user)
                .status(ClearanceStatus.DRAFT)
                .build();
        req = clearanceRepo.save(req);
        return toDto(req);
    }

    @Transactional
    public ClearanceRequestDto uploadOrCr(Long requestId, byte[] image, String ocrData, Long userId) {
        ClearanceRequest req = clearanceRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Clearance request not found"));
        if (!req.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        req.setOrCrImage(image);
        req.setOrCrOcrData(ocrData);
        if (req.getStatus() == ClearanceStatus.DRAFT) {
            req.setStatus(ClearanceStatus.ORCR_UPLOADED);
        }
        req = clearanceRepo.save(req);
        return toDto(req);
    }

    @Transactional
    public ClearanceRequestDto updateVehicleDetails(Long requestId, String plateNumber, String mvFileNumber,
                                                     String chassisNumber, String engineNumber,
                                                     String make, String series, String yearModel, String color,
                                                     String ownerName, String ownerAddress, Long userId) {
        ClearanceRequest req = clearanceRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Clearance request not found"));
        if (!req.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        req.setPlateNumber(plateNumber);
        req.setMvFileNumber(mvFileNumber);
        req.setChassisNumber(chassisNumber);
        req.setEngineNumber(engineNumber);
        req.setVehicleMake(make);
        req.setVehicleSeries(series);
        req.setVehicleYearModel(yearModel);
        req.setVehicleColor(color);
        req.setOwnerName(ownerName);
        req.setOwnerAddress(ownerAddress);
        req = clearanceRepo.save(req);
        return toDto(req);
    }

    @Transactional
    public ClearanceRequestDto markPaymentCompleted(Long requestId, Long userId) {
        ClearanceRequest req = clearanceRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Clearance request not found"));
        if (!req.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        req.setStatus(ClearanceStatus.PAYMENT_COMPLETED);
        req = clearanceRepo.save(req);
        return toDto(req);
    }

    @Transactional
    public ClearanceRequestDto issueVoucher(Long requestId, String voucherCode, Long userId) {
        ClearanceRequest req = clearanceRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Clearance request not found"));
        if (!req.getUser().getId().equals(userId) && !isAgentFixer(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        req.setVoucherCode(voucherCode);
        req.setStatus(ClearanceStatus.VOUCHER_ISSUED);
        req = clearanceRepo.save(req);
        return toDto(req);
    }

    @Transactional
    public ClearanceRequestDto hpgVerify(Long requestId, Long hpgUserId) {
        ClearanceRequest req = clearanceRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Clearance request not found"));
        req.setHpgVerified(true);
        req.setHpgVerifiedBy(hpgUserId);
        req.setHpgVerifiedAt(LocalDateTime.now());
        req.setStatus(ClearanceStatus.HPG_VERIFICATION);
        req = clearanceRepo.save(req);
        return toDto(req);
    }

    @Transactional
    public ClearanceRequestDto uploadMvcMec(Long requestId, byte[] image, String ocrData, Long userId) {
        ClearanceRequest req = clearanceRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Clearance request not found"));
        if (!req.getUser().getId().equals(userId) && !isAgentFixer(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        req.setMvcMecImage(image);
        req.setMvcMecOcrData(ocrData);
        req.setStatus(ClearanceStatus.MVC_MEC_UPLOADED);
        req = clearanceRepo.save(req);
        return toDto(req);
    }

    @Transactional
    public ClearanceRequestDto issueCertificate(Long requestId, String certificateNo, Long userId) {
        ClearanceRequest req = clearanceRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Clearance request not found"));
        req.setCertificateNo(certificateNo);
        req.setStatus(ClearanceStatus.CERTIFICATE_ISSUED);
        req = clearanceRepo.save(req);
        return toDto(req);
    }

    public List<ClearanceRequestDto> getMyRequests(Long userId) {
        return clearanceRepo.findByUserIdOrderByDateCreatedDesc(userId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<ClearanceRequestDto> getByAgentFixer(Long agentFixerId) {
        return clearanceRepo.findByAgentFixerIdOrderByDateCreatedDesc(agentFixerId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public ClearanceRequestDto getByReferenceNo(String referenceNo) {
        ClearanceRequest req = clearanceRepo.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new RuntimeException("Clearance request not found"));
        return toDto(req);
    }

    public ClearanceRequestDto getByVoucherCode(String voucherCode) {
        ClearanceRequest req = clearanceRepo.findByVoucherCode(voucherCode)
                .orElseThrow(() -> new RuntimeException("Clearance request not found"));
        return toDto(req);
    }

    @Transactional
    public ClearanceRequestDto assignVoucher(Long requestId, String voucherCode, Long agentFixerId) {
        ClearanceRequest req = clearanceRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Clearance request not found"));
        req.setVoucherCode(voucherCode);
        req.setAgentFixerId(agentFixerId);
        req.setStatus(ClearanceStatus.VOUCHER_ISSUED);
        req = clearanceRepo.save(req);
        return toDto(req);
    }

    private boolean isAgentFixer(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getRole() == User.UserRole.AGENT_FIXER)
                .orElse(false);
    }

    private String generateReferenceNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("DCI-%s-%06d", date, System.currentTimeMillis() % 1_000_000);
    }

    private ClearanceRequestDto toDto(ClearanceRequest req) {
        return ClearanceRequestDto.builder()
                .id(req.getId())
                .referenceNo(req.getReferenceNo())
                .userId(req.getUser().getId())
                .status(req.getStatus().name())
                .plateNumber(req.getPlateNumber())
                .mvFileNumber(req.getMvFileNumber())
                .chassisNumber(req.getChassisNumber())
                .engineNumber(req.getEngineNumber())
                .vehicleMake(req.getVehicleMake())
                .vehicleSeries(req.getVehicleSeries())
                .vehicleYearModel(req.getVehicleYearModel())
                .vehicleColor(req.getVehicleColor())
                .ownerName(req.getOwnerName())
                .ownerAddress(req.getOwnerAddress())
                .voucherCode(req.getVoucherCode())
                .hpgVerified(req.getHpgVerified())
                .certificateNo(req.getCertificateNo())
                .dateCreated(req.getDateCreated())
                .build();
    }
}