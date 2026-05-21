package com.exakt.vvip.service;

import com.exakt.vvip.entity.Transaction;
import com.exakt.vvip.entity.User;
import com.exakt.vvip.repository.TransactionRepository;
import com.exakt.vvip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Transaction createTransaction(String agent, String company, String assuredName,
                                          String mvFileNumber, String plateNumber, String policyNumber,
                                          String voucherCode, String premiumType, BigDecimal totalAmount,
                                          String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String authNo = "AUTH-" + java.time.Year.now().getValue() + "-" +
                String.format("%05d", System.currentTimeMillis() % 100000);

        Transaction tx = Transaction.builder()
                .agent(agent)
                .company(company)
                .assuredName(assuredName)
                .authNo(authNo)
                .dateCreated(LocalDateTime.now())
                .mvFileNumber(mvFileNumber)
                .plateNumber(plateNumber)
                .policyNumber(policyNumber)
                .voucherCode(voucherCode)
                .premiumType(premiumType)
                .totalAmount(totalAmount)
                .createdBy(user)
                .build();

        return transactionRepository.save(tx);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public List<Transaction> getRecentTransactions() {
        return transactionRepository.findAll().stream()
                .sorted((a, b) -> b.getDateCreated().compareTo(a.getDateCreated()))
                .limit(20)
                .collect(java.util.stream.Collectors.toList());
    }
}