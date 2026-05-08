package com.policypulse.rag;

import com.policypulse.domain.Persona;
import com.policypulse.graphql.dto.ChatResponseView;
import com.policypulse.rag.VectorIndexService;
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

    private final VectorIndexService vectorIndexService;
    private final OllamaChatModel chatModel;
    private final dev.langchain4j.memory.ChatMemory chatMemory = dev.langchain4j.memory.chat.MessageWindowChatMemory.withMaxMessages(10);

    public RagChatService(VectorIndexService vectorIndexService, OllamaChatModel chatModel) {
        this.vectorIndexService = vectorIndexService;
        this.chatModel = chatModel;
    }

    public ChatResponseView ask(Persona persona, String question) {
        List<EmbeddingMatch<TextSegment>> matches = vectorIndexService.search(question, 5);
        String context = matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n---\n"));

        // Record the clean user question into memory to avoid polluting history with massive RSS chunks
        chatMemory.add(dev.langchain4j.data.message.UserMessage.from(question));

        dev.langchain4j.data.message.SystemMessage sysMsg = dev.langchain4j.data.message.SystemMessage.from(
                "You are Policy Pulse, an AI Policy Impact Assistant for India. Your role is to help users understand how new Indian government policies, bills, and regulations might affect them.\n" +
                "The user is asking from the perspective of the following Persona: " + persona.name() + ".\n" +
                "If the user asks a general question about what you can do, ignore the retrieved context and introduce yourself. Explain that you analyze recent Indian policies (sourced from government feeds and PRS India) to explain their practical impact on various sectors and individuals.\n" +
                "For specific policy questions, provide a concise, practical answer based ONLY on the provided context. If the context does not contain the answer, state that you don't have enough information from the recent policy data to answer."
        );

        List<dev.langchain4j.data.message.ChatMessage> messages = new java.util.ArrayList<>();
        messages.add(sysMsg);
        
        // Add chat history (excluding the extremely recent clean question we just added, to replace it with the context-augmented version)
        List<dev.langchain4j.data.message.ChatMessage> history = chatMemory.messages();
        for (int i = 0; i < history.size() - 1; i++) {
            messages.add(history.get(i));
        }

        // Ephemerally inject the RAG context only into the current prompt
        String ephemeralPrompt = """
                Retrieved Policy Context:
                %s

                User question: %s
                """.formatted(context.isBlank() ? "No matching context found." : context, question);
        
        messages.add(dev.langchain4j.data.message.UserMessage.from(ephemeralPrompt));

        dev.langchain4j.data.message.AiMessage answer = chatModel.generate(messages).content();
        chatMemory.add(answer);

        List<String> sources = matches.stream()
                .map(m -> m.embedded().metadata().getString("documentId") + ":" + m.embedded().metadata().getString("chunkIndex"))
                .toList();

        return new ChatResponseView(answer.text(), sources);
    }
}
