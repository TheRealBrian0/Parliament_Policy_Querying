package com.policypulse.rag;

import com.policypulse.domain.Persona;
import com.policypulse.domain.SessionType;
import com.policypulse.graphql.dto.ChatResponseView;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagChatService {

    private final InMemoryVectorIndexService vectorIndexService;
    private final OllamaChatModel chatModel;

    public RagChatService(InMemoryVectorIndexService vectorIndexService, OllamaChatModel chatModel) {
        this.vectorIndexService = vectorIndexService;
        this.chatModel = chatModel;
    }

    public ChatResponseView ask(Persona persona, String question) {
        List<EmbeddingMatch<TextSegment>> matches = vectorIndexService.search(question, 5);
        String context = matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n---\n"));

        String prompt = """
                You are a policy impact assistant for India.
                Persona: %s
                User question: %s

                Use only the following retrieved context when possible:
                %s

                Provide a concise practical answer and mention uncertainty if context is weak.
                """.formatted(persona.name(), question, context.isBlank() ? "No matching context found." : context);

        String answer = chatModel.generate(List.of(UserMessage.from(prompt)))
                .content()
                .text();
        List<String> sources = matches.stream()
                .map(m -> m.embedded().metadata().getString("documentId") + ":" + m.embedded().metadata().getString("chunkIndex"))
                .toList();
        return new ChatResponseView(answer, sources, List.of(SessionType.SESSION_1, SessionType.SESSION_2, SessionType.SESSION_3));
    }
}
