package com.dci.clearance.service;

import com.dci.clearance.entity.Branch;
import com.dci.clearance.entity.Company;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.BranchRepository;
import com.dci.clearance.repository.CompanyRepository;
import com.dci.clearance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Handles post-registration account setup.
 *
 * <ul>
 *   <li><b>CITIZEN role</b> — creates a shadow company (no branch) using the HPG flow
 *       and delegates Billeroo sync to {@link BillerooCompanySyncService}.</li>
 *   <li><b>Non-citizen roles</b> — retains the original CTPL company + branch setup.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountSetupService {

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final BillerooCompanySyncService billerooCompanySyncService;

    private static final String ALPHA_NUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_CODE_GENERATION_ATTEMPTS = 5;

    // ──────────────────────────────────────────────────────────────────────────
    // Public entry point — forks by role
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public void setupCompanyAndBranch(User user) {
        if (user.getRole() == User.UserRole.CITIZEN || user.getRole() == User.UserRole.AGENT_FIXER) {
            setupCitizenShadowCompany(user);
        } else {
            setupCtplCompanyAndBranch(user);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // HPG citizen shadow-company path
    // ──────────────────────────────────────────────────────────────────────────

    private void setupCitizenShadowCompany(User user) {
        if (user.getCompanyCode() != null) return;

        // 1. Generate a unique shadow company code
        String code = generateShadowCompanyCode();

        // 2. Build company name: "{firstName} {lastName} {code}"
        String fullName = buildFullName(user.getFirstName(), user.getLastName());
        String companyName = fullName + " " + code;

        // 3. Insert shadow company row BEFORE Billeroo sync (local record must always exist)
        Company shadowCompany = Company.builder()
                .companyName(companyName)
                .code(code)
                .email(user.getEmail())
                .provider("DCI")
                .address("")
                .approvalStatus("APPROVED")
                .status("ACTIVE")
                .availableVouchers(0)
                .billerooSyncStatus("PENDING")
                .userstamp(String.valueOf(user.getId()))
                .build();
        companyRepository.save(shadowCompany);

        // 4. Attach company code to user
        user.setCompanyCode(code);
        // Branch is NOT created for citizens — leave branchRef null

        // 5. Attempt Billeroo sync
        try {
            billerooCompanySyncService.sync(code, user.getEmail(), companyName);
            user.setBillerooSyncStatus("SYNCED");
            shadowCompany.setBillerooSyncStatus("SYNCED");
            companyRepository.save(shadowCompany);
            log.info("Citizen shadow company synced to Billeroo: userId={}, code={}", user.getId(), code);
        } catch (Exception e) {
            user.setBillerooSyncStatus("FAILED");
            shadowCompany.setBillerooSyncStatus("FAILED");
            companyRepository.save(shadowCompany);
            log.warn("Billeroo sync failed during citizen registration for userId={}, code={}. "
                    + "Retry job will pick this up. Error: {}", user.getId(), code, e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Original CTPL path (non-citizen roles: ADMIN, AGENT_FIXER, HPG, LTO)
    // ──────────────────────────────────────────────────────────────────────────

    private void setupCtplCompanyAndBranch(User user) {
        if (user.getCompanyCode() != null || user.getBranchRef() != null) return;

        String code = "CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String branchId = "BRN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String fullName = buildFullName(user.getFirstName(), user.getLastName());

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

    // ──────────────────────────────────────────────────────────────────────────
    // Shadow company code generation: 3 uppercase alphanumeric characters
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Generates a unique company code of exactly 3 characters (A-Z, 0-9)
     * to comply with Billeroo's strict length limit.
     * <p>
     * Note: A 3-character Base36 space only yields 46,656 unique combinations.
     * We use a larger retry loop to find an unused code.
     */
    private String generateShadowCompanyCode() {
        for (int attempt = 0; attempt < 50; attempt++) {
            String code = generateRandom3();

            // Check uniqueness against both tables
            if (!userRepository.existsByCompanyCode(code) && !companyRepository.existsByCode(code)) {
                return code;
            }
            log.warn("Shadow company code collision detected: {} (attempt {}/{})", code, attempt + 1, 50);
        }

        // Space is exhausted or too dense
        log.error("Failed to generate a unique 3-character company code after 50 attempts. Keyspace might be exhausted.");
        throw new RuntimeException("System capacity reached: Unable to generate unique company code.");
    }

    private String generateRandom3() {
        StringBuilder sb = new StringBuilder(3);
        for (int i = 0; i < 3; i++) {
            sb.append(ALPHA_NUM.charAt(RANDOM.nextInt(ALPHA_NUM.length())));
        }
        return sb.toString();
    }

    private String generateRandom6() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHA_NUM.charAt(RANDOM.nextInt(ALPHA_NUM.length())));
        }
        return sb.toString();
    }

    private String buildFullName(String firstName, String lastName) {
        String fn = firstName != null ? firstName : "";
        String ln = lastName != null ? lastName : "";
        return (fn + " " + ln).trim();
    }
}