package com.policypulse.persistence.repository;

import com.policypulse.domain.SessionType;
import com.policypulse.persistence.entity.SessionMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionMetadataRepository extends JpaRepository<SessionMetadataEntity, Long> {
    List<SessionMetadataEntity> findByYearOrderBySessionTypeAsc(int year);

    Optional<SessionMetadataEntity> findByYearAndSessionType(int year, SessionType sessionType);

    void deleteByYearLessThan(int year);
}
