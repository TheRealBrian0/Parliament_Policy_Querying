package com.policypulse.scrape;

import com.policypulse.domain.SessionType;
import org.springframework.stereotype.Service;

@Service
public class SessionScraperService {

    private final HybridGovDataCollector collector;

    public SessionScraperService(HybridGovDataCollector collector) {
        this.collector = collector;
    }

    public java.util.List<MockScrapedDocument> scrape(int year, SessionType sessionType, long sessionId) {
        return collector.collectForSession(year, sessionType, sessionId);
    }
}
