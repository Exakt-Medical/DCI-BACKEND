package com.exakt.vvip.controller;

import com.exakt.vvip.dto.TicketRequest;
import com.exakt.vvip.entity.SupportTicket;
import com.exakt.vvip.repository.TicketRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support-ticket")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Support Ticket", description = "Support Ticket Management Endpoints")
public class TicketController {



    private final TicketRepository ticketRepository;

    @GetMapping
    @Operation(summary = "Get all support tickets")
    public ResponseEntity<List<SupportTicket>> getAllSupportTickets() {

        return ResponseEntity.ok(ticketRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get support ticket by ID")
    public ResponseEntity<SupportTicket> getSupportTicketById(
            @PathVariable Long id) {

        return ticketRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create support ticket")
    public ResponseEntity<SupportTicket> createSupportTicket(
            @RequestBody TicketRequest request) {

        SupportTicket ticket = SupportTicket.builder()
                .referenceNumber(request.getReferenceNumber())
                .status(request.getStatus())
                .requestedBy(request.getRequestedBy())
                .type(request.getType())
                .processedBy(request.getProcessedBy())
                .dateUpdated(request.getDateUpdated())
                .dateRequested(request.getDateRequested())
                .escalated(request.getEscalated())
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

        return ResponseEntity.ok(ticketRepository.save(ticket));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update support ticket")
    public ResponseEntity<SupportTicket> updateSupportTicket(
            @PathVariable Long id,
            @RequestBody TicketRequest request) {

        return ticketRepository.findById(id)
                .map(ticket -> {

                    ticket.setReferenceNumber(request.getReferenceNumber());
                    ticket.setStatus(request.getStatus());
                    ticket.setRequestedBy(request.getRequestedBy());
                    ticket.setType(request.getType());
                    ticket.setProcessedBy(request.getProcessedBy());
                    ticket.setDateUpdated(request.getDateUpdated());
                    ticket.setDateRequested(request.getDateRequested());
                    ticket.setEscalated(request.getEscalated());
                    ticket.setRoleBased(request.getRoleBased());

                    ticket.setMvFileNo(request.getMvFileNo());
                    ticket.setPlateNo(request.getPlateNo());
                    ticket.setEngineNo(request.getEngineNo());
                    ticket.setChassisNo(request.getChassisNo());
                    ticket.setMake(request.getMake());
                    ticket.setSeries(request.getSeries());
                    ticket.setVehicleColor(request.getVehicleColor());
                    ticket.setVehicleTypeDenomination(request.getVehicleTypeDenomination());
                    ticket.setYearModel(request.getYearModel());
                    ticket.setClassification(request.getClassification());
                    ticket.setName(request.getName());
                    ticket.setAddress(request.getAddress());

                   // ticket.setCertificateOfRegistration(request.getCertificateOfRegistration());
                    ticket.setPlateCertification(request.getPlateCertification());
                    ticket.setActualPlate(request.getActualPlate());
                    ticket.setCrAttachment(request.getCrAttachment());


                    return ResponseEntity.ok(ticketRepository.save(ticket));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete support ticket")
    public ResponseEntity<Void> deleteSupportTicket(
            @PathVariable Long id) {

        if (!ticketRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        ticketRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}