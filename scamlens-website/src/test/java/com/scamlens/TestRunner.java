package com.scamlens;

import com.scamlens.analyzer.MessageAnalyzer;
import com.scamlens.analyzer.RiskScorer;
import com.scamlens.detector.CredentialDetector;
import com.scamlens.detector.Detector;
import com.scamlens.detector.FinancialDetector;
import com.scamlens.detector.RewardDetector;
import com.scamlens.detector.ThreatDetector;
import com.scamlens.detector.URLAnalyzer;
import com.scamlens.detector.UrgencyDetector;
import com.scamlens.model.AnalysisResult;
import com.scamlens.model.Category;
import com.scamlens.model.DashboardStats;
import com.scamlens.model.DetectionResult;
import com.scamlens.model.RiskLevel;
import com.scamlens.model.ScanRecord;
import com.scamlens.service.HistoryService;
import com.scamlens.util.InputValidator;
import com.scamlens.util.PreviewMaker;
import com.scamlens.util.TextHighlighter;
import com.scamlens.web.FormData;
import com.scamlens.web.Html;
import com.scamlens.web.WebServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * ScamLens test suite. Runs without JUnit or any download:  test.bat  (or ./test.sh)
 *
 * Each test has an ID, a description and a block of code. A test PASSES if its code
 * finishes, and FAILS if a check inside it is wrong (an AssertionError is thrown).
 * Expected results come from the PRD rules, not from running the code first.
 */
public class TestRunner {

    // ================= tiny test framework =================

    @FunctionalInterface
    interface TestBody { void run() throws Exception; }

    record TestCase(String id, String description, TestBody body) { }

    private static final List<TestCase> TESTS = new ArrayList<>();

    static void test(String id, String description, TestBody body) {
        TESTS.add(new TestCase(id, description, body));
    }

    static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionError(message);
    }

    static <T extends Throwable> T assertThrows(Class<T> type, TestBody body) {
        try {
            body.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) return type.cast(thrown);
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + thrown);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }

    // ================= shared objects =================

    static final MessageAnalyzer ANALYZER = new MessageAnalyzer();
    static final RiskScorer SCORER = new RiskScorer();

    static AnalysisResult analyze(String message) {
        return ANALYZER.analyze(message);
    }

    static boolean flags(Detector detector, String message) {
        return detector.detect(message).isPresent();
    }

    static DetectionResult hit(Category category) {
        return new DetectionResult(category, List.of("evidence"), "test");
    }

    static String urlExplanation(String message) {
        return new URLAnalyzer().detect(message)
                .orElseThrow(() -> new AssertionError("expected a URL detection for: " + message))
                .explanation();
    }

    // ================= 1. the 15 required test cases =================

    static void requiredCases() {
        test("TC01", "Normal legitimate message -> no indicators, 0, LOW", () -> {
            AnalysisResult r = analyze("Hi Priya, the team meeting has moved to 3 pm tomorrow in room 204. Please bring your laptop.");
            assertEquals(List.of(), r.categories());
            assertEquals(0, r.score());
            assertEquals(RiskLevel.LOW, r.riskLevel());
        });
        test("TC02", "Urgency language -> URGENCY, 10, LOW", () -> {
            AnalysisResult r = analyze("Please respond immediately, this is urgent.");
            assertEquals(List.of(Category.URGENCY), r.categories());
            assertEquals(10, r.score());
            assertEquals(RiskLevel.LOW, r.riskLevel());
        });
        test("TC03", "Threatening language -> THREAT, 15", () -> {
            AnalysisResult r = analyze("Your account will be blocked and legal action will be taken against you.");
            assertEquals(List.of(Category.THREAT), r.categories());
            assertEquals(15, r.score());
        });
        test("TC04", "Fake reward -> REWARD, 10", () -> {
            AnalysisResult r = analyze("Congratulations! You have won a free iPhone in our lucky draw. Claim your prize.");
            assertEquals(List.of(Category.REWARD), r.categories());
            assertEquals(10, r.score());
        });
        test("TC05", "OTP/password request -> CREDENTIAL, 25", () -> {
            AnalysisResult r = analyze("Please share your OTP and enter your password to verify your identity.");
            assertEquals(List.of(Category.CREDENTIAL), r.categories());
            assertEquals(25, r.score());
        });
        test("TC06", "Suspicious URL (IP address over http) -> URL, 25", () -> {
            AnalysisResult r = analyze("Track your parcel here: http://192.168.4.21/track");
            assertEquals(List.of(Category.URL), r.categories());
            assertEquals(25, r.score());
            String explanation = r.detections().get(0).explanation();
            assertTrue(explanation.contains("raw IP address"), explanation);
            assertTrue(explanation.contains("http://"), explanation);
        });
        test("TC07", "Financial manipulation -> FINANCIAL, 15", () -> {
            AnalysisResult r = analyze("To release your refund, transfer money and pay the processing fee.");
            assertEquals(List.of(Category.FINANCIAL), r.categories());
            assertEquals(15, r.score());
        });
        test("TC08", "Multiple indicators -> 4 categories, 75, HIGH", () -> {
            AnalysisResult r = analyze("URGENT! Your account has been suspended. Share your OTP at http://sbi-secure-login.xyz "
                    + "immediately or face legal action.");
            assertEquals(List.of(Category.URGENCY, Category.THREAT, Category.CREDENTIAL, Category.URL), r.categories());
            assertEquals(75, r.score());
            assertEquals(RiskLevel.HIGH, r.riskLevel());
        });
        test("TC09", "Empty input -> rejected with a message", () -> {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> analyze(""));
            assertTrue(e.getMessage().contains("enter or paste"), e.getMessage());
            assertThrows(IllegalArgumentException.class, () -> analyze(null));
        });
        test("TC10", "Whitespace-only input -> rejected", () ->
                assertThrows(IllegalArgumentException.class, () -> analyze("   \n\t   ")));
        test("TC11a", "Input over 5,000 characters -> rejected", () -> {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> analyze("a".repeat(InputValidator.MAX_LENGTH + 1)));
            assertTrue(e.getMessage().contains("too long"), e.getMessage());
        });
        test("TC11b", "Input near the 5,000 limit -> accepted in under 1 second", () -> {
            String message = "This is ordinary text. ".repeat(220).substring(0, 4990) + " Act now";
            long start = System.nanoTime();
            AnalysisResult r = analyze(message);
            long millis = (System.nanoTime() - start) / 1_000_000;
            assertEquals(List.of(Category.URGENCY), r.categories());
            assertTrue(millis < 1000, "took " + millis + " ms");
        });
        test("TC12", "Malformed URL -> no crash, flagged as malformed", () -> {
            AnalysisResult r = analyze("Login now at http://secure-bank%zz.com/login to continue");
            assertEquals(List.of(Category.URL), r.categories());
            assertTrue(r.detections().get(0).explanation().contains("malformed"), r.detections().get(0).explanation());
        });
        test("TC13", "Legitimate message mentioning a bank -> not flagged, 0", () -> {
            AnalysisResult r = analyze("Your HDFC Bank account statement for October is ready. "
                    + "View it at https://www.hdfcbank.com or in the HDFC Bank app.");
            assertEquals(List.of(), r.categories());
            assertEquals(0, r.score());
        });
        test("TC14", "Legitimate message mentioning an OTP -> not flagged, 0", () -> {
            AnalysisResult r = analyze("482913 is your OTP to log in to the SBI YONO app. Never share your OTP with anyone.");
            assertEquals(List.of(), r.categories());
            assertEquals(0, r.score());
        });
        test("TC15", "Repeated phrases from one category -> counted once, 10", () -> {
            AnalysisResult r = analyze("Urgent! Urgent! Act now, act now, reply immediately, within 24 hours!");
            assertEquals(List.of(Category.URGENCY), r.categories());
            assertEquals(10, r.score());
            assertTrue(r.detections().get(0).evidence().size() >= 3, "several pieces of evidence");
        });
    }

    // ================= 2. scoring =================

    static void scoringTests() {
        test("SC01", "Each category uses its PRD weight", () -> {
            assertEquals(10, SCORER.calculateScore(List.of(hit(Category.URGENCY))));
            assertEquals(15, SCORER.calculateScore(List.of(hit(Category.THREAT))));
            assertEquals(10, SCORER.calculateScore(List.of(hit(Category.REWARD))));
            assertEquals(25, SCORER.calculateScore(List.of(hit(Category.CREDENTIAL))));
            assertEquals(15, SCORER.calculateScore(List.of(hit(Category.FINANCIAL))));
            assertEquals(25, SCORER.calculateScore(List.of(hit(Category.URL))));
        });
        test("SC02", "No detections -> 0; same category twice counted once", () -> {
            assertEquals(0, SCORER.calculateScore(List.of()));
            assertEquals(15, SCORER.calculateScore(List.of(hit(Category.THREAT), hit(Category.THREAT))));
        });
        test("SC03", "All six categories -> exactly 100", () ->
                assertEquals(100, SCORER.calculateScore(Arrays.stream(Category.values()).map(TestRunner::hit).toList())));
        test("SC04", "Combo rule: credential + link, or credential + money -> at least 65", () -> {
            assertEquals(65, SCORER.calculateScore(List.of(hit(Category.CREDENTIAL), hit(Category.URL))));
            assertEquals(65, SCORER.calculateScore(List.of(hit(Category.CREDENTIAL), hit(Category.FINANCIAL))));
            assertEquals(75, SCORER.calculateScore(List.of(hit(Category.CREDENTIAL), hit(Category.URL),
                    hit(Category.THREAT), hit(Category.URGENCY))));
        });
        test("SC05", "Level boundaries: 30 LOW, 31 SUSPICIOUS, 60 SUSPICIOUS, 61 HIGH", () -> {
            assertEquals(RiskLevel.LOW, RiskLevel.fromScore(30));
            assertEquals(RiskLevel.SUSPICIOUS, RiskLevel.fromScore(31));
            assertEquals(RiskLevel.SUSPICIOUS, RiskLevel.fromScore(60));
            assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(61));
            assertThrows(IllegalArgumentException.class, () -> RiskLevel.fromScore(101));
        });
        test("SC06", "Same input always gives the same result", () -> {
            String msg = "URGENT: pay the processing fee now at http://1.2.3.4/pay";
            assertEquals(analyze(msg).score(), analyze(msg).score());
            assertEquals(analyze(msg).categories(), analyze(msg).categories());
        });
    }

    // ================= 3. detectors and context rules =================

    static void detectorTests() {
        test("DT01", "Urgency: case-insensitive, finds 'within 24 hours'", () -> {
            assertTrue(flags(new UrgencyDetector(), "ACT NOW before it is too late"), "ACT NOW");
            assertTrue(flags(new UrgencyDetector(), "Reply within 24 hours"), "within 24 hours");
            assertFalse(flags(new UrgencyDetector(), "See you next week"), "normal text");
        });
        test("DT02", "Threat: blocked accounts and disconnections, not balances", () -> {
            assertTrue(flags(new ThreatDetector(), "Your account has been suspended"), "suspended");
            assertTrue(flags(new ThreatDetector(), "Your electricity will be disconnected tonight"), "disconnected");
            assertTrue(flags(new ThreatDetector(), "If you do not verify, your account will be blocked."), "negation in earlier clause");
            assertFalse(flags(new ThreatDetector(), "Your account balance is Rs 2,000"), "balance");
        });
        test("DT03", "Credential: a REQUEST is flagged", () -> {
            assertTrue(flags(new CredentialDetector(), "Enter the 6-digit OTP on our page"), "enter OTP");
            assertTrue(flags(new CredentialDetector(), "Kindly send us your banking details"), "bank details");
            assertTrue(flags(new CredentialDetector(), "SHARE YOUR OTP"), "upper case");
        });
        test("DT04", "Credential: a mention or a negated request is NOT flagged", () -> {
            assertFalse(flags(new CredentialDetector(), "Your OTP is 123456"), "mention");
            assertFalse(flags(new CredentialDetector(), "Do not share your OTP with anyone"), "do not");
            assertFalse(flags(new CredentialDetector(), "Never share your PIN"), "never");
            assertFalse(flags(new CredentialDetector(), "I went shopping"), "pin inside shopping");
        });
        test("DT05", "Financial: payment requests flagged, receipts not", () -> {
            assertTrue(flags(new FinancialDetector(), "Pay a processing fee of Rs 99"), "fee");
            assertFalse(flags(new FinancialDetector(), "Payment of Rs 500 received. Thank you."), "receipt");
        });
        test("DT06", "Regression BUG-1: \"you won't believe\" is not a reward claim", () -> {
            assertFalse(flags(new RewardDetector(), "You won't believe this view!"), "won't");
            assertTrue(flags(new RewardDetector(), "You won a prize"), "won");
        });
        test("DT07", "Regression BUG-2: \"account suspension\" is a threat", () ->
                assertTrue(flags(new ThreatDetector(), "Update your PAN to avoid account suspension"), "suspension"));
    }

    // ================= 4. URL analysis =================

    static void urlTests() {
        test("URL01", "Link shortener is flagged", () ->
                assertTrue(urlExplanation("Click bit.ly/3xYzAb now").contains("shortener"), "shortener"));
        test("URL02", "Official https link is NOT flagged", () ->
                assertFalse(new URLAnalyzer().detect("Track your order at https://www.amazon.in/orders").isPresent(), "amazon"));
        test("URL03", "Brand name on a fake domain is flagged", () ->
                assertTrue(urlExplanation("Claim at https://amazon-prize-claim.com").contains("brand name"), "brand"));
        test("URL04", "Number-for-letter lookalike (paypa1) is flagged", () ->
                assertTrue(urlExplanation("Login: https://paypa1.com/login").contains("swaps letters for numbers"), "lookalike"));
        test("URL05", "@ trick is flagged", () ->
                assertTrue(urlExplanation("Visit http://www.sbi.co.in@evil-site.com/").contains("@ sign"), "@"));
        test("URL06", "Risky ending (.xyz) is flagged; trailing full stop ignored", () -> {
            assertTrue(urlExplanation("Go to https://rewards-center.xyz").contains("domain ending"), ".xyz");
            assertFalse(new URLAnalyzer().detect("Search on https://www.google.com.").isPresent(), "google.com.");
        });
    }

    // ================= 5. helpers =================

    static void utilTests() {
        test("UT01", "Validator rejects empty, symbols-only and too-long input", () -> {
            assertTrue(InputValidator.validate("").isPresent(), "empty");
            assertTrue(InputValidator.validate("!!! ???").isPresent(), "symbols");
            assertTrue(InputValidator.validate("x".repeat(InputValidator.MAX_LENGTH + 1)).isPresent(), "too long");
            assertFalse(InputValidator.validate("Hello there").isPresent(), "normal");
        });
        test("UT02", "Preview masks digits and links, max 40 characters", () -> {
            assertEquals("Share OTP ###### at [link] now", PreviewMaker.make("Share OTP 482913 at http://x.xyz now"));
            assertEquals(PreviewMaker.PREVIEW_LENGTH + 1, PreviewMaker.make("a".repeat(100)).length());
        });
        test("UT03", "Highlighter splits text into normal and highlighted parts", () ->
                assertEquals(List.of(
                        new TextHighlighter.Segment("Act now", true),
                        new TextHighlighter.Segment(" and ", false),
                        new TextHighlighter.Segment("share your OTP", true)),
                        TextHighlighter.split("Act now and share your OTP", List.of("act now", "share your otp"))));
    }

    // ================= 6. history file =================

    static AnalysisResult result(int score, RiskLevel level, List<Category> categories, String message) {
        List<DetectionResult> detections = categories.stream().map(TestRunner::hit).toList();
        return new AnalysisResult(message, score, level, detections, List.of(), LocalDateTime.now());
    }

    static void historyTests() {
        test("DB01", "History file and folder are created automatically; starts empty", () -> {
            Path file = Files.createTempDirectory("scamlens").resolve("new/folder/history.txt");
            HistoryService history = new HistoryService(file);
            assertTrue(Files.exists(file), "file exists");
            assertEquals(List.of(), history.findAll());
            assertEquals(new DashboardStats(0, 0, 0, 0), history.getStats());
        });
        test("DB02", "Save then read back: every field matches, preview is masked", () -> {
            HistoryService history = new HistoryService(Files.createTempDirectory("scamlens").resolve("h.txt"));
            long id = history.save(result(75, RiskLevel.HIGH, List.of(Category.URGENCY, Category.URL),
                    "Act now at http://x.xyz, OTP 123456"), true);
            ScanRecord r = history.findAll().get(0);
            assertEquals(id, r.id());
            assertEquals(75, r.score());
            assertEquals(RiskLevel.HIGH, r.riskLevel());
            assertEquals(List.of(Category.URGENCY, Category.URL), r.categories());
            assertEquals("Act now at [link] OTP ######", r.preview());
        });
        test("DB03", "Privacy: with preview off, no message text is stored", () -> {
            Path file = Files.createTempDirectory("scamlens").resolve("h.txt");
            HistoryService history = new HistoryService(file);
            history.save(result(0, RiskLevel.LOW, List.of(), "my secret message 999"), false);
            assertEquals(null, history.findAll().get(0).preview());
            assertFalse(Files.readString(file).contains("secret"), "file must not contain the message");
        });
        test("DB04", "Delete one, delete a missing id, then clear all", () -> {
            HistoryService history = new HistoryService(Files.createTempDirectory("scamlens").resolve("h.txt"));
            long first = history.save(result(10, RiskLevel.LOW, List.of(Category.URGENCY), "a"), false);
            history.save(result(50, RiskLevel.SUSPICIOUS, List.of(Category.URL), "b"), false);
            assertTrue(history.delete(first), "first delete");
            assertEquals(1, history.findAll().size());
            assertFalse(history.delete(first), "second delete of same id");
            assertEquals(1, history.deleteAll());
            assertEquals(List.of(), history.findAll());
        });
        test("DB05", "Dashboard stats are counted from real saved scans", () -> {
            HistoryService history = new HistoryService(Files.createTempDirectory("scamlens").resolve("h.txt"));
            history.save(result(75, RiskLevel.HIGH, List.of(), "a"), false);
            history.save(result(80, RiskLevel.HIGH, List.of(), "b"), false);
            history.save(result(40, RiskLevel.SUSPICIOUS, List.of(), "c"), false);
            history.save(result(0, RiskLevel.LOW, List.of(), "d"), false);
            assertEquals(new DashboardStats(4, 2, 1, 1), history.getStats());
            assertEquals(3, history.findRecent(3).size());
        });
    }

    // ================= 7. website =================

    static void webTests() {
        test("WEB01", "Html.escape stops scripts from running", () ->
                assertEquals("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;", Html.escape("<script>alert('x')</script>")));
        test("WEB02", "Form data is decoded correctly", () -> {
            FormData form = FormData.parse("message=Act+now%21+%3Cb%3E&savePreview=on");
            assertEquals("Act now! <b>", form.get("message").orElse(""));
            assertTrue(form.isChecked("savePreview"), "checkbox");
            assertFalse(form.isChecked("other"), "missing field");
        });
        test("WEB03", "Full website: pages load, analysis works, history updates", () -> {
            Path folder = Files.createTempDirectory("scamlens-web");
            Path publicFolder = Files.createDirectories(folder.resolve("public"));
            Files.writeString(publicFolder.resolve("styles.css"), "body{}");
            HistoryService history = new HistoryService(folder.resolve("h.txt"));
            WebServer server = new WebServer(new MessageAnalyzer(), history, publicFolder);
            server.start(0);   // 0 = any free port
            try {
                HttpClient client = HttpClient.newHttpClient();
                String base = "http://localhost:" + server.port();
                assertEquals(200, get(client, base + "/").statusCode());
                assertEquals(200, get(client, base + "/static/styles.css").statusCode());
                assertEquals(404, get(client, base + "/static/../h.txt").statusCode());

                HttpResponse<String> empty = post(client, base + "/analyze", "message=+++");
                assertEquals(400, empty.statusCode());
                assertTrue(empty.body().contains("Please enter or paste"), "validation shown");

                HttpResponse<String> scam = post(client, base + "/analyze",
                        "message=" + java.net.URLEncoder.encode(
                                "URGENT: share your OTP at bit.ly/x <script>alert(1)</script>",
                                java.nio.charset.StandardCharsets.UTF_8));
                assertEquals(200, scam.statusCode());
                assertTrue(scam.body().contains("High risk"), "high risk shown");
                assertFalse(scam.body().contains("<script>alert(1)"), "script must be escaped");
                assertEquals(1, history.findAll().size());
                assertTrue(get(client, base + "/dashboard").body().contains("Based on 1 saved check"), "dashboard");
            } finally {
                server.stop();
            }
        });
    }

    // ================= 8. Swing desktop app =================

    static void desktopTests() {
        test("DESK01", "Swing: typing a scam and clicking Check message runs the engine and saves it", () -> {
            HistoryService history = new HistoryService(Files.createTempDirectory("scamlens-desk").resolve("h.txt"));
            List<AnalysisResult> received = new ArrayList<>();
            com.scamlens.desktop.ScannerPanel scanner = new com.scamlens.desktop.ScannerPanel(
                    new MessageAnalyzer(), history, (result, status) -> received.add(result));
            scanner.setMessage("URGENT! Share your OTP at bit.ly/verify now");
            scanner.clickCheck();
            assertEquals(1, received.size());
            assertEquals(RiskLevel.HIGH, received.get(0).riskLevel());
            assertEquals(1, history.findAll().size());
        });
        test("DESK02", "Swing: an empty message shows an error and opens no result", () -> {
            HistoryService history = new HistoryService(Files.createTempDirectory("scamlens-desk").resolve("h.txt"));
            List<AnalysisResult> received = new ArrayList<>();
            com.scamlens.desktop.ScannerPanel scanner = new com.scamlens.desktop.ScannerPanel(
                    new MessageAnalyzer(), history, (result, status) -> received.add(result));
            scanner.setMessage("   ");
            scanner.clickCheck();
            assertEquals(0, received.size());
            assertTrue(scanner.noteText().contains("Please enter or paste"), scanner.noteText());
            assertEquals(0, history.findAll().size());
        });
        test("DESK03", "Swing: the logo draws without errors at every icon size", () -> {
            List<java.awt.Image> icons = com.scamlens.desktop.LogoIcon.windowIcons();
            assertEquals(6, icons.size());
            java.awt.image.BufferedImage big = (java.awt.image.BufferedImage) icons.get(5);
            assertTrue(big.getRGB(64, 10) != 0, "the logo should be drawn, not blank");
        });
    }

    static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    static HttpResponse<String> post(HttpClient client, String url, String form) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
    }

    // ================= run everything =================

    public static void main(String[] args) {
        requiredCases();
        scoringTests();
        detectorTests();
        urlTests();
        utilTests();
        historyTests();
        webTests();
        desktopTests();

        int passed = 0;
        List<String> failures = new ArrayList<>();
        System.out.println();
        System.out.println("ScamLens tests");
        System.out.println("==============");
        for (TestCase t : TESTS) {
            try {
                t.body().run();
                passed++;
                System.out.printf("  PASS  %-6s %s%n", t.id(), t.description());
            } catch (Throwable e) {
                failures.add(t.id());
                System.out.printf("  FAIL  %-6s %s%n         -> %s%n", t.id(), t.description(), e.getMessage());
            }
        }
        System.out.println();
        System.out.printf("RESULT: %d passed, %d failed, %d total%n", passed, failures.size(), TESTS.size());
        if (!failures.isEmpty()) {
            System.out.println("Failed: " + String.join(", ", failures));
            System.exit(1);
        }
    }
}
