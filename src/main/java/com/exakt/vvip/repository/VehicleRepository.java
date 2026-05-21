package com.exakt.vvip.repository;

import com.exakt.vvip.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByMvFileNumber(String mvFileNumber);
    Optional<Vehicle> findByPlateNumber(String plateNumber);
    Optional<Vehicle> findByEngineNumber(String engineNumber);
    Optional<Vehicle> findByChassisNumber(String chassisNumber);
}