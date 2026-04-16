package com.policypulse.scrape;

import org.springframework.stereotype.Component;

@Component
public class EnglishTextFilter {

    public boolean isLikelyEnglish(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        int devanagari = 0;
        int latin = 0;
        char[] chars = text.toCharArray();
        for (char c : chars) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.DEVANAGARI) {
                devanagari++;
            }
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                latin++;
            }
        }
        return latin > 120 && devanagari < latin / 10;
    }
}
