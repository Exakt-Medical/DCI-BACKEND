package com.dci.clearance.service;

import com.dci.clearance.dto.AccessTrailResponse;
import com.dci.clearance.entity.AccessTrail;
import com.dci.clearance.repository.AccessTrailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccessLogService {

    private final AccessTrailRepository accessTrailRepository;

    public List<AccessTrailResponse> getAll() {
        return accessTrailRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void logLogin(String username, String role) {
        AccessTrail accessTrail = AccessTrail.builder()
                .username(username)
                .role(role)
                .action("LOGIN")
                .timestamp(LocalDateTime.now())
                .build();
        accessTrailRepository.save(accessTrail);
    }

    @Transactional
    public void logLogout(String username, String role) {
        AccessTrail accessTrail = AccessTrail.builder()
                .username(username)
                .role(role)
                .action("LOGOUT")
                .timestamp(LocalDateTime.now())
                .build();
        accessTrailRepository.save(accessTrail);
    }

    private AccessTrailResponse toResponse(AccessTrail accessTrail) {
        return AccessTrailResponse.builder()
                .id(accessTrail.getId())
                .username(accessTrail.getUsername())
                .role(accessTrail.getRole())
                .action(accessTrail.getAction())
                .timestamp(accessTrail.getTimestamp())
                .build();
    }
}
