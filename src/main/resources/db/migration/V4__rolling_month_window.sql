-- ============================================================
-- V4: Replace session-based model with rolling month window
-- ============================================================

-- 1. Drop old FK constraints so we can restructure the tables
ALTER TABLE session_document DROP FOREIGN KEY fk_session_document_session;
ALTER TABLE session_chunk DROP FOREIGN KEY fk_chunk_session;
ALTER TABLE session_chunk DROP FOREIGN KEY fk_chunk_document;

-- 2. Drop old session-based tables (calendar is no longer needed)
DROP TABLE IF EXISTS session_calendar;
DROP TABLE IF EXISTS session_metadata;

-- 3. Create the new month-tracking table
CREATE TABLE ingested_month (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    year        INT NOT NULL,
    month       INT NOT NULL,
    ingested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ingested_month UNIQUE (year, month)
);

CREATE INDEX idx_ingested_month_year_month ON ingested_month (year, month);

-- 4. Wipe existing document/chunk data (orphaned from old sessions)
DELETE FROM session_chunk;
DELETE FROM session_document;

-- 5. Replace session_id with month_id on session_document
ALTER TABLE session_document
    DROP COLUMN session_id,
    ADD COLUMN month_id BIGINT NOT NULL AFTER id,
    ADD CONSTRAINT fk_session_document_month FOREIGN KEY (month_id) REFERENCES ingested_month(id) ON DELETE CASCADE;

-- 6. Replace session_id with month_id on session_chunk; restore document FK
ALTER TABLE session_chunk
    DROP COLUMN session_id,
    ADD COLUMN month_id BIGINT NOT NULL AFTER id,
    ADD CONSTRAINT fk_chunk_month     FOREIGN KEY (month_id)    REFERENCES ingested_month(id)    ON DELETE CASCADE,
    ADD CONSTRAINT fk_chunk_document  FOREIGN KEY (document_id) REFERENCES session_document(id)  ON DELETE CASCADE;
