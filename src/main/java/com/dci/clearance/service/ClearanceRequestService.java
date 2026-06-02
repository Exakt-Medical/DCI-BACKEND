package com.dci.clearance.service;

import com.dci.clearance.dto.ClearanceRequestDto;
import com.dci.clearance.entity.ClearanceRequest;
import com.dci.clearance.entity.ClearanceRequest.ClearanceStatus;
import com.dci.clearance.entity.ClearanceRequest.RequestType;
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
public class ClearanceRequestService {

    private final ClearanceRequestRepository clearanceRepo;
    private final UserRepository userRepository;

    @Transactional
    public ClearanceRequestDto createFromVoucher(Long voucherRequestId, Long userId) {
        ClearanceRequest voucherReq = clearanceRepo.findById(voucherRequestId)
                .orElseThrow(() -> new RuntimeException("Voucher request not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ClearanceRequest req = ClearanceRequest.builder()
                .referenceNo(generateReferenceNo())
                .user(user)
                .requestType(RequestType.CLEARANCE_REQUEST)
                .voucherRequestId(voucherRequestId)
                .plateNumber(voucherReq.getPlateNumber())
                .mvFileNumber(voucherReq.getMvFileNumber())
                .chassisNumber(voucherReq.getChassisNumber())
                .engineNumber(voucherReq.getEngineNumber())
                .vehicleMake(voucherReq.getVehicleMake())
                .vehicleSeries(voucherReq.getVehicleSeries())
                .vehicleYearModel(voucherReq.getVehicleYearModel())
                .vehicleColor(voucherReq.getVehicleColor())
                .ownerName(voucherReq.getOwnerName())
                .ownerAddress(voucherReq.getOwnerAddress())
                .voucherCode(voucherReq.getVoucherCode())
                .status(ClearanceStatus.MVC_MEC_UPLOADED)
                .build();
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
    public ClearanceRequestDto issueCertificate(Long requestId, String certificateNo, Long userId) {
        ClearanceRequest req = clearanceRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Clearance request not found"));
        req.setCertificateNo(certificateNo);
        req.setStatus(ClearanceStatus.CERTIFICATE_ISSUED);
        req = clearanceRepo.save(req);
        return toDto(req);
    }

    public List<ClearanceRequestDto> getMyRequests(Long userId) {
        return clearanceRepo.findByUserIdAndRequestTypeOrderByDateCreatedDesc(userId, RequestType.CLEARANCE_REQUEST).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<ClearanceRequestDto> getByAgentFixer(Long agentFixerId) {
        return clearanceRepo.findByAgentFixerIdAndRequestTypeOrderByDateCreatedDesc(agentFixerId, RequestType.CLEARANCE_REQUEST).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public List<ClearanceRequestDto> getAll() {
        return clearanceRepo.findByRequestTypeOrderByDateCreatedDesc(RequestType.CLEARANCE_REQUEST).stream()
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

    public ClearanceRequestDto getByCertificateNo(String certificateNo) {
        ClearanceRequest req = clearanceRepo.findByCertificateNo(certificateNo)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));
        return toDto(req);
    }

    private boolean isAgentFixer(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getRole() == User.UserRole.AGENT_FIXER)
                .orElse(false);
    }

    private String generateReferenceNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("CREQ-%s-%06d", date, System.currentTimeMillis() % 1_000_000);
    }

    private ClearanceRequestDto toDto(ClearanceRequest req) {
        return ClearanceRequestDto.builder()
                .id(req.getId())
                .referenceNo(req.getReferenceNo())
                .userId(req.getUser().getId())
                .requestType(req.getRequestType() != null ? req.getRequestType().name() : "CLEARANCE_REQUEST")
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
                .voucherRequestId(req.getVoucherRequestId())
                .hpgVerified(req.getHpgVerified())
                .certificateNo(req.getCertificateNo())
                .clientName(req.getUser().getFirstName() + " " + req.getUser().getLastName())
                .dateCreated(req.getDateCreated())
                .build();
    }
}
