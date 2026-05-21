package com.exakt.vvip.config;

import com.exakt.vvip.entity.*;
import com.exakt.vvip.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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

    @Override
    public void run(String... args) {
        initUsers();
        initInsuranceProducts();
        initInsuranceFees();
        initVehicles();
        initCompanies();
        initTransactions();
    }

    private void initUsers() {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.UserRole.ADMIN)
                    .email("exaktdev@exakt.com.ph")
                    .mfaEnabled(true)
                    .mfaVerified(false)
                    .active(true)
                    .firstName("Admin")
                    .lastName("User")
                    .build();
            userRepository.save(admin);

            User agent = User.builder()
                    .username("agent")
                    .password(passwordEncoder.encode("agent123"))
                    .role(User.UserRole.AGENT)
                    .email("exaktdev@exakt.com.ph")
                    .mfaEnabled(true)
                    .mfaVerified(false)
                    .active(true)
                    .firstName("Agent")
                    .lastName("User")
                    .build();
            userRepository.save(agent);
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
            companyRepository.save(Company.builder().code("PIC-001").name("Premier Insurance Corp")
                    .provider("LTO").status(Company.CompanyStatus.ACTIVE).branch("Main")
                    .address("123 Ayala Ave, Makati City").build());
            companyRepository.save(Company.builder().code("FGI-002").name("Fortune General Insurance")
                    .provider("LTO").status(Company.CompanyStatus.PENDING).branch("Cebu Branch")
                    .address("45 Colon St, Cebu City").build());
            companyRepository.save(Company.builder().code("MIC-003").name("Malayan Insurance Co.")
                    .provider("LTO").status(Company.CompanyStatus.ACTIVE).branch("Davao")
                    .address("78 JP Laurel Ave, Davao City").build());
            companyRepository.save(Company.builder().code("COC-004").name("Country Bankers Insurance")
                    .provider("LTO").status(Company.CompanyStatus.INACTIVE).branch("BGC")
                    .address("11th Ave, BGC, Taguig").build());
            companyRepository.save(Company.builder().code("SGI-005").name("Standard Insurance Co.")
                    .provider("LTO").status(Company.CompanyStatus.DECLINED).branch("QC Branch")
                    .address("12 Quezon Ave, QC").build());
            companyRepository.save(Company.builder().code("CAI-006").name("Charter Ping An Insurance")
                    .provider("LTO").status(Company.CompanyStatus.DEACTIVATED).branch("Ortigas")
                    .address("ADB Ave, Mandaluyong").build());
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
            transactionRepository.save(Transaction.builder()
                    .agent("Jose Bautista").company("Premier Insurance Corp").assuredName("Elena Ramos")
                    .authNo("AUTH-2026-00126").build());
            transactionRepository.save(Transaction.builder()
                    .agent("Carla Torres").company("Standard Insurance Co.").assuredName("Miguel Fernandez")
                    .authNo("AUTH-2026-00127").build());
        }
    }
}