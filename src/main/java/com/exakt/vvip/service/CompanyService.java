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
        if (request.getStatus() == null) request.setStatus("ACTIVE");

        User user = userRepository.findByUsername(username).orElse(null);

        Company company = Company.builder()
                .companyName(request.getCompanyName())
                .code("CMP-" + java.util.UUID.randomUUID().toString().substring(0, 8))
                .provider(request.getProvider())
                .approvalStatus(request.getApprovalStatus() != null ? request.getApprovalStatus() : "PENDING")
                .status(request.getStatus())
                .address(request.getAddress())
                .userstamp(user)
                .build();

        company = companyRepository.save(company);
        auditTrailService.logAction("Added a New Company " + company.getCompanyName(), "Created company '" + company.getCompanyName() + "'", username, user != null ? user.getRole().name() : "SYSTEM");
        return toResponse(company);
    }

    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request, String username) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String oldName = company.getCompanyName();
        String oldApprovalStatus = company.getApprovalStatus();
        String oldStatus = company.getStatus();

        company.setCompanyName(request.getCompanyName());
        company.setProvider(request.getProvider());
        company.setApprovalStatus(request.getApprovalStatus() != null ? request.getApprovalStatus() : company.getApprovalStatus());
        company.setStatus(request.getStatus() != null ? request.getStatus() : company.getStatus());
        company.setAddress(request.getAddress());
        company.setUserstamp(user);

        company = companyRepository.save(company);

        if ("INACTIVE".equals(request.getStatus()) && "ACTIVE".equals(oldStatus)) {
            auditTrailService.logAction("Deactivated Company " + company.getCompanyName(), "Set Company " + company.getCompanyName() + " to inactive", username, user.getRole().name());
            return toResponse(company);
        }
        if ("ACTIVE".equals(request.getStatus()) && "INACTIVE".equals(oldStatus)) {
            auditTrailService.logAction("Activated Company " + company.getCompanyName(), "Set Company " + company.getCompanyName() + " to active", username, user.getRole().name());
            return toResponse(company);
        }
        List<String> changes = new ArrayList<>();
        if (!oldName.equals(request.getCompanyName())) {
            changes.add("Name from '" + oldName + "' to '" + request.getCompanyName() + "'");
        }
        if (request.getApprovalStatus() != null && !oldApprovalStatus.equals(request.getApprovalStatus())) {
            changes.add("Approval Status from '" + oldApprovalStatus + "' to '" + request.getApprovalStatus() + "'");
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
        User user = userRepository.findByUsername(username).orElse(null);
        List<CompanyResponse> responses = requests.stream().map(request -> {
            if (request.getStatus() == null) request.setStatus("ACTIVE");
            Company company = Company.builder()
                    .companyName(request.getCompanyName())
                    .code("CMP-" + java.util.UUID.randomUUID().toString().substring(0, 8))
                    .provider(request.getProvider())
                    .approvalStatus("APPROVED")
                    .status(request.getStatus())
                    .userstamp(user)
                    .build();
            return companyRepository.save(company);
        }).map(this::toResponse).collect(Collectors.toList());
        auditTrailService.logAction("Bulk Added " + responses.size() + " Companies", "Bulk created " + responses.size() + " companies", username, user != null ? user.getRole().name() : "SYSTEM");
        return responses;
    }

    private CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .companyName(company.getCompanyName())
                .code(company.getCode())
                .provider(company.getProvider())
                .approvalStatus(company.getApprovalStatus())
                .status(company.getStatus())
                .address(company.getAddress())
                .userstamp(company.getUserstamp() != null ? company.getUserstamp().getUsername() : null)
                .dateCreated(company.getDateCreated())
                .build();
    }
}
