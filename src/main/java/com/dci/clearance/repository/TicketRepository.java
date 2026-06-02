package com.dci.clearance.repository;

import com.dci.clearance.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<SupportTicket, Long> {

}