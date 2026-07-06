package com.dci.clearance.controller;

import com.dci.clearance.dto.TicketRequest;
import com.dci.clearance.entity.SupportTicket;
import com.dci.clearance.service.TicketService;
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

    private final TicketService ticketService;

    @GetMapping
    @Operation(summary = "Get all support tickets")
    public ResponseEntity<List<SupportTicket>> getAllSupportTickets() {
        return ResponseEntity.ok(ticketService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get support ticket by ID")
    public ResponseEntity<SupportTicket> getSupportTicketById(@PathVariable Long id) {
        SupportTicket ticket = ticketService.getById(id);
        return ticket != null ? ResponseEntity.ok(ticket) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Create support ticket")
    public ResponseEntity<SupportTicket> createSupportTicket(@RequestBody TicketRequest request) {
        return ResponseEntity.ok(ticketService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update support ticket")
    public ResponseEntity<SupportTicket> updateSupportTicket(@PathVariable Long id, @RequestBody TicketRequest request) {
        SupportTicket updated = ticketService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete support ticket")
    public ResponseEntity<Void> deleteSupportTicket(@PathVariable Long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change ticket status only")
    public ResponseEntity<SupportTicket> changeTicketStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(ticketService.changeStatus(id, status));
    }

    @PatchMapping("/{id}/escalate")
    @Operation(summary = "Escalate ticket to DCI or other department")
    public ResponseEntity<SupportTicket> escalateTicket(@PathVariable Long id, @RequestParam String escalateTo) {
        return ResponseEntity.ok(ticketService.escalateTicket(id, escalateTo));
    }
}