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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public List<CompanyResponse> getAll() {
        return companyRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

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
        return toResponse(company);
    }

    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request, String username) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        company.setCompanyId(request.getCompanyId());
        company.setCompanyName(request.getCompanyName());
        company.setCompanyShortname(request.getCompanyShortname());
        company.setApprovalStatus(request.getApprovalStatus() != null ? request.getApprovalStatus() : company.getApprovalStatus());
        company.setIsactive(request.getIsactive() != null ? request.getIsactive() : company.getIsactive());
        company.setUserstamp(user);

        company = companyRepository.save(company);
        return toResponse(company);
    }

    @Transactional
    public void delete(Long id) {
        companyRepository.deleteById(id);
    }

    @Transactional
    public List<CompanyResponse> bulkCreate(List<CompanyRequest> requests, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return requests.stream().map(request -> {
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
