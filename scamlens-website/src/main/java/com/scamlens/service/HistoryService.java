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
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Saves, loads and deletes scan history, and calculates dashboard statistics.
 *
 * The history is stored in a plain text file (data/scan-history.txt), one scan per line:
 *
 *     id <TAB> date-time <TAB> score <TAB> level <TAB> categories <TAB> preview <TAB> owner
 *     1     2026-11-24T22:40:05   75         HIGH        URGENCY,THREAT,URL      URGENT: Your…   3f2a9c1e-…
 *
 * PRIVACY: the full message is never stored. The preview is optional, at most
 * 40 characters, with digits turned into # and links into [link].
 *
 * OWNERS: on the website every browser gets a random visitor id (a cookie), and each
 * scan is saved with that id as its owner. The website only ever asks for one owner's
 * scans, so people see and delete only their own history.
 * The desktop app is used by one person on their own computer, so it saves with the
 * owner "desktop" and uses ALL_OWNERS to see every scan in the local file.
 *
 * The web pages never touch this file directly; they call these methods.
 */
public class HistoryService {

    private static final String SEPARATOR = "\t";

    /** Pass this as the owner to work with every scan in the file (used by the desktop app). */
    public static final String ALL_OWNERS = null;

    /** Owner name used for scans made in the Swing desktop app. */
    public static final String DESKTOP_OWNER = "desktop";

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
     * @param owner       who made the scan: a website visitor id, or DESKTOP_OWNER
     */
    public synchronized long save(AnalysisResult result, boolean savePreview, String owner) throws IOException {
        if (owner == null || owner.isBlank() || owner.contains(SEPARATOR) || owner.contains("\n")) {
            throw new IllegalArgumentException("Every saved scan needs a simple owner id");
        }
        long id = findAll(ALL_OWNERS).stream()
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
                preview,
                owner);

        Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);
        return id;
    }

    /** One owner's scans (or every scan for ALL_OWNERS), newest first. Empty list if there are none. */
    public synchronized List<ScanRecord> findAll(String owner) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank())
                .map(HistoryService::parseLine)
                .filter(belongsTo(owner))
                .sorted(Comparator.comparing(ScanRecord::scannedAt)
                        .thenComparing(ScanRecord::id)
                        .reversed())
                .toList();
    }

    /** One owner's newest scans, up to the given limit. */
    public List<ScanRecord> findRecent(String owner, int limit) throws IOException {
        return findAll(owner).stream().limit(limit).toList();
    }

    /**
     * Deletes one scan, but only if it belongs to this owner.
     * Returns true if it was deleted. Someone else's id is treated as "not found".
     */
    public synchronized boolean delete(long id, String owner) throws IOException {
        List<ScanRecord> all = findAll(ALL_OWNERS);
        List<ScanRecord> kept = all.stream()
                .filter(record -> record.id() != id || !belongsTo(owner).test(record))
                .toList();
        if (kept.size() == all.size()) {
            return false;                                      // nothing had that id
        }
        rewrite(kept);
        return true;
    }

    /** Deletes every scan of this owner (every scan in the file for ALL_OWNERS). Returns how many. */
    public synchronized int deleteAll(String owner) throws IOException {
        List<ScanRecord> all = findAll(ALL_OWNERS);
        List<ScanRecord> kept = all.stream()
                .filter(belongsTo(owner).negate())
                .toList();
        rewrite(kept);
        return all.size() - kept.size();
    }

    /** One owner's dashboard numbers, counted from their real saved scans. */
    public DashboardStats getStats(String owner) throws IOException {
        // FUNCTIONAL: group the scans by risk level and count each group
        Map<RiskLevel, Long> counts = findAll(owner).stream()
                .collect(Collectors.groupingBy(ScanRecord::riskLevel, Collectors.counting()));

        Function<RiskLevel, Integer> countOf = level -> counts.getOrDefault(level, 0L).intValue();
        int total = countOf.apply(RiskLevel.HIGH) + countOf.apply(RiskLevel.SUSPICIOUS) + countOf.apply(RiskLevel.LOW);
        return new DashboardStats(total, countOf.apply(RiskLevel.HIGH),
                countOf.apply(RiskLevel.SUSPICIOUS), countOf.apply(RiskLevel.LOW));
    }

    // ---------- helpers ----------

    /** FUNCTIONAL: a Predicate that answers "does this scan belong to this owner?" */
    private static Predicate<ScanRecord> belongsTo(String owner) {
        return record -> owner == ALL_OWNERS || owner.equals(record.owner());
    }

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
                        r.preview() == null ? "" : r.preview(),
                        r.owner()))
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
        // Lines saved before owners existed have no 7th field; they count as desktop scans
        String owner = parts.length > 6 && !parts[6].isEmpty() ? parts[6] : DESKTOP_OWNER;
        return new ScanRecord(
                Long.parseLong(parts[0]),
                LocalDateTime.parse(parts[1]),
                Integer.parseInt(parts[2]),
                RiskLevel.valueOf(parts[3]),
                parseCategories(parts[4]),
                preview,
                owner);
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
