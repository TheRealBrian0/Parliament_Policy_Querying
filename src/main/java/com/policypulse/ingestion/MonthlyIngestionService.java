package com.policypulse.ingestion;

import com.policypulse.kafka.event.DocumentScrapedEvent;
import com.policypulse.kafka.producer.DocumentDataProducer;
import com.policypulse.persistence.entity.IngestedMonthEntity;
import com.policypulse.persistence.repository.IngestedMonthRepository;
import com.policypulse.persistence.repository.SessionChunkRepository;
import com.policypulse.persistence.repository.SessionDocumentRepository;
import com.policypulse.scrape.HybridGovDataCollector;
import com.policypulse.scrape.ScrapedDocument;
import com.policypulse.rag.VectorIndexService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * <p>
 * On startup and on the 1st of every month:
 * <ol>
 * <li>Computes the 24-month window (from 23 months ago to the current
 * month).</li>
 * <li>Scrapes any months in that window not yet in the database.</li>
 * <li>Evicts any months that fall outside the 24-month window.</li>
 * </ol>
 */
@Service
public class MonthlyIngestionService {
    // responsible for month context window and scrapper
    private static final int WINDOW_MONTHS = 24;
    private static final Logger log = LoggerFactory.getLogger(MonthlyIngestionService.class);

    private final IngestedMonthRepository ingestedMonthRepository;
    private final SessionDocumentRepository sessionDocumentRepository;
    private final SessionChunkRepository sessionChunkRepository;
    private final HybridGovDataCollector collector;
    private final DocumentDataProducer documentDataProducer;
    private final VectorIndexService vectorIndexService;

    public MonthlyIngestionService(
            IngestedMonthRepository ingestedMonthRepository,
            SessionDocumentRepository sessionDocumentRepository,
            SessionChunkRepository sessionChunkRepository,
            HybridGovDataCollector collector,
            DocumentDataProducer documentDataProducer,
            VectorIndexService vectorIndexService) {
        this.ingestedMonthRepository = ingestedMonthRepository;
        this.sessionDocumentRepository = sessionDocumentRepository;
        this.sessionChunkRepository = sessionChunkRepository;
        this.collector = collector;
        this.documentDataProducer = documentDataProducer;
        this.vectorIndexService = vectorIndexService;
    }

    /**
     * Called on startup and by the monthly scheduler.
     * Fills any gaps in the 24-month window and evicts stale months.
     */
    @Transactional
    public void ensureRollingWindow() {
        YearMonth current = YearMonth.now(ZoneOffset.UTC);

        // Build ordered list of the 24 months we want to keep
        List<YearMonth> targetWindow = new ArrayList<>();
        for (int i = WINDOW_MONTHS - 1; i >= 0; i--) {
            targetWindow.add(current.minusMonths(i));
        }

        log.info("Rolling window: {} months, from {} to {}", WINDOW_MONTHS,
                targetWindow.get(0), targetWindow.get(targetWindow.size() - 1));

        // Scrape any months not yet in the database
        for (YearMonth ym : targetWindow) {
            if (ingestedMonthRepository.findByYearAndMonth(ym.getYear(), ym.getMonthValue()).isEmpty()) {
                ingestMonth(ym.getYear(), ym.getMonthValue());
            } else {
                log.info("Month already ingested: {}-{}", ym.getYear(), ym.getMonthValue());
            }
        }

        // Evict any months that are now outside the 24-month window
        evictStaleMonths(targetWindow);
    }

    /**
     * Fired on the 1st of every month at 06:00 UTC to pick up the newest month
     * and drop the one that has aged out of the 24-month window.
     */
    @Scheduled(cron = "0 0 6 1 * *")
    @Transactional
    public void onNewMonth() {
        log.info("Monthly scheduler triggered — refreshing rolling window");
        ensureRollingWindow();
    }

    // ------------------------------------------------------------------

    private void ingestMonth(int year, int month) {
        log.info("Ingesting month: {}-{}", year, month);

        IngestedMonthEntity monthEntity = new IngestedMonthEntity();
        monthEntity.setYear(year);
        monthEntity.setMonth(month);
        monthEntity.setIngestedAt(OffsetDateTime.now(ZoneOffset.UTC));
        IngestedMonthEntity saved = ingestedMonthRepository.save(monthEntity);

        List<ScrapedDocument> docs = collector.collectForMonth(year, month, saved.getId());

        String kafkaKey = year + "-" + String.format("%02d", month);
        for (ScrapedDocument doc : docs) {
            documentDataProducer.publish(kafkaKey, new DocumentScrapedEvent(
                    saved.getId(),
                    doc.documentId(),
                    year,
                    month,
                    doc.sourceUrl(),
                    doc.title(),
                    doc.rawText(),
                    doc.publishedAt()));
        }

        log.info("Month ingestion complete: {}-{}, documents published to Kafka: {}", year, month, docs.size());
    }

    private void evictStaleMonths(List<YearMonth> targetWindow) {
        List<IngestedMonthEntity> all = ingestedMonthRepository.findAllByOrderByYearAscMonthAsc();
        for (IngestedMonthEntity entity : all) {
            YearMonth ym = YearMonth.of(entity.getYear(), entity.getMonth());
            if (!targetWindow.contains(ym)) {
                log.info("Evicting stale month: {}-{} (monthId={})", entity.getYear(), entity.getMonth(),
                        entity.getId());
                // Chunks cascade-delete via FK, but we delete explicitly for clarity
                vectorIndexService.deleteByMonthId(entity.getId());
                sessionChunkRepository.deleteByMonthId(entity.getId());
                sessionDocumentRepository.deleteByMonthId(entity.getId());
                ingestedMonthRepository.delete(entity);
                log.info("Eviction complete: {}-{}", entity.getYear(), entity.getMonth());
            }
        }
    }

    /**
     * Returns list of all currently ingested months, ordered oldest to newest.
     */
    public List<IngestedMonthEntity> listIngestedMonths() {
        return ingestedMonthRepository.findAllByOrderByYearAscMonthAsc();
    }

    /**
     * Returns a human-readable status summary for GraphQL.
     */
    public String currentStatus() {
        List<IngestedMonthEntity> months = listIngestedMonths();
        if (months.isEmpty()) {
            return "No months ingested yet";
        }
        IngestedMonthEntity latest = months.get(months.size() - 1);
        return String.format("%d months in window, latest: %d-%02d",
                months.size(), latest.getYear(), latest.getMonth());
    }
}
