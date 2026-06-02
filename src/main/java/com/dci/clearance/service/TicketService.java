package com.dci.clearance.service;

import com.dci.clearance.dto.TicketRequest;
import com.dci.clearance.entity.AuditTrail;
import com.dci.clearance.entity.SupportTicket;
import com.dci.clearance.repository.AuditTrailRepository;
import com.dci.clearance.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final AuditTrailRepository auditTrailRepository;

    // ✅ Philippine Time zone constant
    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    // Helper to get current user
    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    // Helper to get current user role
    private String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().size() > 0) {
            String role = auth.getAuthorities().iterator().next().getAuthority();
            return role.replace("ROLE_", "");
        }
        return "UNKNOWN";
    }

    // Helper to save log to audit_trail table
    private void saveLog(String action, String details) {
        AuditTrail log = AuditTrail.builder()
                .actionMade(action)
                .details(details)
                .userstamp(getCurrentUser())
                .userrole(getCurrentRole())
                // ✅ Use Manila time for audit logs
                .timestamp(LocalDateTime.now(MANILA))
                .build();
        auditTrailRepository.save(log);
    }

    public List<SupportTicket> getAll() {
        return ticketRepository.findAll();
    }

    public SupportTicket getById(Long id) {
        return ticketRepository.findById(id).orElse(null);
    }

    @Transactional
    public SupportTicket create(TicketRequest request) {
        SupportTicket ticket = SupportTicket.builder()
                .referenceNumber(request.getReferenceNumber())
                .status(request.getStatus() != null ? request.getStatus() : "PENDING")
                .requestedBy(request.getRequestedBy())
                .type(request.getType())
                .processedBy(getCurrentUser())
                // ✅ Always set both dates server-side in Manila time
                .dateUpdated(LocalDateTime.now(MANILA))
                .dateRequested(LocalDateTime.now(MANILA))
                .escalated(request.getEscalated() != null ? request.getEscalated() : "NO")
                .roleBased(request.getRoleBased())
                .mvFileNo(request.getMvFileNo())
                .plateNo(request.getPlateNo())
                .engineNo(request.getEngineNo())
                .chassisNo(request.getChassisNo())
                .make(request.getMake())
                .series(request.getSeries())
                .vehicleColor(request.getVehicleColor())
                .vehicleTypeDenomination(request.getVehicleTypeDenomination())
                .yearModel(request.getYearModel())
                .classification(request.getClassification())
                .name(request.getName())
                .address(request.getAddress())
                // .certificateOfRegistration(request.getCertificateOfRegistration())
                .plateCertification(request.getPlateCertification())
                .actualPlate(request.getActualPlate())
                .crAttachment(request.getCrAttachment())
                .build();

        SupportTicket savedTicket = ticketRepository.save(ticket);

        // LOG: Create Ticket
        saveLog("Create Ticket", "Created ticket: " + savedTicket.getReferenceNumber() + " | Type: " + savedTicket.getType() + " | Requested by: " + savedTicket.getRequestedBy());

        return savedTicket;
    }

    @Transactional
    public SupportTicket update(Long id, TicketRequest request) {
        SupportTicket existingTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        String oldStatus = existingTicket.getStatus();
        String oldEscalated = existingTicket.getEscalated();
        String oldProcessedBy = existingTicket.getProcessedBy();

        existingTicket.setReferenceNumber(request.getReferenceNumber());
        existingTicket.setStatus(request.getStatus());
        existingTicket.setRequestedBy(request.getRequestedBy());
        existingTicket.setType(request.getType());
        existingTicket.setProcessedBy(request.getProcessedBy());
        // ✅ Use Manila time
        existingTicket.setDateUpdated(LocalDateTime.now(MANILA));
        existingTicket.setDateRequested(request.getDateRequested());
        existingTicket.setEscalated(request.getEscalated());
        existingTicket.setRoleBased(request.getRoleBased());
        existingTicket.setMvFileNo(request.getMvFileNo());
        existingTicket.setPlateNo(request.getPlateNo());
        existingTicket.setEngineNo(request.getEngineNo());
        existingTicket.setChassisNo(request.getChassisNo());
        existingTicket.setMake(request.getMake());
        existingTicket.setSeries(request.getSeries());
        existingTicket.setVehicleColor(request.getVehicleColor());
        existingTicket.setVehicleTypeDenomination(request.getVehicleTypeDenomination());
        existingTicket.setYearModel(request.getYearModel());
        existingTicket.setClassification(request.getClassification());
        existingTicket.setName(request.getName());
        existingTicket.setAddress(request.getAddress());
        // existingTicket.setCertificateOfRegistration(request.getCertificateOfRegistration());
        existingTicket.setPlateCertification(request.getPlateCertification());
        existingTicket.setActualPlate(request.getActualPlate());
        existingTicket.setCrAttachment(request.getCrAttachment());

        SupportTicket updatedTicket = ticketRepository.save(existingTicket);

        // Build log details based on what changed
        StringBuilder details = new StringBuilder();
        details.append("Ticket: ").append(updatedTicket.getReferenceNumber());

        boolean changed = false;

        if (!oldStatus.equals(updatedTicket.getStatus())) {
            details.append(" | Status changed from '").append(oldStatus).append("' to '").append(updatedTicket.getStatus()).append("'");
            changed = true;
        }

        if (!oldEscalated.equals(updatedTicket.getEscalated())) {
            details.append(" | Escalation changed from '").append(oldEscalated).append("' to '").append(updatedTicket.getEscalated()).append("'");
            changed = true;
        }

        if (updatedTicket.getProcessedBy() != null &&
                (oldProcessedBy == null || !oldProcessedBy.equals(updatedTicket.getProcessedBy()))) {
            details.append(" | Assigned to: ").append(updatedTicket.getProcessedBy());
            changed = true;
        }

        // LOG: Update Ticket (only if something changed)
        if (changed) {
            saveLog("Update Ticket", details.toString());
        } else {
            saveLog("Update Ticket", "Updated ticket: " + updatedTicket.getReferenceNumber());
        }

        return updatedTicket;
    }

    @Transactional
    public void delete(Long id) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        String refNumber = ticket.getReferenceNumber();
        String ticketType = ticket.getType();

        ticketRepository.deleteById(id);

        // LOG: Delete Ticket
        saveLog("Delete Ticket", "Deleted ticket: " + refNumber + " | Type: " + ticketType);
    }

    // Separate method for Status Change only
    @Transactional
    public SupportTicket changeStatus(Long id, String newStatus) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        String oldStatus = ticket.getStatus();
        String refNumber = ticket.getReferenceNumber();

        ticket.setStatus(newStatus);
        // ✅ Use Manila time
        ticket.setDateUpdated(LocalDateTime.now(MANILA));

        SupportTicket updatedTicket = ticketRepository.save(ticket);

        // LOG: Change Ticket Status
        saveLog("Change Ticket Status", "Ticket " + refNumber + " status changed from " + oldStatus + " to " + newStatus);

        return updatedTicket;
    }

    // Separate method for Escalate Ticket
    @Transactional
    public SupportTicket escalateTicket(Long id, String escalateTo) {
        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        String refNumber = ticket.getReferenceNumber();
        String oldEscalated = ticket.getEscalated();

        ticket.setEscalated("YES");
        ticket.setRoleBased(escalateTo);
        // ✅ Use Manila time
        ticket.setDateUpdated(LocalDateTime.now(MANILA));

        SupportTicket updatedTicket = ticketRepository.save(ticket);

        // LOG: Escalate Ticket
        saveLog("Escalate Ticket", "Ticket " + refNumber + " escalated to " + escalateTo + " (was: " + oldEscalated + ")");

        return updatedTicket;
    }
}