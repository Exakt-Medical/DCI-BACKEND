package com.dci.clearance.service;

import com.dci.clearance.dto.AuditTrailRequest;
import com.dci.clearance.dto.AuditTrailResponse;
import com.dci.clearance.entity.AuditTrail;
import com.dci.clearance.repository.AuditTrailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditTrailService {

    private final AuditTrailRepository auditTrailRepository;

    public List<AuditTrailResponse> getAll() {
        return auditTrailRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AuditTrailResponse getById(Long id) {
        return auditTrailRepository.findById(id)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public AuditTrailResponse create(AuditTrailRequest request, String username, String userrole) {
        AuditTrail auditTrail = AuditTrail.builder()
                .auditTrailId(request.getAuditTrailId())
                .actionMade(request.getActionMade())
                .details(request.getDetails())
                .userstamp(username)
                .userrole(userrole)
                .build();

        auditTrail = auditTrailRepository.save(auditTrail);
        return toResponse(auditTrail);
    }

    @Transactional
    public void logAction(String actionMade, String details, String username, String userrole) {
        AuditTrail auditTrail = AuditTrail.builder()
                .actionMade(actionMade)
                .details(details)
                .userstamp(username)
                .userrole(userrole)
                .build();
        auditTrailRepository.save(auditTrail);
    }

    @Transactional
    public AuditTrailResponse update(Long id, AuditTrailRequest request, String username, String userrole) {
        AuditTrail auditTrail = auditTrailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audit trail not found"));

        auditTrail.setAuditTrailId(request.getAuditTrailId());
        auditTrail.setActionMade(request.getActionMade());
        auditTrail.setDetails(request.getDetails());
        auditTrail.setUserstamp(username);
        auditTrail.setUserrole(userrole);

        auditTrail = auditTrailRepository.save(auditTrail);
        return toResponse(auditTrail);
    }

    @Transactional
    public void delete(Long id) {
        auditTrailRepository.deleteById(id);
    }

    public List<String> getUniqueActions() {
        return auditTrailRepository.findDistinctActionMades();
    }

    public List<String> getUniqueUsers() {
        return auditTrailRepository.findDistinctUserstamps();
    }

    private AuditTrailResponse toResponse(AuditTrail auditTrail) {
        return AuditTrailResponse.builder()
                .id(auditTrail.getId())
                .auditTrailId(auditTrail.getAuditTrailId())
                .actionMade(auditTrail.getActionMade())
                .details(auditTrail.getDetails())
                .userstamp(auditTrail.getUserstamp())
                .userrole(auditTrail.getUserrole())
                .timestamp(auditTrail.getTimestamp())
                .build();
    }
}