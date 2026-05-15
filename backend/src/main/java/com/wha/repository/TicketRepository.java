package com.wha.repository;

import com.wha.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, String> {

    List<Ticket> findByUserIdOrderByPurchasedAtDesc(String userId);

    Page<Ticket> findByEventId(String eventId, Pageable pageable);

    Optional<Ticket> findByTicketCode(String ticketCode);

    long countByUserIdAndEventId(String userId, String eventId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.purchaseIp = :ip AND t.purchasedAt > :since")
    long countPurchasesFromIp(@Param("ip") String ip, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status")
    long countByStatus(@Param("status") Ticket.TicketStatus status);

    default long countActiveTickets() {
        return countByStatus(Ticket.TicketStatus.ACTIVE);
    }

    Page<Ticket> findAllByOrderByPurchasedAtDesc(Pageable pageable);

    void deleteByEventId(String eventId);
}
