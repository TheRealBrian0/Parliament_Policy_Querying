package com.policypulse.chronology;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ChronologyStartupInitializer {

    private final ChronologyService chronologyService;

    public ChronologyStartupInitializer(ChronologyService chronologyService) {
        this.chronologyService = chronologyService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        chronologyService.reconcileOnStartup();
    }
}
