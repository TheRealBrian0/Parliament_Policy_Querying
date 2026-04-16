CREATE TABLE session_calendar (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    year INT NOT NULL,
    session_type VARCHAR(32) NOT NULL,
    start_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_calendar_year_type UNIQUE (year, session_type)
);

CREATE INDEX idx_session_metadata_year ON session_metadata (year);
CREATE INDEX idx_session_calendar_year ON session_calendar (year);
