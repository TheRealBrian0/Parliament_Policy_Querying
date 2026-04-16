ALTER TABLE session_document
    ADD COLUMN language VARCHAR(8) NOT NULL DEFAULT 'EN',
    ADD COLUMN fingerprint VARCHAR(128) NULL;

CREATE UNIQUE INDEX uk_session_document_fingerprint ON session_document (fingerprint);
