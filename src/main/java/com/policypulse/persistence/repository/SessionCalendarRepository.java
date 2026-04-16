package com.policypulse.persistence.repository;

import com.policypulse.domain.SessionType;
import com.policypulse.persistence.entity.SessionCalendarEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionCalendarRepository extends JpaRepository<SessionCalendarEntity, Long> {
    List<SessionCalendarEntity> findByYearOrderByStartDateAsc(int year);

    Optional<SessionCalendarEntity> findByYearAndSessionType(int year, SessionType sessionType);
}
