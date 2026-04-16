package com.policypulse.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "session_chunk")
public class SessionChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "vector_ref", length = 255)
    private String vectorRef;

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public void setChunkText(String chunkText) {
        this.chunkText = chunkText;
    }

    public void setVectorRef(String vectorRef) {
        this.vectorRef = vectorRef;
    }

    public Long getId() {
        return id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getChunkText() {
        return chunkText;
    }

    public String getVectorRef() {
        return vectorRef;
    }
}
