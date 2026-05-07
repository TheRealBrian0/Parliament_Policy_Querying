package com.policypulse.persistence.repository;

import com.policypulse.persistence.entity.SessionDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionDocumentRepository extends JpaRepository<SessionDocumentEntity, Long> {

    boolean existsByFingerprint(String fingerprint);

    void deleteByMonthId(long monthId);
}
