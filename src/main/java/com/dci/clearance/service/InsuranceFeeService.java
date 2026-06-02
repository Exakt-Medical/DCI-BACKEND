package com.dci.clearance.service;

import com.dci.clearance.dto.InsuranceFeeResponse;
import com.dci.clearance.entity.InsuranceFee;
import com.dci.clearance.repository.InsuranceFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InsuranceFeeService {

    private final InsuranceFeeRepository insuranceFeeRepository;

    public List<InsuranceFeeResponse> getAllFees() {
        return insuranceFeeRepository.findAll().stream().map(f -> {
            BigDecimal total = f.getPrescribedPremiumFee()
                    .add(f.getDst())
                    .add(f.getVat())
                    .add(f.getLgt())
                    .add(f.getValidationFee());
            return InsuranceFeeResponse.builder()
                    .insuranceCode(f.getInsuranceCode())
                    .prescribedPremiumFee(f.getPrescribedPremiumFee())
                    .dst(f.getDst())
                    .vat(f.getVat())
                    .lgt(f.getLgt())
                    .validationFee(f.getValidationFee())
                    .totalAmount(total)
                    .build();
        }).collect(Collectors.toList());
    }

    public InsuranceFeeResponse getFeeByCode(String insuranceCode) {
        InsuranceFee fee = insuranceFeeRepository.findByInsuranceCode(insuranceCode)
                .orElse(null);
        if (fee == null) return null;
        BigDecimal total = fee.getPrescribedPremiumFee()
                .add(fee.getDst())
                .add(fee.getVat())
                .add(fee.getLgt())
                .add(fee.getValidationFee());
        return InsuranceFeeResponse.builder()
                .insuranceCode(fee.getInsuranceCode())
                .prescribedPremiumFee(fee.getPrescribedPremiumFee())
                .dst(fee.getDst())
                .vat(fee.getVat())
                .lgt(fee.getLgt())
                .validationFee(fee.getValidationFee())
                .totalAmount(total)
                .build();
    }

    public InsuranceFee saveFee(InsuranceFee fee) {
        return insuranceFeeRepository.save(fee);
    }
}