package com.policypulse.scrape;

import com.policypulse.persistence.entity.SessionDocumentEntity;
import com.policypulse.persistence.repository.SessionDocumentRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class HybridGovDataCollector {

    private static final String PRS_MONTHLY_URL = "https://prsindia.org/policy/monthly-policy-review";
    private static final Logger log = LoggerFactory.getLogger(HybridGovDataCollector.class);

    private final ResourceLoader resourceLoader;
    private final SessionDocumentRepository sessionDocumentRepository;
    private final EnglishTextFilter englishTextFilter;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private volatile IngestionDiagnosticsSnapshot lastDiagnostics = IngestionDiagnosticsSnapshot.empty();

    public HybridGovDataCollector(
            ResourceLoader resourceLoader,
            SessionDocumentRepository sessionDocumentRepository,
            EnglishTextFilter englishTextFilter) {
        this.resourceLoader = resourceLoader;
        this.sessionDocumentRepository = sessionDocumentRepository;
        this.englishTextFilter = englishTextFilter;
    }

    /**
     * Collect all available documents for a specific calendar month.
     * Sources: PRS India Monthly Policy Review PDFs + RSS feeds.
     */
    public List<ScrapedDocument> collectForMonth(int year, int month, long monthId) {
        List<ScrapedDocument> merged = new ArrayList<>();
        int[] rssStats = new int[]{0, 0, 0};  // checked, accepted, rejected
        int[] prsStats = new int[]{0, 0, 0};

        merged.addAll(collectRssDocuments(year, month, monthId, rssStats));
        merged.addAll(collectPrsPdfDocuments(year, month, monthId, prsStats));

        lastDiagnostics = new IngestionDiagnosticsSnapshot(
                OffsetDateTime.now(ZoneOffset.UTC),
                rssStats[0], rssStats[1], rssStats[2],
                prsStats[0], prsStats[1], prsStats[2]);

        log.info("Ingestion summary [year={} month={}]: rssChecked={}, rssAccepted={}, rssRejected={}, " +
                        "prsChecked={}, prsAccepted={}, prsRejected={}",
                year, month,
                rssStats[0], rssStats[1], rssStats[2],
                prsStats[0], prsStats[1], prsStats[2]);

        return merged;
    }

    public IngestionDiagnosticsSnapshot getLastDiagnostics() {
        return lastDiagnostics;
    }

    // ------------------------------------------------------------------
    // RSS ingestion — scoped to the specific year + month
    // ------------------------------------------------------------------

    private List<ScrapedDocument> collectRssDocuments(int year, int month, long monthId, int[] rssStats) {
        List<ScrapedDocument> docs = new ArrayList<>();
        for (String feedUrl : readRssLinks()) {
            rssStats[0]++;
            try (XmlReader reader = new XmlReader(URI.create(feedUrl).toURL())) {
                SyndFeed feed = new SyndFeedInput().build(reader);
                for (SyndEntry entry : feed.getEntries()) {
                    OffsetDateTime publishedAt = toOffsetDateTime(
                            entry.getPublishedDate() != null
                                    ? entry.getPublishedDate().toInstant()
                                    : Instant.now());

                    // Only accept articles from the exact target year+month
                    if (publishedAt.getYear() != year || publishedAt.getMonthValue() != month) {
                        rssStats[2]++;
                        continue;
                    }
                    String text = summarizeEntry(entry);
                    if (!englishTextFilter.isLikelyEnglish(text)) {
                        rssStats[2]++;
                        continue;
                    }
                    String url = entry.getLink() != null ? entry.getLink() : feedUrl;
                    String title = entry.getTitle() != null ? entry.getTitle() : "RSS update";
                    String fingerprint = fingerprint(url, publishedAt.toString(), title, text);
                    if (sessionDocumentRepository.existsByFingerprint(fingerprint)) {
                        rssStats[2]++;
                        continue;
                    }
                    Long docId = persistDocument(monthId, url, title, text, publishedAt, fingerprint);
                    docs.add(new ScrapedDocument(docId, url, title, text, publishedAt));
                    rssStats[1]++;
                }
            } catch (Exception ex) {
                log.warn("RSS feed fetch failed: {}", feedUrl, ex);
            }
        }
        return docs;
    }

    // ------------------------------------------------------------------
    // PRS India PDF ingestion — scoped to the specific year + month
    // ------------------------------------------------------------------

    private List<ScrapedDocument> collectPrsPdfDocuments(int year, int month, long monthId, int[] prsStats) {
        List<ScrapedDocument> docs = new ArrayList<>();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(PRS_MONTHLY_URL))
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                    "(KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .GET()
                    .build();
            String html = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
            Document page = Jsoup.parse(html, PRS_MONTHLY_URL);

            for (Element link : page.select("a[href]")) {
                String pdfUrl = link.absUrl("href").toLowerCase(Locale.ROOT);
                if (!pdfUrl.endsWith(".pdf") && !pdfUrl.contains(".pdf?")) {
                    continue;
                }
                prsStats[0]++;
                String absPdfUrl = link.absUrl("href");

                if (!isMonthlyPolicyReviewPdf(link, absPdfUrl, year, month)) {
                    prsStats[2]++;
                    continue;
                }

                String text = extractPdfText(absPdfUrl);
                if (text == null || text.isBlank() || !englishTextFilter.isLikelyEnglish(text)) {
                    prsStats[2]++;
                    continue;
                }

                OffsetDateTime publishedAt = OffsetDateTime.now(ZoneOffset.UTC);
                String title = link.text().trim();
                if (title.isBlank() || title.equalsIgnoreCase("Download") || title.equalsIgnoreCase("Read More")) {
                    title = "PRS Monthly Policy Review - "
                            + Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                            + " " + year;
                }

                String fingerprint = fingerprint(absPdfUrl, year + "-" + month, title, text);
                if (sessionDocumentRepository.existsByFingerprint(fingerprint)) {
                    prsStats[2]++;
                    continue;
                }
                Long docId = persistDocument(monthId, absPdfUrl, title, text, publishedAt, fingerprint);
                docs.add(new ScrapedDocument(docId, absPdfUrl, title, text, publishedAt));
                prsStats[1]++;
            }
        } catch (Exception ex) {
            log.warn("PRS monthly PDF scrape failed for year={} month={}", year, month, ex);
        }
        return docs;
    }

    /**
     * Strict matching for PRS Monthly Policy Review PDFs.
     * The URL must be from prsindia.org, must be a PDF, must contain the year,
     * and must contain the target month name (or abbreviation) to pin it to a specific month.
     */
    private boolean isMonthlyPolicyReviewPdf(Element link, String pdfUrl, int year, int month) {
        if (pdfUrl.isBlank()) return false;
        String url  = pdfUrl.toLowerCase(Locale.ROOT);
        String text = link.text().toLowerCase(Locale.ROOT);
        String yearToken  = String.valueOf(year);
        String monthFull  = Month.of(month).getDisplayName(java.time.format.TextStyle.FULL,  java.util.Locale.ENGLISH).toLowerCase(Locale.ROOT);
        String monthShort = Month.of(month).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH).toLowerCase(Locale.ROOT);

        if (!url.contains("prsindia.org")) return false;
        if (!url.contains(".pdf"))        return false;

        // Year must appear somewhere
        if (!url.contains(yearToken) && !text.contains(yearToken)) return false;

        // Must explicitly mention "monthly policy review"
        boolean isMPR = text.contains("monthly policy review")
                || url.contains("monthly-policy-review")
                || (url.contains("monthly") && url.contains("policy") && url.contains("review"));
        if (!isMPR) return false;

        // Month must appear in URL or anchor text
        return url.contains(monthFull) || url.contains(monthShort)
                || text.contains(monthFull) || text.contains(monthShort);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private List<String> readRssLinks() {
        try {
            Resource resource = resourceLoader.getResource("classpath:gov_dataset/rss.txt");
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            return content.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (Exception e) {
            log.warn("Could not read RSS feed list", e);
            return List.of();
        }
    }

    private String summarizeEntry(SyndEntry entry) {
        String desc = entry.getDescription() != null ? entry.getDescription().getValue() : "";
        return (entry.getTitle() + ". " + Jsoup.parse(desc).text()).trim();
    }

    private String extractPdfText(String pdfUrl) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(pdfUrl)).GET().build();
        byte[] pdfBytes = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray()).body();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private Long persistDocument(long monthId, String sourceUrl, String title, String text,
                                 OffsetDateTime publishedAt, String fingerprint) {
        SessionDocumentEntity entity = new SessionDocumentEntity();
        entity.setMonthId(monthId);
        entity.setSourceUrl(sourceUrl);
        entity.setTitle(title);
        entity.setRawText(text);
        entity.setPublishedAt(publishedAt);
        entity.setLanguage("EN");
        entity.setFingerprint(fingerprint);
        return sessionDocumentRepository.save(entity).getId();
    }

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private String fingerprint(String sourceUrl, String publishedAt, String title, String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(
                    (sourceUrl + "|" + publishedAt + "|" + title + "|" + text)
                            .getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return sourceUrl + ":" + publishedAt + ":" + title.hashCode();
        }
    }
}
