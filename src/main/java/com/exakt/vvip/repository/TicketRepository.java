package com.exakt.vvip.repository;

import com.exakt.vvip.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<SupportTicket, Long> {

}