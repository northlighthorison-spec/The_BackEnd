package com.wha.repository;

import com.wha.entity.VipSelection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VipSelectionRepository extends JpaRepository<VipSelection, String> {

    Optional<VipSelection> findBySelectionYearAndSelectionMonth(int year, int month);

    boolean existsBySelectionYearAndSelectionMonth(int year, int month);

    Page<VipSelection> findAllByOrderBySelectionYearDescSelectionMonthDesc(Pageable pageable);
}
