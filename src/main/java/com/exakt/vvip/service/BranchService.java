package com.exakt.vvip.service;

import com.exakt.vvip.dto.BranchRequest;
import com.exakt.vvip.dto.BranchResponse;
import com.exakt.vvip.entity.Branch;
import com.exakt.vvip.entity.Company;
import com.exakt.vvip.entity.User;
import com.exakt.vvip.repository.BranchRepository;
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
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        Branch branch = Branch.builder()
                .branchId(request.getBranchId())
                .branchName(request.getBranchName())
                .branchShortname(request.getBranchShortname())
                .company(company)
                .isactive(request.getIsactive() != null ? request.getIsactive() : true)
                .userstamp(user)
                .build();

        branch = branchRepository.save(branch);
        auditTrailService.logAction("Added a New Branch " + branch.getBranchName() + " for " + company.getCompanyName(), "Created branch '" + branch.getBranchName() + "' for " + company.getCompanyName(), username, user.getRole().name());
        return toResponse(branch);
    }

    @Transactional
    public BranchResponse update(Long id, BranchRequest request, String username) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        String oldBranchId = branch.getBranchId();
        String oldName = branch.getBranchName();
        String oldShortname = branch.getBranchShortname();
        String oldCompanyName = branch.getCompany().getCompanyName();
        Boolean oldActive = branch.getIsactive();

        branch.setBranchId(request.getBranchId());
        branch.setBranchName(request.getBranchName());
        branch.setBranchShortname(request.getBranchShortname());
        branch.setCompany(company);
        branch.setIsactive(request.getIsactive() != null ? request.getIsactive() : branch.getIsactive());
        branch.setUserstamp(user);

        branch = branchRepository.save(branch);

        if (Boolean.FALSE.equals(request.getIsactive()) && Boolean.TRUE.equals(oldActive)) {
            auditTrailService.logAction("Deactivated Branch " + branch.getBranchName(), "Set Branch " + branch.getBranchName() + " to inactive", username, user.getRole().name());
            return toResponse(branch);
        }
        if (Boolean.TRUE.equals(request.getIsactive()) && Boolean.FALSE.equals(oldActive)) {
            auditTrailService.logAction("Activated Branch " + branch.getBranchName(), "Set Branch " + branch.getBranchName() + " to active", username, user.getRole().name());
            return toResponse(branch);
        }
        List<String> changes = new ArrayList<>();
        if (!oldBranchId.equals(request.getBranchId())) {
            changes.add("Branch ID from '" + oldBranchId + "' to '" + request.getBranchId() + "'");
        }
        if (!oldName.equals(request.getBranchName())) {
            changes.add("Name from '" + oldName + "' to '" + request.getBranchName() + "'");
        }
        if (!oldShortname.equals(request.getBranchShortname())) {
            changes.add("Short Name from '" + oldShortname + "' to '" + request.getBranchShortname() + "'");
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
    public void delete(Long id) {
        Branch branch = branchRepository.findById(id).orElse(null);
        branchRepository.deleteById(id);
        if (branch != null) {
            auditTrailService.logAction("Deleted Branch " + branch.getBranchName(), "Deleted branch '" + branch.getBranchName() + "'", "system", "SYSTEM");
        }
    }

    @Transactional
    public List<BranchResponse> bulkCreate(List<BranchRequest> requests, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<BranchResponse> responses = requests.stream().map(request -> {
            Company company;
            if (request.getCompanyCode() != null && !request.getCompanyCode().isBlank()) {
                company = companyRepository.findByCompanyId(request.getCompanyCode())
                        .orElseThrow(() -> new RuntimeException("Company not found for companyId '" + request.getCompanyCode() + "' for branch: " + request.getBranchId()));
            } else if (request.getCompanyId() != null) {
                company = companyRepository.findById(request.getCompanyId())
                        .orElseThrow(() -> new RuntimeException("Company not found for id " + request.getCompanyId() + " for branch: " + request.getBranchId()));
            } else {
                throw new RuntimeException("companyId or companyCode is required for branch " + (request.getBranchId() != null ? "'" + request.getBranchId() + "'" : "") + ". Please check your CSV.");
            }
            Branch branch = Branch.builder()
                    .branchId(request.getBranchId())
                    .branchName(request.getBranchName())
                    .branchShortname(request.getBranchShortname())
                    .company(company)
                    .isactive(true)
                    .userstamp(user)
                    .build();
            return branchRepository.save(branch);
        }).map(this::toResponse).collect(Collectors.toList());
        auditTrailService.logAction("Bulk Added " + responses.size() + " Branches", "Bulk created " + responses.size() + " branches", username, user.getRole().name());
        return responses;
    }

    private BranchResponse toResponse(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .branchId(branch.getBranchId())
                .branchName(branch.getBranchName())
                .branchShortname(branch.getBranchShortname())
                .companyId(branch.getCompany().getId())
                .companyName(branch.getCompany().getCompanyName())
                .isactive(branch.getIsactive())
                .userstamp(branch.getUserstamp() != null ? branch.getUserstamp().getUsername() : null)
                .timestamp(branch.getTimestamp())
                .build();
    }
}
