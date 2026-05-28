package com.exakt.vvip.service;

import com.exakt.vvip.dto.TicketRequest;
import com.exakt.vvip.entity.SupportTicket;
import com.exakt.vvip.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    public List<SupportTicket> getAll() {
        return ticketRepository.findAll();
    }

    public SupportTicket getById(Long id) {
        return ticketRepository.findById(id)
                .orElse(null);
    }

    @Transactional
    public SupportTicket create(TicketRequest request) {

        SupportTicket ticket = SupportTicket.builder()
                .referenceNumber(request.getReferenceNumber())
                .status(request.getStatus())
                .requestedBy(request.getRequestedBy())
                .type(request.getType())
                .processedBy(request.getProcessedBy())
                .dateUpdated(LocalDateTime.now())
                .dateRequested(request.getDateRequested())
                .escalated(request.getEscalated())
                .roleBased(request.getRoleBased())
                .build();

        return ticketRepository.save(ticket);
    }

    @Transactional
    public SupportTicket update(Long id, TicketRequest request) {

        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticket.setReferenceNumber(request.getReferenceNumber());
        ticket.setStatus(request.getStatus());
        ticket.setRequestedBy(request.getRequestedBy());
        ticket.setType(request.getType());
        ticket.setProcessedBy(request.getProcessedBy());
        ticket.setDateUpdated(LocalDateTime.now());
        ticket.setDateRequested(request.getDateRequested());
        ticket.setEscalated(request.getEscalated());
        ticket.setRoleBased(request.getRoleBased());

        return ticketRepository.save(ticket);
    }

    @Transactional
    public void delete(Long id) {
        ticketRepository.deleteById(id);
    }
}