package com.exakt.vvip.service;

import com.exakt.vvip.dto.CompanyRequest;
import com.exakt.vvip.dto.CompanyResponse;
import com.exakt.vvip.entity.Company;
import com.exakt.vvip.entity.User;
import com.exakt.vvip.repository.CompanyRepository;
import com.exakt.vvip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final AuditTrailService auditTrailService;

    @Transactional(readOnly = true)
    public List<CompanyResponse> getAll() {
        return companyRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(Long id) {
        return companyRepository.findById(id)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public CompanyResponse create(CompanyRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Company company = Company.builder()
                .companyId(request.getCompanyId())
                .companyName(request.getCompanyName())
                .companyShortname(request.getCompanyShortname())
                .code("CMP-" + java.util.UUID.randomUUID().toString().substring(0, 8))
                .approvalStatus(request.getApprovalStatus() != null ? request.getApprovalStatus() : "PENDING")
                .isactive(request.getIsactive() != null ? request.getIsactive() : true)
                .userstamp(user)
                .build();

        company = companyRepository.save(company);
        auditTrailService.logAction("Added a New Company " + company.getCompanyName(), "Created company '" + company.getCompanyName() + "'", username, user.getRole().name());
        return toResponse(company);
    }

    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request, String username) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String oldCompanyId = company.getCompanyId();
        String oldName = company.getCompanyName();
        String oldShortname = company.getCompanyShortname();
        String oldStatus = company.getApprovalStatus();
        Boolean oldActive = company.getIsactive();

        company.setCompanyId(request.getCompanyId());
        company.setCompanyName(request.getCompanyName());
        company.setCompanyShortname(request.getCompanyShortname());
        company.setApprovalStatus(request.getApprovalStatus() != null ? request.getApprovalStatus() : company.getApprovalStatus());
        company.setIsactive(request.getIsactive() != null ? request.getIsactive() : company.getIsactive());
        company.setUserstamp(user);

        company = companyRepository.save(company);

        List<String> changes = new ArrayList<>();
        if (Boolean.FALSE.equals(request.getIsactive()) && Boolean.TRUE.equals(oldActive)) {
            auditTrailService.logAction("Deactivated Company " + company.getCompanyName(), "Set Company " + company.getCompanyName() + " to inactive", username, user.getRole().name());
            return toResponse(company);
        }
        if (Boolean.TRUE.equals(request.getIsactive()) && Boolean.FALSE.equals(oldActive)) {
            auditTrailService.logAction("Activated Company " + company.getCompanyName(), "Set Company " + company.getCompanyName() + " to active", username, user.getRole().name());
            return toResponse(company);
        }
        if (!oldCompanyId.equals(request.getCompanyId())) {
            changes.add("Company ID from '" + oldCompanyId + "' to '" + request.getCompanyId() + "'");
        }
        if (!oldName.equals(request.getCompanyName())) {
            changes.add("Name from '" + oldName + "' to '" + request.getCompanyName() + "'");
        }
        if (!oldShortname.equals(request.getCompanyShortname())) {
            changes.add("Short Name from '" + oldShortname + "' to '" + request.getCompanyShortname() + "'");
        }
        if (request.getApprovalStatus() != null && !oldStatus.equals(request.getApprovalStatus())) {
            changes.add("Approval Status from '" + oldStatus + "' to '" + request.getApprovalStatus() + "'");
        }
        String actionMade, details;
        if (changes.isEmpty()) {
            actionMade = "Edited Company " + company.getCompanyName();
            details = "No changes detected";
        } else {
            actionMade = "Edited Company " + company.getCompanyName() + " (" + String.join("; ", changes) + ")";
            details = String.join("; ", changes);
        }
        auditTrailService.logAction(actionMade, details, username, user.getRole().name());
        return toResponse(company);
    }

    @Transactional
    public void delete(Long id) {
        Company company = companyRepository.findById(id).orElse(null);
        companyRepository.deleteById(id);
        if (company != null) {
            auditTrailService.logAction("Deleted Company " + company.getCompanyName(), "Deleted company '" + company.getCompanyName() + "'", "system", "SYSTEM");
        }
    }

    @Transactional
    public List<CompanyResponse> bulkCreate(List<CompanyRequest> requests, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<CompanyResponse> responses = requests.stream().map(request -> {
            Company company = Company.builder()
                    .companyId(request.getCompanyId())
                    .companyName(request.getCompanyName())
                    .companyShortname(request.getCompanyShortname())
                    .code("CMP-" + java.util.UUID.randomUUID().toString().substring(0, 8))
                    .approvalStatus("APPROVED")
                    .isactive(true)
                    .userstamp(user)
                    .build();
            return companyRepository.save(company);
        }).map(this::toResponse).collect(Collectors.toList());
        auditTrailService.logAction("Bulk Added " + responses.size() + " Companies", "Bulk created " + responses.size() + " companies", username, user.getRole().name());
        return responses;
    }

    private CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .companyId(company.getCompanyId())
                .companyName(company.getCompanyName())
                .companyShortname(company.getCompanyShortname())
                .approvalStatus(company.getApprovalStatus())
                .isactive(company.getIsactive())
                .userstamp(company.getUserstamp() != null ? company.getUserstamp().getUsername() : null)
                .timestamp(company.getTimestamp())
                .build();
    }
}
