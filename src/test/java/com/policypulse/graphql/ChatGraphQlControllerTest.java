package com.policypulse.graphql;

import com.policypulse.chronology.ChronologyService;
import com.policypulse.domain.Persona;
import com.policypulse.domain.SessionType;
import com.policypulse.graphql.controller.ChatGraphQlController;
import com.policypulse.graphql.dto.ChatResponseView;
import com.policypulse.graphql.dto.SystemStatusView;
import com.policypulse.rag.RagChatService;
import com.policypulse.scrape.HybridGovDataCollector;
import com.policypulse.scrape.IngestionDiagnosticsSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@GraphQlTest(ChatGraphQlController.class)
class ChatGraphQlControllerTest {

    @Autowired
    GraphQlTester graphQlTester;

    @MockBean
    ChronologyService chronologyService;

    @MockBean
    HybridGovDataCollector collector;

    @MockBean
    RagChatService ragChatService;

    @Test
    void systemStatusQueryReturnsCurrentFields() {
        when(chronologyService.currentStatus()).thenReturn(
                new SystemStatusView(2026, List.of(SessionType.SESSION_1), 2026, null)
        );

        graphQlTester.document("{ systemStatus { currentYear activeSessions vectorContextYear } }")
                .execute()
                .path("systemStatus.currentYear").entity(Integer.class).isEqualTo(2026)
                .path("systemStatus.activeSessions[0]").entity(String.class).isEqualTo("SESSION_1")
                .path("systemStatus.vectorContextYear").entity(Integer.class).isEqualTo(2026);
    }

    @Test
    void askQuestionMutationDelegatesToRagService() {
        when(ragChatService.ask(eq(Persona.GENERAL_CITIZEN), any()))
                .thenReturn(new ChatResponseView("answer", List.of("1:0"), List.of(SessionType.SESSION_1)));
        when(collector.getLastDiagnostics()).thenReturn(new IngestionDiagnosticsSnapshot(
                OffsetDateTime.now(), 12, 4, 8, 3, 3, 0
        ));

        graphQlTester.document("""
                        mutation {
                          askQuestion(input: { persona: GENERAL_CITIZEN, question: "policy outlook?" }) {
                            answer
                            sources
                          }
                        }
                        """)
                .execute()
                .path("askQuestion.answer").entity(String.class).isEqualTo("answer")
                .path("askQuestion.sources[0]").entity(String.class).isEqualTo("1:0");
    }
}
