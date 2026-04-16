package com.policypulse.persistence.repository;

import com.policypulse.persistence.entity.SessionChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionChunkRepository extends JpaRepository<SessionChunkEntity, Long> {
    boolean existsByDocumentIdAndChunkIndex(long documentId, int chunkIndex);
}
