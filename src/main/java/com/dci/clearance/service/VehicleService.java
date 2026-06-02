package com.dci.clearance.service;

import com.dci.clearance.entity.Vehicle;
import com.dci.clearance.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public Optional<Vehicle> findVehicle(String mvFileNo, String plateNo, String engineNo, String chassisNo) {
        if (mvFileNo != null && !mvFileNo.isBlank()) {
            return vehicleRepository.findByMvFileNumber(mvFileNo);
        }
        if (plateNo != null && !plateNo.isBlank()) {
            return vehicleRepository.findByPlateNumber(plateNo);
        }
        if (engineNo != null && !engineNo.isBlank()) {
            return vehicleRepository.findByEngineNumber(engineNo);
        }
        if (chassisNo != null && !chassisNo.isBlank()) {
            return vehicleRepository.findByChassisNumber(chassisNo);
        }
        return Optional.empty();
    }

    public Vehicle save(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }
}