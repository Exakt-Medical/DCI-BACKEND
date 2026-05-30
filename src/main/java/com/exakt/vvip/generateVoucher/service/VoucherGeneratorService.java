package com.exakt.vvip.generateVoucher.service;

import com.exakt.vvip.entity.Order;
import com.exakt.vvip.entity.Voucher;
import com.exakt.vvip.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherGeneratorService {

    private final VoucherRepository voucherRepository;
    private static final Random RANDOM = new Random();

    @Transactional
    public List<Voucher> generateVouchers(Order order, int quantity) {
        List<Voucher> vouchers = new ArrayList<>(quantity);

        for (int i = 0; i < quantity; i++) {
            String code = generateUniqueCode(order.getCompanyCode());

            vouchers.add(Voucher.builder()
                .voucherCode(code)
                .company(order.getCompany())
                .companyCode(order.getCompanyCode())
                .order(order)
                .tlpeTransactionId(order.getTlpeTransactionId())
                .merchantReference(order.getMerchantReferenceId())
                .paymentReference(order.getPaymentReference())
                .originalUser(order.getUser())
                .currentUser(order.getUser())
                .status("AVAILABLE")
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build());
        }

        return voucherRepository.saveAll(vouchers);
    }

    private String generateUniqueCode(String companyCode) {
        String code;
        int attempts = 0;
        do {
            if (attempts++ > 10) {
                throw new RuntimeException("Too many collisions for company: " + companyCode);
            }
            code = "BLR-" + companyCode + "-" + randomSegment(4) + "-" + randomSegment(4);
        } while (voucherRepository.existsByVoucherCode(code));
        return code;
    }

    private String randomSegment(int length) {
        String charset = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(charset.charAt(RANDOM.nextInt(charset.length())));
        }
        return sb.toString();
    }
}
