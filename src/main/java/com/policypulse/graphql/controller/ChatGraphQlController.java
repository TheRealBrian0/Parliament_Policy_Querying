package com.policypulse.graphql.controller;

import com.policypulse.graphql.dto.AskQuestionInput;
import com.policypulse.graphql.dto.ChatResponseView;
import com.policypulse.graphql.dto.IngestionDiagnosticsView;
import com.policypulse.graphql.dto.MonthStatusView;
import com.policypulse.graphql.dto.SystemStatusView;
import com.policypulse.ingestion.MonthlyIngestionService;
import com.policypulse.persistence.entity.IngestedMonthEntity;
import com.policypulse.rag.RagChatService;
import com.policypulse.scrape.HybridGovDataCollector;
import com.policypulse.scrape.IngestionDiagnosticsSnapshot;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.List;

@Controller
public class ChatGraphQlController {

    private final MonthlyIngestionService monthlyIngestionService;
    private final HybridGovDataCollector collector;
    private final RagChatService ragChatService;

    public ChatGraphQlController(
            MonthlyIngestionService monthlyIngestionService,
            HybridGovDataCollector collector,
            RagChatService ragChatService) {
        this.monthlyIngestionService = monthlyIngestionService;
        this.collector = collector;
        this.ragChatService = ragChatService;
    }

    @QueryMapping
    public SystemStatusView systemStatus() {
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        List<IngestedMonthEntity> months = monthlyIngestionService.listIngestedMonths();
        return new SystemStatusView(
                now.getYear(),
                now.getMonthValue(),
                months.size(),
                monthlyIngestionService.currentStatus()
        );
    }

    @QueryMapping
    public List<MonthStatusView> ingestedMonths() {
        return monthlyIngestionService.listIngestedMonths().stream()
                .map(e -> new MonthStatusView(
                        e.getYear(),
                        e.getMonth(),
                        e.getIngestedAt().toString()))
                .toList();
    }

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

    @MutationMapping
    public ChatResponseView askQuestion(@Argument @Valid AskQuestionInput input) {
        return ragChatService.ask(input.persona(), input.question());
    }

    @MutationMapping
    public boolean triggerIngestion() {
        monthlyIngestionService.ensureRollingWindow();
        return true;
    }
}
