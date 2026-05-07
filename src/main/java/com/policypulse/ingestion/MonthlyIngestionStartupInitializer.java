package com.policypulse.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MonthlyIngestionStartupInitializer {

    private static final Logger log = LoggerFactory.getLogger(MonthlyIngestionStartupInitializer.class);
    private final MonthlyIngestionService monthlyIngestionService;

    public MonthlyIngestionStartupInitializer(MonthlyIngestionService monthlyIngestionService) {
        this.monthlyIngestionService = monthlyIngestionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Application ready — bootstrapping rolling 24-month ingestion window");
        monthlyIngestionService.ensureRollingWindow();
    }
}
