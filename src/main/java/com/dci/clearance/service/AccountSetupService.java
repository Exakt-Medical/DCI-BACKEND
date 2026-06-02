package com.dci.clearance.service;

import com.dci.clearance.entity.Branch;
import com.dci.clearance.entity.Company;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.BranchRepository;
import com.dci.clearance.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountSetupService {

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public void setupCompanyAndBranch(User user) {
        if (user.getCompanyCode() != null || user.getBranchRef() != null) return;

        String code = "CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String branchId = "BRN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") +
                " " + (user.getLastName() != null ? user.getLastName() : "");

        Company company = Company.builder()
                .companyName(fullName.trim() + " Company")
                .code(code)
                .email(user.getEmail())
                .provider("DCI")
                .address("")
                .approvalStatus("APPROVED")
                .status("ACTIVE")
                .availableVouchers(0)
                .userstamp(String.valueOf(user.getId()))
                .build();
        company = companyRepository.save(company);

        Branch branch = Branch.builder()
                .branchId(branchId)
                .branchName(fullName.trim() + " Branch")
                .company(company)
                .status("ACTIVE")
                .userstamp(String.valueOf(user.getId()))
                .build();
        branch = branchRepository.save(branch);

        user.setCompanyCode(code);
        user.setBranchRef(branchId);
    }
}