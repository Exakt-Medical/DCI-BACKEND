package com.dci.clearance.service;

import com.dci.clearance.dto.BranchRequest;
import com.dci.clearance.dto.BranchResponse;
import com.dci.clearance.entity.Branch;
import com.dci.clearance.entity.Company;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.BranchRepository;
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
public class BranchService {

    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final AuditTrailService auditTrailService;

    @Transactional(readOnly = true)
    public List<BranchResponse> getAll() {
        return branchRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getByCompany(Long companyId) {
        return branchRepository.findByCompanyId(companyId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BranchResponse getById(Long id) {
        return branchRepository.findById(id)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public BranchResponse create(BranchRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String companyCode = request.getCompanyCode() != null ? request.getCompanyCode() : String.valueOf(request.getCompanyId());
        Company company = companyRepository.findByCode(companyCode)
                .orElseThrow(() -> new RuntimeException("Company not found for code: " + companyCode));

        Branch branch = Branch.builder()
                .branchId(request.getBranchId())
                .branchName(request.getBranchName())
                .company(company)
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .userstamp(String.valueOf(user.getId()))
                .dateCreated(new java.text.SimpleDateFormat("MMM. dd, yyyy hh:mm a").format(new java.util.Date()))
                .build();

        branch = branchRepository.save(branch);
        auditTrailService.logAction("Add Branch", "Added branch: " + branch.getBranchName() + " for " + company.getCompanyName(), username, user.getRole().name());
        return toResponse(branch);
    }

    @Transactional
    public BranchResponse update(Long id, BranchRequest request, String username) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String companyCode = request.getCompanyCode() != null ? request.getCompanyCode() : String.valueOf(request.getCompanyId());
        Company company = companyRepository.findByCode(companyCode)
                .orElseThrow(() -> new RuntimeException("Company not found for code: " + companyCode));

        String oldBranchId = branch.getBranchId();
        String oldName = branch.getBranchName();
        String oldCompanyName = branch.getCompany().getCompanyName();
        String oldStatus = branch.getStatus();

        branch.setBranchId(request.getBranchId());
        branch.setBranchName(request.getBranchName());
        branch.setCompany(company);
        branch.setStatus(request.getStatus() != null ? request.getStatus() : branch.getStatus());
        branch.setUserstamp(String.valueOf(user.getId()));

        branch = branchRepository.save(branch);

        if ("INACTIVE".equals(request.getStatus()) && "ACTIVE".equals(oldStatus)) {
            auditTrailService.logAction("Deactivated Branch " + branch.getBranchName(), "Set Branch " + branch.getBranchName() + " to inactive", username, user.getRole().name());
            return toResponse(branch);
        }
        if ("ACTIVE".equals(request.getStatus()) && "INACTIVE".equals(oldStatus)) {
            auditTrailService.logAction("Activate Branch", "Activated branch: " + branch.getBranchName(), username, user.getRole().name());
            return toResponse(branch);
        }
        List<String> changes = new ArrayList<>();
        if (!oldBranchId.equals(request.getBranchId())) {
            changes.add("Branch ID from '" + oldBranchId + "' to '" + request.getBranchId() + "'");
        }
        if (!oldName.equals(request.getBranchName())) {
            changes.add("Name from '" + oldName + "' to '" + request.getBranchName() + "'");
        }
        if (!oldCompanyName.equals(company.getCompanyName())) {
            changes.add("Company from '" + oldCompanyName + "' to '" + company.getCompanyName() + "'");
        }
        String actionMade, details;
        if (changes.isEmpty()) {
            actionMade = "Edited Branch " + branch.getBranchName();
            details = "No changes detected";
        } else {
            actionMade = "Edited Branch " + branch.getBranchName() + " (" + String.join("; ", changes) + ")";
            details = String.join("; ", changes);
        }
        auditTrailService.logAction(actionMade, details, username, user.getRole().name());
        return toResponse(branch);
    }

    @Transactional
    public void delete(Long id, String username) {
        Branch branch = branchRepository.findById(id).orElse(null);
        User user = userRepository.findByUsername(username).orElse(null);
        branchRepository.deleteById(id);
        if (branch != null) {
            auditTrailService.logAction("Delete Branch", "Deleted branch: " + branch.getBranchName(), username, user != null ? user.getRole().name() : "SYSTEM");
        }
    }

    @Transactional
    public List<BranchResponse> bulkCreate(List<BranchRequest> requests, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<BranchResponse> responses = requests.stream().map(request -> {
            Company company;
            if (request.getCompanyCode() != null && !request.getCompanyCode().isBlank()) {
                company = companyRepository.findByCode(request.getCompanyCode())
                        .orElseThrow(() -> new RuntimeException("Company not found for code '" + request.getCompanyCode() + "' for branch: " + request.getBranchId()));
            } else if (request.getCompanyId() != null) {
                company = companyRepository.findById(request.getCompanyId())
                        .orElseThrow(() -> new RuntimeException("Company not found for id " + request.getCompanyId() + " for branch: " + request.getBranchId()));
            } else {
                throw new RuntimeException("companyId or companyCode is required for branch " + (request.getBranchId() != null ? "'" + request.getBranchId() + "'" : "") + ". Please check your CSV.");
            }
            Branch branch = Branch.builder()
                    .branchId(request.getBranchId())
                    .branchName(request.getBranchName())
                    .company(company)
                    .status("ACTIVE")
                    .userstamp(String.valueOf(user.getId()))
                    .dateCreated(new java.text.SimpleDateFormat("MMM. dd, yyyy hh:mm a").format(new java.util.Date()))
                    .build();
            return branchRepository.save(branch);
        }).map(this::toResponse).collect(Collectors.toList());
        auditTrailService.logAction("Bulk Add Branches", "Bulk added " + responses.size() + " branches", username, user.getRole().name());
        return responses;
    }

    private BranchResponse toResponse(Branch branch) {
        Company c = branch.getCompany();
        return BranchResponse.builder()
                .id(branch.getId())
                .branchId(branch.getBranchId())
                .branchName(branch.getBranchName())
                .companyId(c != null ? c.getId() : null)
                .companyCode(c != null ? c.getCode() : null)
                .companyName(c != null ? c.getCompanyName() : null)
                .companyProvider(c != null ? c.getProvider() : null)
                .status(branch.getStatus())
                .userstamp(branch.getUserstamp())
                .dateCreated(branch.getDateCreated())
                .build();
    }
}
