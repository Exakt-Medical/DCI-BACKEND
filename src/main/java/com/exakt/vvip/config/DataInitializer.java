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
        fixTable("companies", List.of("id", "company_id", "company_name", "company_shortname",
                "code", "name", "isactive", "userstamp", "timestamp"));
        fixTable("users", List.of("id", "username", "password", "user_id", "first_name", "last_name",
                "middle_initial", "ext_name", "email", "active", "role", "branch_id", "manager_id",
                "is_sub_agent", "userstamp", "timestamp"));
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
                    .isactive(true)
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
                    .isactive(true)
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
                    .isactive(true)
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
                    .isactive(true)
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
                    .isactive(true)
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
                    .isactive(true)
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
                    .companyId("PIC-001").code("PIC").companyName("Premier Insurance Corp")
                    .companyShortname("PIC").isactive(true).build());
            companyRepository.save(Company.builder()
                    .companyId("FGI-002").code("FGI").companyName("Fortune General Insurance")
                    .companyShortname("FGI").isactive(true).build());
            companyRepository.save(Company.builder()
                    .companyId("MIC-003").code("MIC").companyName("Malayan Insurance Co.")
                    .companyShortname("MIC").isactive(true).build());
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
