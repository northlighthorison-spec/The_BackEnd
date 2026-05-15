package com.wha.repository;

import com.wha.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    Optional<Subscription> findByUserId(String userId);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = :status")
    long countActive(@Param("status") Subscription.SubscriptionStatus status);

    Page<Subscription> findByStatusOrderByCreatedAtDesc(Subscription.SubscriptionStatus status, Pageable pageable);
}
