package com.policypulse.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "session_document")
public class SessionDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "source_url", length = 1024)
    private String sourceUrl;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(name = "raw_text", nullable = false, columnDefinition = "LONGTEXT")
    private String rawText;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(nullable = false, length = 8)
    private String language;

    @Column(length = 128)
    private String fingerprint;

    public Long getId() {
        return id;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
}
