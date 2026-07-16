package com.dci.clearance.config;

import com.dci.clearance.entity.*;
import com.dci.clearance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        migrateSchema();
        initRoles();
        initUsers();
        // initCompaniesAndBranchesForExistingUsers();
    }

    private void migrateSchema() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            try { stmt.execute("ALTER TABLE users MODIFY `role` varchar(20) NOT NULL DEFAULT 'CITIZEN'"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users DROP COLUMN `middle_initial`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users DROP COLUMN `ext_name`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users DROP COLUMN `user_id`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users DROP COLUMN `mfa_enabled`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users DROP COLUMN `mfa_verified`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users DROP COLUMN `mfa_code`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users DROP COLUMN `mfa_code_expiry`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users DROP COLUMN `is_buy_voucher_allowed`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users DROP FOREIGN KEY IF EXISTS `FK_users_branch`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users DROP COLUMN `branch_id`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users DROP FOREIGN KEY IF EXISTS `FK_users_manager`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users DROP COLUMN `manager_id`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN `company_code` varchar(100) DEFAULT NULL"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN `branch_ref` varchar(50) DEFAULT NULL"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE access_trail ADD COLUMN `role` varchar(50) DEFAULT NULL"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE access_trail ADD COLUMN `action` varchar(20) DEFAULT NULL"); } catch (Exception ignored) {}

            try { stmt.execute("ALTER TABLE users ADD COLUMN `email_verified` BOOLEAN NOT NULL DEFAULT FALSE"); } catch (Exception ignored) {}
            try { stmt.execute("UPDATE users SET email_verified = TRUE WHERE email IS NOT NULL AND email != ''"); } catch (Exception ignored) {}

            // Create email_verification_tokens table if not exists
            try {
                stmt.execute("CREATE TABLE IF NOT EXISTS email_verification_tokens (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "token VARCHAR(128) NOT NULL UNIQUE, " +
                        "user_id BIGINT NOT NULL, " +
                        "expiry_date DATETIME NOT NULL, " +
                        "used BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "date_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                        ")");
            } catch (Exception ignored) {}

            // Create user_notifications table if not exists
            try {
                stmt.execute("CREATE TABLE IF NOT EXISTS user_notifications (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "user_id BIGINT NOT NULL, " +
                        "title VARCHAR(200) NOT NULL, " +
                        "message VARCHAR(500) NOT NULL, " +
                        "type VARCHAR(50) NOT NULL, " +
                        "reference_id BIGINT DEFAULT NULL, " +
                        "is_read BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "date_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                        ")");
            } catch (Exception ignored) {}

        } catch (Exception ignored) {}
    }

    private void initRoles() {
        if (roleRepository.findByRoleId("CITIZEN").isEmpty()) {
            roleRepository.save(Role.builder().roleId("CITIZEN").roleName("Citizen").build());
        }
        if (roleRepository.findByRoleId("AGENT_FIXER").isEmpty()) {
            roleRepository.save(Role.builder().roleId("AGENT_FIXER").roleName("Agent/Fixer").build());
        }
        if (roleRepository.findByRoleId("HPG").isEmpty()) {
            roleRepository.save(Role.builder().roleId("HPG").roleName("Highway Patrol Group").build());
        }
        if (roleRepository.findByRoleId("DCI").isEmpty()) {
            roleRepository.save(Role.builder().roleId("DCI").roleName("DCI Officer").build());
        }
        if (roleRepository.findByRoleId("ADMIN").isEmpty()) {
            roleRepository.save(Role.builder().roleId("ADMIN").roleName("System Administrator").build());
        }
        if (roleRepository.findByRoleId("LTO").isEmpty()) {
            roleRepository.save(Role.builder().roleId("LTO").roleName("Land Transportation Office").build());
        }
    }

    private void initUsers() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@dci.gov.ph")
                    .role(User.UserRole.ADMIN)
                    .status("ACTIVE")
                    .build());
        }

        if (!userRepository.existsByUsername("citizen")) {
            userRepository.save(User.builder()
                    .username("citizen")
                    .password(passwordEncoder.encode("citizen123"))
                    .firstName("Juan")
                    .lastName("Dela Cruz")
                    .email("juan@email.com")
                    .role(User.UserRole.CITIZEN)
                    .status("ACTIVE")
                    .build());
        }

        if (!userRepository.existsByUsername("agent")) {
            userRepository.save(User.builder()
                    .username("agent")
                    .password(passwordEncoder.encode("agent123"))
                    .firstName("Agent")
                    .lastName("Fixer")
                    .email("agent@dci.gov.ph")
                    .role(User.UserRole.AGENT_FIXER)
                    .status("ACTIVE")
                    .build());
        }

        if (!userRepository.existsByUsername("hpg")) {
            userRepository.save(User.builder()
                    .username("hpg")
                    .password(passwordEncoder.encode("hpg123"))
                    .firstName("HPG")
                    .lastName("Officer")
                    .email("hpg@dci.gov.ph")
                    .role(User.UserRole.HPG)
                    .status("ACTIVE")
                    .build());
        }

        if (!userRepository.existsByUsername("dci")) {
            userRepository.save(User.builder()
                    .username("dci")
                    .password(passwordEncoder.encode("dci123"))
                    .firstName("DCI")
                    .lastName("Officer")
                    .email("dci@dci.gov.ph")
                    .role(User.UserRole.DCI)
                    .status("ACTIVE")
                    .build());
        }

        if (!userRepository.existsByUsername("lto")) {
            userRepository.save(User.builder()
                    .username("lto")
                    .password(passwordEncoder.encode("lto123"))
                    .firstName("LTO")
                    .lastName("Officer")
                    .email("lto@dci.gov.ph")
                    .role(User.UserRole.LTO)
                    .status("ACTIVE")
                    .build());
        }
    }

    // private void initCompaniesAndBranchesForExistingUsers() {
    //     List<User> users = userRepository.findAll();
    //     for (User user : users) {
    //         if (user.getCompanyCode() != null && user.getBranchRef() != null) continue;
    //         if (user.getRole() == User.UserRole.HPG || user.getRole() == User.UserRole.DCI) continue;

    //         String code = "CMP-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    //         String branchId = "BRN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    //         String fullName = (user.getFirstName() != null ? user.getFirstName() : "") +
    //                 " " + (user.getLastName() != null ? user.getLastName() : "");

    //         Company company = Company.builder()
    //                 .companyName(fullName.trim() + " Company")
    //                 .code(code)
    //                 .email(user.getEmail())
    //                 .provider("DCI")
    //                 .address("")
    //                 .approvalStatus("APPROVED")
    //                 .status("ACTIVE")
    //                 .availableVouchers(0)
    //                 .userstamp(String.valueOf(user.getId()))
    //                 .build();
    //         companyRepository.save(company);

    //         Branch branch = Branch.builder()
    //                 .branchId(branchId)
    //                 .branchName(fullName.trim() + " Branch")
    //                 .company(company)
    //                 .status("ACTIVE")
    //                 .userstamp(String.valueOf(user.getId()))
    //                 .build();
    //         branchRepository.save(branch);

    //         user.setCompanyCode(code);
    //         user.setBranchRef(branchId);
    //         userRepository.save(user);
    //     }
    // }
}