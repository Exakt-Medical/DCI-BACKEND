package com.exakt.vvip.config;

import com.exakt.vvip.entity.*;
import com.exakt.vvip.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InsuranceProductRepository productRepository;
    private final InsuranceFeeRepository insuranceFeeRepository;
    private final VehicleRepository vehicleRepository;
    private final CompanyRepository companyRepository;
    private final TransactionRepository transactionRepository;
    private final RoleRepository roleRepository;
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        migrateSchema();
        fixTable("companies", List.of("id", "company_name", "code", "address", "status", "userstamp", "date_created"));
        fixTable("branches", List.of("id", "branch_id", "branch_name", "status", "company_id", "userstamp", "date_created"));
        fixTable("users", List.of("id", "username", "password", "user_id", "first_name", "last_name",
                "middle_initial", "ext_name", "email", "mobile", "status", "role",
                "branch_id", "manager_id", "userstamp", "date_created"));
        fixTable("roles", List.of("id", "role_id", "role_name"));
        ensureRoleColumnSize();
        initRoles();
        initUsers();
        initInsuranceProducts();
        initInsuranceFees();
        initVehicles();
        initCompanies();
        initTransactions();
    }

    private void migrateSchema() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // === COMPANIES ===
            // Add status column, migrate data from isactive, drop old columns
            try { stmt.execute("ALTER TABLE companies ADD COLUMN `status` varchar(20) DEFAULT 'ACTIVE'"); } catch (Exception ignored) {}
            try { stmt.execute("UPDATE companies SET `status` = 'ACTIVE' WHERE `isactive` = 1"); } catch (Exception ignored) {}
            try { stmt.execute("UPDATE companies SET `status` = 'INACTIVE' WHERE `isactive` = 0 OR `isactive` IS NULL"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE companies DROP COLUMN `company_shortname`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE companies DROP COLUMN `company_id`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE companies DROP COLUMN `name`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE companies DROP COLUMN `isactive`"); } catch (Exception ignored) {}

            // === BRANCHES ===
            try { stmt.execute("ALTER TABLE branches ADD COLUMN `status` varchar(20) DEFAULT 'ACTIVE'"); } catch (Exception ignored) {}
            try { stmt.execute("UPDATE branches SET `status` = 'ACTIVE' WHERE `isactive` = 1"); } catch (Exception ignored) {}
            try { stmt.execute("UPDATE branches SET `status` = 'INACTIVE' WHERE `isactive` = 0 OR `isactive` IS NULL"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE branches DROP COLUMN `branch_shortname`"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE branches DROP COLUMN `isactive`"); } catch (Exception ignored) {}

            // === USERS ===
            try { stmt.execute("ALTER TABLE users ADD COLUMN `mfa_enabled` tinyint(1) DEFAULT 0"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN `mfa_verified` tinyint(1) DEFAULT 0"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN `mfa_code` varchar(50) DEFAULT '000'"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN `mfa_code_expiry` varchar(50) DEFAULT ''"); } catch (Exception ignored) {}

            // Rename timestamp to date_created on companies
            try { stmt.execute("ALTER TABLE companies CHANGE COLUMN `timestamp` `date_created` varchar(50) DEFAULT NULL"); } catch (Exception ignored) {}
            // Rename timestamp to date_created on branches
            try { stmt.execute("ALTER TABLE branches CHANGE COLUMN `timestamp` `date_created` varchar(50) DEFAULT NULL"); } catch (Exception ignored) {}
            // Rename timestamp to date_created on users
            try { stmt.execute("ALTER TABLE users CHANGE COLUMN `timestamp` `date_created` varchar(50) DEFAULT NULL"); } catch (Exception ignored) {}
            // USERS: replace isactive with status column
            try { stmt.execute("ALTER TABLE users ADD COLUMN `status` varchar(20) DEFAULT 'ACTIVE'"); } catch (Exception ignored) {}
            try { stmt.execute("UPDATE users SET `status` = 'ACTIVE' WHERE `active` = 1"); } catch (Exception ignored) {}
            try { stmt.execute("UPDATE users SET `status` = 'INACTIVE' WHERE `active` = 0 OR `active` IS NULL"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users DROP COLUMN `active`"); } catch (Exception ignored) {}
            // USERS: drop is_sub_agent column
            try { stmt.execute("ALTER TABLE users DROP COLUMN `is_sub_agent`"); } catch (Exception ignored) {}
            // USERS: add mobile column
            try { stmt.execute("ALTER TABLE users ADD COLUMN `mobile` varchar(20) DEFAULT NULL"); } catch (Exception ignored) {}
            // USERS: add is_buy_voucher_allowed column
            try { stmt.execute("ALTER TABLE users ADD COLUMN `is_buy_voucher_allowed` tinyint(1) DEFAULT 1"); } catch (Exception ignored) {}
            // COMPANIES: add address column
            try { stmt.execute("ALTER TABLE companies ADD COLUMN `address` varchar(1000) DEFAULT NULL"); } catch (Exception ignored) {}
            // COMPANIES: add provider column
            try { stmt.execute("ALTER TABLE companies ADD COLUMN `provider` varchar(200) DEFAULT NULL"); } catch (Exception ignored) {}
            // BRANCHES: change company_id from BIGINT to VARCHAR(100) to reference companies.code
            try { stmt.execute("ALTER TABLE branches MODIFY COLUMN `company_id` varchar(100) DEFAULT NULL"); } catch (Exception ignored) {}
            // Fix orphan branch company_id values: map old numeric IDs to company codes
            try { stmt.execute("UPDATE branches b INNER JOIN companies c ON c.id = CAST(b.company_id AS UNSIGNED) SET b.company_id = c.code"); } catch (Exception ignored) {}
            // Delete branches whose company_id still doesn't match any company code
            try { stmt.execute("DELETE b FROM branches b LEFT JOIN companies c ON c.code = b.company_id WHERE c.code IS NULL AND b.company_id IS NOT NULL AND b.company_id != ''"); } catch (Exception ignored) {}
            // Fix invalid user roles: map non-standard roles to existing enum values
            try { stmt.execute("UPDATE users SET role = 'AGENT' WHERE role NOT IN ('ADMIN','MANAGER','AGENT','SUBAGENT','VIEWER','SUPPORT')"); } catch (Exception ignored) {}
            try { stmt.execute("UPDATE users SET role = 'SUBAGENT' WHERE role = 'SUB_AGENT'"); } catch (Exception ignored) {}
            // Increase audit_trail action_made column size
            try { stmt.execute("ALTER TABLE audit_trail MODIFY COLUMN `action_made` varchar(500) DEFAULT NULL"); } catch (Exception ignored) {}
            // Fix mfa_code_expiry column type (was incorrectly set as datetime in MySQL)
            try { stmt.execute("ALTER TABLE users MODIFY COLUMN `mfa_code_expiry` varchar(50) DEFAULT ''"); } catch (Exception ignored) {}
            // Convert all text columns to utf8mb4 to support special characters (ñ, etc.)
            try { stmt.execute("ALTER TABLE companies CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE branches CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE users CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE audit_trail CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"); } catch (Exception ignored) {}

        } catch (Exception ignored) {}
    }

    private void ensureRoleColumnSize() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE users MODIFY `role` varchar(20) NOT NULL DEFAULT 'AGENT'");
        } catch (Exception ignored) {}
    }

    private void fixTable(String tableName, List<String> excludeCols) {
        List<String> columns = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                String colName = rs.getString("COLUMN_NAME");
                String nullable = rs.getString("IS_NULLABLE");
                String def = rs.getString("COLUMN_DEF");
                if ("NO".equals(nullable) && (def == null || def.isEmpty()) && !excludeCols.contains(colName)) {
                    columns.add(colName);
                }
            }
        } catch (Exception e) {
            return;
        }
        if (columns.isEmpty()) return;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String col : columns) {
                try {
                    stmt.execute("ALTER TABLE " + tableName + " MODIFY `" + col + "` varchar(255) DEFAULT NULL");
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(Role.builder().roleId("ADMIN").roleName("Administrator").build());
            roleRepository.save(Role.builder().roleId("MANAGER").roleName("Manager").build());
            roleRepository.save(Role.builder().roleId("AGENT").roleName("Agent").build());
            roleRepository.save(Role.builder().roleId("SUBAGENT").roleName("Sub-Agent").build());
            roleRepository.save(Role.builder().roleId("VIEWER").roleName("Viewer").build());
            roleRepository.save(Role.builder().roleId("SUPPORT").roleName("Support").build());
        }
    }

    private void initUsers() {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .userId("ADM-001")
                    .firstName("Admin")
                    .lastName("User")
                    .email("exaktdev@exakt.com.ph")
                    .role(User.UserRole.ADMIN)
                    .status("ACTIVE")
                    .mobile("09171234567")
                    .build();
            userRepository.save(admin);

            User agent = User.builder()
                    .username("agent")
                    .password(passwordEncoder.encode("agent123"))
                    .userId("AGT-001")
                    .firstName("Agent")
                    .lastName("User")
                    .email("exaktdev@exakt.com.ph")
                    .role(User.UserRole.AGENT)
                    .status("ACTIVE")
                    .mobile("09171234567")
                    .build();
            userRepository.save(agent);

            User manager = User.builder()
                    .username("manager")
                    .password(passwordEncoder.encode("manager123"))
                    .userId("MGR-001")
                    .firstName("Manager")
                    .lastName("User")
                    .email("exaktdev@exakt.com.ph")
                    .role(User.UserRole.MANAGER)
                    .status("ACTIVE")
                    .mobile("09171234567")
                    .build();
            userRepository.save(manager);

            User subagent = User.builder()
                    .username("subagent")
                    .password(passwordEncoder.encode("subagent123"))
                    .userId("SUB-001")
                    .firstName("Sub")
                    .lastName("Agent")
                    .email("exaktdev@exakt.com.ph")
                    .role(User.UserRole.SUBAGENT)
                    .status("ACTIVE")
                    .mobile("09171234567")
                    .build();
            userRepository.save(subagent);

            User viewer = User.builder()
                    .username("viewer")
                    .password(passwordEncoder.encode("viewer123"))
                    .userId("VWR-001")
                    .firstName("Viewer")
                    .lastName("User")
                    .email("exaktdev@exakt.com.ph")
                    .role(User.UserRole.VIEWER)
                    .status("ACTIVE")
                    .mobile("09171234567")
                    .build();
            userRepository.save(viewer);
        }

        if (!userRepository.findByUsername("support").isPresent()) {
            User support = User.builder()
                    .username("support")
                    .password(passwordEncoder.encode("support123"))
                    .userId("SUP-001")
                    .firstName("Support")
                    .lastName("User")
                    .email("exaktdev@exakt.com.ph")
                    .role(User.UserRole.SUPPORT)
                    .status("ACTIVE")
                    .mobile("09171234567")
                    .build();
            userRepository.save(support);
        }
    }

    private void initInsuranceProducts() {
        if (productRepository.count() == 0) {
            productRepository.save(InsuranceProduct.builder()
                    .productName("Basic CTPL").coverage("Third Party Liability")
                    .price(new BigDecimal("560.00"))
                    .description("Basic coverage for third party liability as required by LTO")
                    .validityDays(365).insuranceCode("PRIVATE CARS (INCLUDING JEEPS AND AUVS)").isActive(true).build());
            productRepository.save(InsuranceProduct.builder()
                    .productName("Premium CTPL").coverage("Third Party Liability + Personal Accident")
                    .price(new BigDecimal("850.00"))
                    .description("Enhanced coverage with personal accident insurance for driver")
                    .validityDays(365).insuranceCode("PRIVATE CARS (INCLUDING JEEPS AND AUVS)").isActive(true).build());
            productRepository.save(InsuranceProduct.builder()
                    .productName("Motorcycle CTPL").coverage("Third Party Liability for Motorcycles")
                    .price(new BigDecimal("350.00"))
                    .description("Affordable CTPL coverage for motorcycles")
                    .validityDays(365).insuranceCode("MOTORCYCLES").isActive(true).build());
            productRepository.save(InsuranceProduct.builder()
                    .productName("Commercial Vehicle CTPL").coverage("Third Party Liability for Commercial Vehicles")
                    .price(new BigDecimal("1200.00"))
                    .description("Comprehensive CTPL for commercial vehicles and fleets")
                    .validityDays(365).insuranceCode("COMMERCIAL VEHICLES").isActive(true).build());
            productRepository.save(InsuranceProduct.builder()
                    .productName("Heavy Equipment CTPL").coverage("Third Party Liability for Heavy Equipment")
                    .price(new BigDecimal("1800.00"))
                    .description("Specialized coverage for heavy equipment and machinery")
                    .validityDays(365).insuranceCode("HEAVY EQUIPMENT").isActive(true).build());
            productRepository.save(InsuranceProduct.builder()
                    .productName("Public Utility CTPL").coverage("Third Party Liability for PUVs")
                    .price(new BigDecimal("1450.00"))
                    .description("CTPL coverage for public utility vehicles, taxis, and jeepneys")
                    .validityDays(365).insuranceCode("TAXI/PUBLIC UTILITY VEHICLES").isActive(true).build());
        }
    }

    private void initInsuranceFees() {
        if (insuranceFeeRepository.count() == 0) {
            insuranceFeeRepository.save(InsuranceFee.builder()
                    .insuranceCode("PRIVATE CARS (INCLUDING JEEPS AND AUVS)")
                    .prescribedPremiumFee(new BigDecimal("449.40")).dst(new BigDecimal("56.18"))
                    .vat(new BigDecimal("53.93")).lgt(new BigDecimal("0.90"))
                    .validationFee(new BigDecimal("80.40")).build());
            insuranceFeeRepository.save(InsuranceFee.builder()
                    .insuranceCode("MOTORCYCLES")
                    .prescribedPremiumFee(new BigDecimal("287.00")).dst(new BigDecimal("35.88"))
                    .vat(new BigDecimal("34.44")).lgt(new BigDecimal("0.57"))
                    .validationFee(new BigDecimal("80.40")).build());
            insuranceFeeRepository.save(InsuranceFee.builder()
                    .insuranceCode("COMMERCIAL VEHICLES")
                    .prescribedPremiumFee(new BigDecimal("825.00")).dst(new BigDecimal("103.13"))
                    .vat(new BigDecimal("99.00")).lgt(new BigDecimal("1.65"))
                    .validationFee(new BigDecimal("80.40")).build());
            insuranceFeeRepository.save(InsuranceFee.builder()
                    .insuranceCode("HEAVY EQUIPMENT")
                    .prescribedPremiumFee(new BigDecimal("1250.00")).dst(new BigDecimal("156.25"))
                    .vat(new BigDecimal("150.00")).lgt(new BigDecimal("2.50"))
                    .validationFee(new BigDecimal("80.40")).build());
            insuranceFeeRepository.save(InsuranceFee.builder()
                    .insuranceCode("TAXI/PUBLIC UTILITY VEHICLES")
                    .prescribedPremiumFee(new BigDecimal("975.00")).dst(new BigDecimal("121.88"))
                    .vat(new BigDecimal("117.00")).lgt(new BigDecimal("1.95"))
                    .validationFee(new BigDecimal("80.40")).build());
        }
    }

    private void initVehicles() {
        if (vehicleRepository.count() == 0) {
            vehicleRepository.save(Vehicle.builder()
                    .mvFileNumber("MV-2019-00456789").plateNumber("ABC 1234")
                    .engineNumber("4K-E123456").chassisNumber("JTDBE33K7Y0123456")
                    .make("Toyota").series("Vios 1.3 E").color("Pearl White").yearModel("2019")
                    .classification("Private").bodyType("Sedan")
                    .vehicleCategory("Passenger Car").vehicleType("Sedan")
                    .lastRegistrationDate("December 31, 2024")
                    .ownerFirstName("Juan").ownerLastName("Dela Cruz").ownerMiddleName("Santos")
                    .ownerAddress("123 Rizal St, San Juan, Metro Manila")
                    .ownerContactNo("09171234567").ownerEmail("juan.delacruz@email.com")
                    .ownerTin("123-456-789-000").build());
        }
    }

    private void initCompanies() {
        if (companyRepository.count() == 0) {
            companyRepository.save(Company.builder()
                    .code("PIC").companyName("Premier Insurance Corp")
                    .status("ACTIVE").build());
            companyRepository.save(Company.builder()
                    .code("FGI").companyName("Fortune General Insurance")
                    .status("ACTIVE").build());
            companyRepository.save(Company.builder()
                    .code("MIC").companyName("Malayan Insurance Co.")
                    .status("ACTIVE").build());
        }
    }

    private void initTransactions() {
        if (transactionRepository.count() == 0) {
            transactionRepository.save(Transaction.builder()
                    .agent("Maria Santos").company("Premier Insurance Corp").assuredName("Juan dela Cruz")
                    .authNo("AUTH-2026-00123").build());
            transactionRepository.save(Transaction.builder()
                    .agent("Pedro Reyes").company("Malayan Insurance Co.").assuredName("Rosa Magsaysay")
                    .authNo("AUTH-2026-00124").build());
            transactionRepository.save(Transaction.builder()
                    .agent("Ana Lim").company("Fortune General Insurance").assuredName("Carlos Aquino")
                    .authNo("AUTH-2026-00125").build());
        }
    }
}
