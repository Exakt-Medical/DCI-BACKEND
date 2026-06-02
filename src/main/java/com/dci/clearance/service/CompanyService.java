package com.dci.clearance.service;

import com.dci.clearance.dto.CompanyRequest;
import com.dci.clearance.dto.CompanyResponse;
import com.dci.clearance.entity.Company;
import com.dci.clearance.entity.User;
import com.dci.clearance.generateVoucher.client.BillerooClient;
import com.dci.clearance.repository.CompanyRepository;
import com.dci.clearance.repository.UserRepository;
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
    private final BillerooClient billerooClient;

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

        String code = request.getCode() != null && !request.getCode().isBlank() ? request.getCode() : "CMP-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Company company = Company.builder()
                .companyName(request.getCompanyName())
                .email(request.getEmail())
                .code(code)
                .provider(request.getProvider())
                .approvalStatus(request.getApprovalStatus() != null ? request.getApprovalStatus() : "PENDING")
                .status(request.getStatus())
                .address(request.getAddress())
                    .userstamp(user != null ? String.valueOf(user.getId()) : null)
                    .dateCreated(new java.text.SimpleDateFormat("MMM. dd, yyyy hh:mm a").format(new java.util.Date()))
                    .build();
            company = companyRepository.save(company);
        
        try {
            billerooClient.syncCompany(company);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync company to Billeroo: " + e.getMessage());
        }

        auditTrailService.logAction("Add Company", "Added company: " + company.getCompanyName(), username, user != null ? user.getRole().name() : "SYSTEM");
        return toResponse(company);
    }

    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request, String username) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String oldName = company.getCompanyName();
        String oldCode = company.getCode();
        String oldEmail = company.getEmail();
        String oldApprovalStatus = company.getApprovalStatus();
        String oldStatus = company.getStatus();

        company.setCompanyName(request.getCompanyName());
        company.setCode(request.getCode() != null ? request.getCode() : company.getCode());
        company.setEmail(request.getEmail() != null ? request.getEmail() : company.getEmail());
        company.setProvider(request.getProvider());
        company.setApprovalStatus(request.getApprovalStatus() != null ? request.getApprovalStatus() : company.getApprovalStatus());
        company.setStatus(request.getStatus() != null ? request.getStatus() : company.getStatus());
        company.setAddress(request.getAddress());
        company.setUserstamp(String.valueOf(user.getId()));

        company = companyRepository.save(company);

        boolean needsSync = !java.util.Objects.equals(oldName, company.getCompanyName()) ||
                            !java.util.Objects.equals(oldCode, company.getCode()) ||
                            !java.util.Objects.equals(oldEmail, company.getEmail()) ||
                            !java.util.Objects.equals(oldStatus, company.getStatus());

        if (needsSync) {
            try {
                billerooClient.syncCompany(company);
            } catch (Exception e) {
                throw new RuntimeException("Failed to sync updated company to Billeroo: " + e.getMessage());
            }
        }

        if ("INACTIVE".equals(request.getStatus()) && "ACTIVE".equals(oldStatus)) {
            auditTrailService.logAction("Deactivate Company", "Deactivated company: " + company.getCompanyName(), username, user.getRole().name());
            return toResponse(company);
        }
        if ("ACTIVE".equals(request.getStatus()) && "INACTIVE".equals(oldStatus)) {
            auditTrailService.logAction("Activate Company", "Activated company: " + company.getCompanyName(), username, user.getRole().name());
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
    public void delete(Long id, String username) {
        Company company = companyRepository.findById(id).orElse(null);
        User user = userRepository.findByUsername(username).orElse(null);
        companyRepository.deleteById(id);
        if (company != null) {
            auditTrailService.logAction("Delete Company", "Deleted company: " + company.getCompanyName(), username, user != null ? user.getRole().name() : "SYSTEM");
        }
    }

    @Transactional
    public List<CompanyResponse> bulkCreate(List<CompanyRequest> requests, String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        List<CompanyResponse> responses = requests.stream().map(request -> {
            if (request.getStatus() == null) request.setStatus("ACTIVE");
            String code = request.getCode() != null && !request.getCode().isBlank() ? request.getCode() : "CMP-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            Company company = Company.builder()
                    .companyName(request.getCompanyName())
                    .email(request.getEmail())
                    .code(code)
                    .provider(request.getProvider())
                    .address(request.getAddress())
                    .approvalStatus("APPROVED")
                    .status(request.getStatus())
                .userstamp(user != null ? String.valueOf(user.getId()) : null)
                    .dateCreated(new java.text.SimpleDateFormat("MMM. dd, yyyy hh:mm a").format(new java.util.Date()))
                    .build();
            company = companyRepository.save(company);
            try {
                billerooClient.syncCompany(company);
            } catch (Exception e) {
                throw new RuntimeException("Failed to sync company in bulk create: " + e.getMessage());
            }
            return company;
        }).map(this::toResponse).collect(Collectors.toList());
        auditTrailService.logAction("Bulk Add Companies", "Bulk added " + responses.size() + " companies", username, user != null ? user.getRole().name() : "SYSTEM");
        return responses;
    }

    private CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .companyName(company.getCompanyName())
                .email(company.getEmail())
                .code(company.getCode())
                .provider(company.getProvider())
                .approvalStatus(company.getApprovalStatus())
                .status(company.getStatus())
                .address(company.getAddress())
                .userstamp(company.getUserstamp())
                .dateCreated(company.getDateCreated())
                .build();
    }
}
