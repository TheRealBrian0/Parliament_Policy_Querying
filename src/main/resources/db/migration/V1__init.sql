CREATE TABLE session_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    year INT NOT NULL,
    session_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    scrape_date DATE NOT NULL,
    activated_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_session_year_type UNIQUE (year, session_type)
);

CREATE TABLE session_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    source_url VARCHAR(1024) NULL,
    title VARCHAR(512) NOT NULL,
    raw_text LONGTEXT NOT NULL,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_document_session FOREIGN KEY (session_id) REFERENCES session_metadata(id) ON DELETE CASCADE
);

CREATE TABLE session_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    vector_ref VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chunk_session FOREIGN KEY (session_id) REFERENCES session_metadata(id) ON DELETE CASCADE,
    CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES session_document(id) ON DELETE CASCADE,
    CONSTRAINT uk_document_chunk UNIQUE (document_id, chunk_index)
);

CREATE TABLE ingestion_checkpoint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    checkpoint_key VARCHAR(128) NOT NULL,
    checkpoint_value VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_checkpoint_key UNIQUE (checkpoint_key)
);
