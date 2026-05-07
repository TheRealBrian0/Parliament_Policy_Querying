package com.policypulse.persistence.repository;

import com.policypulse.persistence.entity.SessionChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionChunkRepository extends JpaRepository<SessionChunkEntity, Long> {

    boolean existsByDocumentIdAndChunkIndex(long documentId, int chunkIndex);

    void deleteByMonthId(long monthId);

    List<SessionChunkEntity> findAll();
}
