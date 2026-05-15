package com.wha.repository;

import com.wha.entity.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, String> {

    List<Donation> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<Donation> findByFlaggedTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Donation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT SUM(d.amount) FROM Donation d WHERE d.status = :status")
    BigDecimal sumDonationsByStatus(@Param("status") Donation.DonationStatus status);

    default BigDecimal sumCompletedDonations() {
        BigDecimal result = sumDonationsByStatus(Donation.DonationStatus.COMPLETED);
        return result != null ? result : java.math.BigDecimal.ZERO;
    }

    @Query("SELECT COUNT(d) FROM Donation d WHERE d.donorIp = :ip AND d.createdAt > :since")
    long countDonationsFromIp(@Param("ip") String ip, @Param("since") LocalDateTime since);

    @Query("SELECT SUM(d.amount) FROM Donation d WHERE d.user.id = :userId " +
           "AND d.status = :status AND d.createdAt > :since")
    BigDecimal sumDonationsByUserAndStatus(@Param("userId") String userId,
                                           @Param("status") Donation.DonationStatus status,
                                           @Param("since") LocalDateTime since);

    default BigDecimal sumRecentDonationsByUser(String userId, LocalDateTime since) {
        return sumDonationsByUserAndStatus(userId, Donation.DonationStatus.COMPLETED, since);
    }

    @Query("SELECT COUNT(d) FROM Donation d WHERE d.status = :status")
    long countByStatus(@Param("status") Donation.DonationStatus status);

    default long countCompleted() {
        return countByStatus(Donation.DonationStatus.COMPLETED);
    }
}
