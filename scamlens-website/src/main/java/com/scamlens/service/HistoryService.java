package com.scamlens.service;

import com.scamlens.model.AnalysisResult;
import com.scamlens.model.Category;
import com.scamlens.model.DashboardStats;
import com.scamlens.model.RiskLevel;
import com.scamlens.model.ScanRecord;
import com.scamlens.util.PreviewMaker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Saves, loads and deletes scan history, and calculates dashboard statistics.
 *
 * The history is stored in a plain text file (data/scan-history.txt), one scan per line:
 *
 *     id <TAB> date-time <TAB> score <TAB> level <TAB> categories <TAB> preview
 *     1     2026-11-24T22:40:05   75         HIGH        URGENCY,THREAT,URL      URGENT: Your SBI account…
 *
 * PRIVACY: the full message is never stored. The preview is optional, at most
 * 40 characters, with digits turned into # and links into [link].
 *
 * The web pages never touch this file directly; they call these methods.
 */
public class HistoryService {

    private static final String SEPARATOR = "\t";

    private final Path file;

    public HistoryService(Path file) throws IOException {
        this.file = file.toAbsolutePath();
        if (this.file.getParent() != null) {
            Files.createDirectories(this.file.getParent());   // make the data folder if it is missing
        }
        if (Files.notExists(this.file)) {
            Files.createFile(this.file);                       // start with an empty history
        }
    }

    /** Default location: the "data" folder next to where the app is started. */
    public static Path defaultFile() {
        return Path.of("data", "scan-history.txt");
    }

    /**
     * Saves one scan and returns its new id.
     * @param savePreview true = store a short masked preview, false = store no message text at all
     */
    public synchronized long save(AnalysisResult result, boolean savePreview) throws IOException {
        long id = findAll().stream()
                .mapToLong(ScanRecord::id)
                .max()
                .orElse(0) + 1;                                // next id = biggest id + 1

        String categories = result.categories().stream()
                .map(Category::name)
                .collect(Collectors.joining(","));
        String preview = savePreview ? PreviewMaker.make(result.message()) : "";

        String line = String.join(SEPARATOR,
                String.valueOf(id),
                result.analysedAt().withNano(0).toString(),
                String.valueOf(result.score()),
                result.riskLevel().name(),
                categories,
                preview);

        Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);
        return id;
    }

    /** All scans, newest first. Returns an empty list if there are none. */
    public synchronized List<ScanRecord> findAll() throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank())
                .map(HistoryService::parseLine)
                .sorted(Comparator.comparing(ScanRecord::scannedAt)
                        .thenComparing(ScanRecord::id)
                        .reversed())
                .toList();
    }

    /** The newest scans, up to the given limit. */
    public List<ScanRecord> findRecent(int limit) throws IOException {
        return findAll().stream().limit(limit).toList();
    }

    /** Deletes one scan. Returns true if it existed. */
    public synchronized boolean delete(long id) throws IOException {
        List<ScanRecord> all = findAll();
        List<ScanRecord> kept = all.stream()
                .filter(record -> record.id() != id)
                .toList();
        if (kept.size() == all.size()) {
            return false;                                      // nothing had that id
        }
        rewrite(kept);
        return true;
    }

    /** Deletes every scan. Returns how many were deleted. */
    public synchronized int deleteAll() throws IOException {
        int count = findAll().size();
        Files.writeString(file, "", StandardCharsets.UTF_8);
        return count;
    }

    /** Dashboard numbers, counted from the real saved scans. */
    public DashboardStats getStats() throws IOException {
        // FUNCTIONAL: group the scans by risk level and count each group
        Map<RiskLevel, Long> counts = findAll().stream()
                .collect(Collectors.groupingBy(ScanRecord::riskLevel, Collectors.counting()));

        Function<RiskLevel, Integer> countOf = level -> counts.getOrDefault(level, 0L).intValue();
        int total = countOf.apply(RiskLevel.HIGH) + countOf.apply(RiskLevel.SUSPICIOUS) + countOf.apply(RiskLevel.LOW);
        return new DashboardStats(total, countOf.apply(RiskLevel.HIGH),
                countOf.apply(RiskLevel.SUSPICIOUS), countOf.apply(RiskLevel.LOW));
    }

    // ---------- helpers ----------

    /** Writes the given records back to the file, oldest first. */
    private void rewrite(List<ScanRecord> records) throws IOException {
        List<String> lines = records.stream()
                .sorted(Comparator.comparing(ScanRecord::id))
                .map(r -> String.join(SEPARATOR,
                        String.valueOf(r.id()),
                        r.scannedAt().toString(),
                        String.valueOf(r.score()),
                        r.riskLevel().name(),
                        r.categories().stream().map(Category::name).collect(Collectors.joining(",")),
                        r.preview() == null ? "" : r.preview()))
                .toList();
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /** Turns one line of the file back into a ScanRecord. */
    static ScanRecord parseLine(String line) {
        String[] parts = line.split(SEPARATOR, -1);            // -1 keeps empty fields
        if (parts.length < 6) {
            throw new IllegalStateException("Damaged history line: " + line);
        }
        String preview = parts[5].isEmpty() ? null : parts[5];
        return new ScanRecord(
                Long.parseLong(parts[0]),
                LocalDateTime.parse(parts[1]),
                Integer.parseInt(parts[2]),
                RiskLevel.valueOf(parts[3]),
                parseCategories(parts[4]),
                preview);
    }

    /** "URGENCY,URL" -> [URGENCY, URL] */
    static List<Category> parseCategories(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(Category::valueOf)
                .toList();
    }
}
