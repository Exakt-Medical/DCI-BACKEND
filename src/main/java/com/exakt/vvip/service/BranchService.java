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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public List<BranchResponse> getAll() {
        return branchRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<BranchResponse> getByCompany(Long companyId) {
        return branchRepository.findByCompanyId(companyId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

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

        branch.setBranchId(request.getBranchId());
        branch.setBranchName(request.getBranchName());
        branch.setBranchShortname(request.getBranchShortname());
        branch.setCompany(company);
        branch.setIsactive(request.getIsactive() != null ? request.getIsactive() : branch.getIsactive());
        branch.setUserstamp(user);

        branch = branchRepository.save(branch);
        return toResponse(branch);
    }

    @Transactional
    public void delete(Long id) {
        branchRepository.deleteById(id);
    }

    @Transactional
    public List<BranchResponse> bulkCreate(List<BranchRequest> requests, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return requests.stream().map(request -> {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found for branch: " + request.getBranchId()));
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
