package com.policypulse.domain;

/**
 * Broad professional lenses for prompt conditioning (simple RAG input only).
 */
public enum Persona {
    /** Retail, wholesale, street vendors, small shops, MSMEs */
    TRADE_AND_SMALL_BUSINESS,
    /** Manufacturing, industrial, supply chain, transport, logistics */
    INDUSTRY_AND_LOGISTICS,
    /** Banking, finance, law, accounting, tech consulting, corporate roles */
    FINANCE_AND_PROFESSIONAL,
    /** Public sector, NGOs, healthcare, education staff, civic engagement */
    PUBLIC_AND_COMMUNITY,
    /** Students, teachers, researchers, academic staff */
    STUDENT_AND_EDUCATOR,
    /** Everyday consumer and general public perspective */
    GENERAL_CITIZEN
}
