package com.policypulse.chronology;

import com.policypulse.config.ChronologyProperties;
import com.policypulse.domain.SessionStatus;
import com.policypulse.domain.SessionType;
import com.policypulse.graphql.dto.SessionView;
import com.policypulse.graphql.dto.SystemStatusView;
import com.policypulse.kafka.event.SessionScrapedEvent;
import com.policypulse.kafka.producer.SessionDataProducer;
import com.policypulse.persistence.entity.SessionCalendarEntity;
import com.policypulse.persistence.entity.SessionMetadataEntity;
import com.policypulse.persistence.repository.SessionCalendarRepository;
import com.policypulse.persistence.repository.SessionMetadataRepository;
import com.policypulse.scrape.MockScrapedDocument;
import com.policypulse.scrape.SessionScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ChronologyService {
    private static final Logger log = LoggerFactory.getLogger(ChronologyService.class);

    private final SessionCalendarRepository sessionCalendarRepository;
    private final SessionMetadataRepository sessionMetadataRepository;
    private final SessionScraperService scraperService;
    private final SessionDataProducer sessionDataProducer;
    private final ChronologyProperties chronologyProperties;
    private final Clock clock;

    public ChronologyService(
            SessionCalendarRepository sessionCalendarRepository,
            SessionMetadataRepository sessionMetadataRepository,
            SessionScraperService scraperService,
            SessionDataProducer sessionDataProducer,
            ChronologyProperties chronologyProperties
    ) {
        this.sessionCalendarRepository = sessionCalendarRepository;
        this.sessionMetadataRepository = sessionMetadataRepository;
        this.scraperService = scraperService;
        this.sessionDataProducer = sessionDataProducer;
        this.chronologyProperties = chronologyProperties;
        this.clock = Clock.systemDefaultZone();
    }

    @Transactional
    public void reconcileOnStartup() {
        reconcile();
    }

    @Scheduled(cron = "${app.chronology.reconcile-cron:0 0 4 * * *}")
    @Transactional
    public void reconcileBySchedule() {
        reconcile();
    }

    @Transactional
    public SystemStatusView currentStatus() {
        int year = LocalDate.now(clock).getYear();
        List<SessionMetadataEntity> sessions = sessionMetadataRepository.findByYearOrderBySessionTypeAsc(year);
        List<SessionType> active = sessions.stream()
                .filter(it -> it.getStatus() == SessionStatus.ACTIVE)
                .map(SessionMetadataEntity::getSessionType)
                .toList();
        return new SystemStatusView(year, active, year, null);
    }

    @Transactional
    public List<SessionView> sessionsForYear(int year) {
        return sessionMetadataRepository.findByYearOrderBySessionTypeAsc(year).stream()
                .map(it -> new SessionView(
                        String.valueOf(it.getId()),
                        it.getYear(),
                        it.getSessionType(),
                        it.getStatus(),
                        it.getScrapeDate().toString(),
                        it.getActivatedAt() != null ? it.getActivatedAt().toString() : null
                ))
                .toList();
    }

    private void reconcile() {
        LocalDate today = LocalDate.now(clock);
        int currentYear = today.getYear();

        List<SessionCalendarEntity> calendar = ensureCalendarForYear(currentYear);
        ensureSessionRows(currentYear, calendar);
        wipePreviousYearsIfCurrentYearStarted(today, currentYear, calendar);
        runCatchUpScrapes(today, currentYear, calendar);
    }

    private List<SessionCalendarEntity> ensureCalendarForYear(int year) {
        List<SessionCalendarEntity> existing = sessionCalendarRepository.findByYearOrderByStartDateAsc(year);
        if (existing.size() == 3) {
            return existing;
        }

        saveCalendarIfMissing(year, SessionType.SESSION_1, chronologyProperties.session1Month(), chronologyProperties.session1Day());
        saveCalendarIfMissing(year, SessionType.SESSION_2, chronologyProperties.session2Month(), chronologyProperties.session2Day());
        saveCalendarIfMissing(year, SessionType.SESSION_3, chronologyProperties.session3Month(), chronologyProperties.session3Day());

        return sessionCalendarRepository.findByYearOrderByStartDateAsc(year);
    }

    private void saveCalendarIfMissing(int year, SessionType sessionType, int month, int day) {
        if (sessionCalendarRepository.findByYearAndSessionType(year, sessionType).isPresent()) {
            return;
        }
        sessionCalendarRepository.save(newCalendar(year, sessionType, month, day));
    }

    private SessionCalendarEntity newCalendar(int year, SessionType sessionType, int month, int day) {
        SessionCalendarEntity entity = new SessionCalendarEntity();
        entity.setYear(year);
        entity.setSessionType(sessionType);
        entity.setStartDate(LocalDate.of(year, month, day));
        return entity;
    }

    private void ensureSessionRows(int year, List<SessionCalendarEntity> calendar) {
        for (SessionCalendarEntity slot : calendar) {
            sessionMetadataRepository.findByYearAndSessionType(year, slot.getSessionType())
                    .orElseGet(() -> {
                        SessionMetadataEntity meta = new SessionMetadataEntity();
                        meta.setYear(year);
                        meta.setSessionType(slot.getSessionType());
                        meta.setStatus(SessionStatus.SCHEDULED);
                        meta.setScrapeDate(slot.getStartDate());
                        return sessionMetadataRepository.save(meta);
                    });
        }
    }

    private void wipePreviousYearsIfCurrentYearStarted(LocalDate today, int currentYear, List<SessionCalendarEntity> calendar) {
        LocalDate session1Date = calendar.stream()
                .filter(it -> it.getSessionType() == SessionType.SESSION_1)
                .map(SessionCalendarEntity::getStartDate)
                .findFirst()
                .orElse(LocalDate.of(currentYear, chronologyProperties.session1Month(), chronologyProperties.session1Day()));
        if (!today.isBefore(session1Date)) {
            sessionMetadataRepository.deleteByYearLessThan(currentYear);
        }
    }

    private void runCatchUpScrapes(LocalDate today, int year, List<SessionCalendarEntity> calendar) {
        List<SessionCalendarEntity> dueSessions = calendar.stream()
                .filter(slot -> !today.isBefore(slot.getStartDate()))
                .sorted(Comparator.comparing(SessionCalendarEntity::getStartDate))
                .toList();

        for (SessionCalendarEntity due : dueSessions) {
            SessionMetadataEntity session = sessionMetadataRepository.findByYearAndSessionType(year, due.getSessionType())
                    .orElseThrow();
            if (session.getStatus() == SessionStatus.ACTIVE) {
                continue;
            }
            publishScrapeEventsForSession(session);
            session.setStatus(SessionStatus.ACTIVE);
            session.setActivatedAt(OffsetDateTime.now(clock));
            sessionMetadataRepository.save(session);
        }
    }

    private void publishScrapeEventsForSession(SessionMetadataEntity session) {
        List<MockScrapedDocument> docs = scraperService.scrape(session.getYear(), session.getSessionType(), session.getId());
        String key = session.getYear() + "-" + session.getSessionType().name();
        for (MockScrapedDocument doc : docs) {
            sessionDataProducer.publish(key, new SessionScrapedEvent(
                    session.getId(),
                    doc.documentId(),
                    session.getYear(),
                    session.getSessionType(),
                    doc.sourceUrl(),
                    doc.title(),
                    doc.rawText(),
                    doc.publishedAt()
            ));
        }
        log.info("Session scrape publish complete: sessionId={}, key={}, publishedDocuments={}",
                session.getId(), key, docs.size());
    }
}
