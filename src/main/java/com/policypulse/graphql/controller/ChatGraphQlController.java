package com.policypulse.graphql.controller;

import com.policypulse.chronology.ChronologyService;
import com.policypulse.graphql.dto.AskQuestionInput;
import com.policypulse.graphql.dto.ChatResponseView;
import com.policypulse.graphql.dto.IngestionDiagnosticsView;
import com.policypulse.graphql.dto.SessionView;
import com.policypulse.graphql.dto.SystemStatusView;
import com.policypulse.rag.RagChatService;
import com.policypulse.scrape.HybridGovDataCollector;
import com.policypulse.scrape.IngestionDiagnosticsSnapshot;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ChatGraphQlController {

    private final ChronologyService chronologyService;
    private final HybridGovDataCollector collector;
    private final RagChatService ragChatService;

    public ChatGraphQlController(ChronologyService chronologyService, HybridGovDataCollector collector, RagChatService ragChatService) {
        this.chronologyService = chronologyService;
        this.collector = collector;
        this.ragChatService = ragChatService;
    }

    /*
     * GraphQL schema mapping:
     * - Query.systemStatus -> this method via @QueryMapping(name = "systemStatus")
     * - The return fields are auto-mapped by Spring GraphQL from record getters:
     *   currentYear, activeSessions, vectorContextYear, lastIngestionAt
     * - In Phase 2+, this will call chronology + ingestion status services instead of static values.
     */
    @QueryMapping
    public SystemStatusView systemStatus() {
        return chronologyService.currentStatus();
    }

    /*
     * GraphQL schema mapping:
     * - Query.sessions(year: Int!) -> method parameter `year`
     * - @Argument binds the GraphQL argument by name to Java method parameter.
     * - The response list of SessionView is serialized field-by-field based on schema names.
     */
    @QueryMapping
    public List<SessionView> sessions(@Argument int year) {
        return chronologyService.sessionsForYear(year);
    }

    /*
     * GraphQL schema mapping:
     * - Query.ingestionDiagnostics -> exposes latest RSS/PRS collector counters.
     * - Useful for demos to confirm what was accepted/rejected in recent runs.
     */
    @QueryMapping
    public IngestionDiagnosticsView ingestionDiagnostics() {
        IngestionDiagnosticsSnapshot snapshot = collector.getLastDiagnostics();
        return new IngestionDiagnosticsView(
                snapshot.lastRunAt() == null ? null : snapshot.lastRunAt().toString(),
                snapshot.rssFeedsChecked(),
                snapshot.rssAccepted(),
                snapshot.rssRejected(),
                snapshot.prsPdfLinksChecked(),
                snapshot.prsPdfAccepted(),
                snapshot.prsPdfRejected(),
                snapshot.rssAccepted() + snapshot.prsPdfAccepted()
        );
    }

    /*
     * GraphQL schema mapping:
     * - Mutation.askQuestion(input: AskQuestionInput!) -> `askQuestion` method
     * - `@Valid` ensures Bean Validation annotations in AskQuestionInput are enforced.
     * - In Phase 4, this method will orchestrate the full RAG pipeline:
     *   embed query -> retrieve chunks -> generate answer with Ollama via LangChain4j.
     */
    @MutationMapping
    public ChatResponseView askQuestion(@Argument @Valid AskQuestionInput input) {
        return ragChatService.ask(input.persona(), input.question());
    }

    /*
     * Manual sync trigger exposed to admin UI for demos and forced reconciliation.
     */
    @MutationMapping
    public boolean triggerIngestion() {
        chronologyService.reconcileOnStartup();
        return true;
    }
}
