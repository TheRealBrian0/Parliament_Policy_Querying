package com.policypulse.persistence.repository;

import com.policypulse.persistence.entity.IngestedMonthEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngestedMonthRepository extends JpaRepository<IngestedMonthEntity, Long> {

    Optional<IngestedMonthEntity> findByYearAndMonth(int year, int month);

    List<IngestedMonthEntity> findAllByOrderByYearAscMonthAsc();
}
