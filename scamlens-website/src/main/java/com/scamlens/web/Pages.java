package com.scamlens.web;

import com.scamlens.model.AnalysisResult;
import com.scamlens.model.Category;
import com.scamlens.model.DashboardStats;
import com.scamlens.model.DetectionResult;
import com.scamlens.model.ScanRecord;
import com.scamlens.util.InputValidator;
import com.scamlens.util.SampleMessages;
import com.scamlens.util.TextHighlighter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Builds the HTML for every page of the website (the PRESENTATION layer).
 *
 * Each method takes data and returns a complete HTML page as a String.
 * It contains no detection logic and no file access: it only displays
 * what WebServer hands to it. All user text goes through Html.escape().
 */
public final class Pages {

    private Pages() { }

    // =====================================================================
    // SHARED LAYOUT: header + page content + footer
    // =====================================================================

    static String layout(String title, String activeNav, String content) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s | ScamLens</title>
                  <link rel="icon" type="image/svg+xml" href="/static/logo.svg">
                  <link rel="stylesheet" href="/static/styles.css">
                </head>
                <body>
                  <header class="header">
                    <a class="brand" href="/" aria-label="ScamLens home">
                      <img class="brand-logo" src="/static/logo.svg" width="34" height="34" alt="">
                      <span>Scam<span class="tint">Lens</span></span>
                    </a>
                    <nav class="nav" aria-label="Main">
                      %s
                      %s
                      %s
                    </nav>
                  </header>
                  <main>
                    %s
                  </main>
                  <footer class="footer">
                    <span>ScamLens · Rule-based scam message detection</span>
                  </footer>
                  <script src="/static/app.js"></script>
                </body>
                </html>
                """.formatted(
                Html.escape(title),
                navLink("/", "Scanner", activeNav),
                navLink("/dashboard", "Dashboard", activeNav),
                navLink("/history", "History", activeNav),
                content);
    }

    private static String navLink(String href, String label, String activeNav) {
        String current = label.equals(activeNav) ? " aria-current=\"page\"" : "";
        return "<a href=\"" + href + "\"" + current + ">" + label + "</a>";
    }

    // =====================================================================
    // 1. SCANNER
    // =====================================================================

    /**
     * @param message     text to put back in the box (after an error or an example), or ""
     * @param error       validation message to show, or null
     * @param sampleIndex which example is loaded (0-based), or -1 if none
     */
    public static String scanner(String message, String error, int sampleIndex, boolean savePreview) {
        int nextSample = (sampleIndex + 1) % SampleMessages.ALL.size();

        String note = "";
        if (error != null) {
            note = "<p class=\"note note-error\" role=\"alert\">" + Html.escape(error) + "</p>";
        } else if (sampleIndex >= 0) {
            SampleMessages.Sample sample = SampleMessages.ALL.get(sampleIndex);
            note = "<p class=\"note\">Example " + (sampleIndex + 1) + " of " + SampleMessages.ALL.size()
                    + ": " + Html.escape(sample.title()) + "</p>";
        }

        String content = """
                <section class="hero">
                  <div class="hero-text">
                    <h1><span>Is this message</span><span class="tint">a scam?</span></h1>
                    <p class="lead">Paste a text, WhatsApp message or email. ScamLens checks it against six warning signs scammers rely on and explains every one it finds.</p>
                  </div>

                  <form class="panel scan-panel" method="post" action="/analyze" id="scan-form">
                    <div class="window-bar">
                      <span class="lights" aria-hidden="true"><i></i><i></i><i></i></span>
                      <span class="mono">ScamLens · Scanner</span>
                      <span class="chip mono" id="char-count">%,d / %,d</span>
                    </div>
                    <label for="message" class="visually-hidden">Message to check</label>
                    <textarea id="message" name="message" placeholder="Paste the message here…" data-max="%d" autofocus>%s</textarea>
                    %s
                    <div class="form-actions">
                      <a class="btn btn-ghost" href="/?sample=%d">Try an example</a>
                      <a class="btn btn-ghost" href="/">Clear</a>
                      <button class="btn btn-primary" type="submit">Check message</button>
                    </div>
                    <div class="form-foot">
                      <label class="checkbox">
                        <input type="checkbox" name="savePreview"%s>
                        Keep a masked preview in history
                      </label>
                      <span class="mono hint">Ctrl + Enter</span>
                    </div>
                  </form>

                  <div class="watermark" aria-hidden="true">SCAMLENS</div>
                </section>

                %s

                %s
                """.formatted(
                message.length(), InputValidator.MAX_LENGTH, InputValidator.MAX_LENGTH,
                Html.escape(message),
                note,
                nextSample,
                savePreview ? " checked" : "",
                statsStrip(),
                signsSection());

        return layout("Scanner", "Scanner", content);
    }

    /** Four facts about how ScamLens works, under the hero. */
    private static String statsStrip() {
        return """
                <section class="strip" aria-label="How ScamLens works">
                  <div class="strip-cell">%s<strong>6</strong><span class="mono">Warning signs checked</span><span class="chip mono">Rule-based</span></div>
                  <div class="strip-cell">%s<strong>8</strong><span class="mono">Checks on every link</span><span class="chip mono">Shorteners, IPs, fake sites</span></div>
                  <div class="strip-cell">%s<strong>0</strong><span class="mono">Messages stored in full</span><span class="chip mono">Masked previews</span></div>
                  <div class="strip-cell">%s<strong>100</strong><span class="mono">Highest risk score</span><span class="chip mono">Capped</span></div>
                </section>
                """.formatted(
                icon("M4 6h16 M4 12h16 M4 18h10"),
                icon("M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1 M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"),
                icon("M6 11h12v9H6z M8.5 11V8a3.5 3.5 0 0 1 7 0v3"),
                icon("M4 16a8 8 0 0 1 16 0 M12 16l4-5"));
    }

    /** The six warning signs as cards, each with its points and an example. */
    private static String signsSection() {
        String cards = Arrays.stream(Category.values())
                .map(c -> """
                        <article class="panel sign-card">
                          %s
                          <h3>%s</h3>
                          <p>%s</p>
                          <dl class="specs">
                            <div><dt>Points</dt><dd>+%d</dd></div>
                            <div><dt>Example</dt><dd>%s</dd></div>
                          </dl>
                        </article>
                        """.formatted(icon(iconPath(c)), Html.escape(c.label()), Html.escape(c.explanation()),
                        c.points(), Html.escape(example(c))))
                .collect(Collectors.joining());
        return """
                <section class="section">
                  <p class="section-tag mono">Detection rules</p>
                  <h2 class="section-title"><span>Six signs scammers</span> <span class="tint">rely on</span></h2>
                  <p class="section-lead">Each sign adds its points once, however often it appears. Asking for an OTP together with a link or a payment always counts as high risk.</p>
                  <div class="sign-grid">%s</div>
                </section>
                """.formatted(cards);
    }

    private static String example(Category category) {
        return switch (category) {
            case URGENCY -> "\"act now\"";
            case THREAT -> "\"account will be blocked\"";
            case REWARD -> "\"you have won\"";
            case CREDENTIAL -> "\"share your OTP\"";
            case FINANCIAL -> "\"pay a processing fee\"";
            case URL -> "bit.ly, http://, fake brand sites";
        };
    }

    private static String iconPath(Category category) {
        return switch (category) {
            case URGENCY -> "M12 4a8 8 0 1 0 0 16a8 8 0 1 0 0-16 M12 8v4l3 2";
            case THREAT -> "M12 4L21 19H3z M12 10v4 M12 16.5v.5";
            case REWARD -> "M4 10h16v10H4z M4 14h16 M12 10v10 M12 10c-1.5-4-6-4-5-1 M12 10c1.5-4 6-4 5-1";
            case CREDENTIAL -> "M8 11a4 4 0 1 0 0 8a4 4 0 1 0 0-8 M11 12l8-8 M16 7l2 2";
            case FINANCIAL -> "M12 4a8 8 0 1 0 0 16a8 8 0 1 0 0-16 M9 8.5h6 M9 11.5h6 M10 8.5c3.5 0 3.5 3 0 3l4 4.5";
            case URL -> "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1 M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1";
        };
    }

    /** A small blue tile with a line icon. */
    private static String icon(String path) {
        return "<span class=\"icon\" aria-hidden=\"true\"><svg viewBox=\"0 0 24 24\" width=\"18\" height=\"18\">"
                + "<path d=\"" + path + "\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" "
                + "stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg></span>";
    }

    // =====================================================================
    // 2. RESULTS
    // =====================================================================

    public static String results(AnalysisResult result, String savedStatus) {
        String level = result.riskLevel().cssClass();

        // The message, with each suspicious phrase wrapped in <mark>
        String bubble = TextHighlighter.split(result.message(), result.allEvidence()).stream()
                .map(segment -> segment.highlighted()
                        ? "<mark>" + Html.escape(segment.text()) + "</mark>"
                        : Html.escape(segment.text()))
                .collect(Collectors.joining());

        // Safety tips, without repeating the summary sentence already shown in the verdict
        List<String> advice = result.recommendations().stream()
                .filter(tip -> !tip.equals(result.riskLevel().summary()))
                .toList();
        if (advice.isEmpty()) {
            advice = List.of("If a message later asks for money, a code or a password, "
                    + "check with the sender using a number you already trust.");
        }
        String tips = advice.stream()
                .map(tip -> "<li>" + Html.escape(tip) + "</li>")
                .collect(Collectors.joining());

        String reasons = result.hasIndicators()
                ? "<div class=\"reason-grid\">"
                        + result.detections().stream().map(Pages::reasonCard).collect(Collectors.joining())
                        + "</div>"
                : "<div class=\"panel empty-panel\"><p>None of the six warning signs appear in this message.</p></div>";

        String content = """
                <section class="page-head">
                  <p class="section-tag mono">Analysis complete · %s</p>
                </section>

                <section class="panel verdict %s">
                  <div class="verdict-text">
                    <h1>%s</h1>
                    <p class="verdict-summary">%s</p>
                    <div class="scale" role="img" aria-label="Score %d on a scale from 0 to 100">
                      <div class="scale-track">
                        <span class="zone zone-low"></span><span class="zone zone-suspicious"></span><span class="zone zone-high"></span>
                        <span class="scale-marker" style="left: %d%%"></span>
                      </div>
                      <div class="scale-labels mono"><span>Low 0–30</span><span>Suspicious 31–60</span><span>High 61–100</span></div>
                    </div>
                  </div>
                  <div class="verdict-score">
                    <span class="score-number">%d</span>
                    <span class="mono muted">Risk score / 100</span>
                  </div>
                </section>
                <p class="fine">The score is a warning level worked out from fixed rules, not proof that a message is a scam. %s</p>

                <div class="two-col">
                  <section class="panel">
                    <h2 class="panel-title">The message</h2>
                    <div class="bubble %s">%s</div>
                  </section>
                  <section class="panel">
                    <h2 class="panel-title">What to do</h2>
                    <ul class="tips">%s</ul>
                  </section>
                </div>

                <section class="section-tight">
                  <h2 class="block-title">%s</h2>
                  %s
                </section>

                <p class="center"><a class="btn btn-primary" href="/">Check another message</a></p>
                """.formatted(
                Html.date(result.analysedAt()),
                level,
                Html.escape(result.riskLevel().label()),
                Html.escape(result.riskLevel().summary()),
                result.score(), result.score(),
                result.score(),
                Html.escape(savedStatus),
                level, bubble,
                tips,
                result.hasIndicators() ? "Why it was flagged" : "Nothing suspicious found",
                reasons);

        return layout("Result", "Scanner", "<div class=\"container\">" + content + "</div>");
    }

    private static String reasonCard(DetectionResult detection) {
        String found = detection.evidence().stream()
                .map(text -> "\"" + Html.escape(text) + "\"")
                .collect(Collectors.joining(", "));
        return """
                <article class="panel reason">
                  <div class="reason-head">
                    %s
                    <h3>%s</h3>
                  </div>
                  <p class="reason-text">%s</p>
                  <dl class="specs">
                    <div><dt>Points</dt><dd>+%d</dd></div>
                    <div><dt>Found</dt><dd>%s</dd></div>
                  </dl>
                </article>
                """.formatted(
                icon(iconPath(detection.category())),
                Html.escape(detection.category().label()),
                Html.escape(detection.explanation()),
                detection.category().points(),
                found);
    }

    // =====================================================================
    // 3. DASHBOARD
    // =====================================================================

    public static String dashboard(DashboardStats stats, List<ScanRecord> recent) {
        String body;
        if (stats.isEmpty()) {
            body = """
                    <div class="panel empty-panel">
                      <p>No messages checked yet. Numbers appear here after your first check.</p>
                      <a class="btn btn-primary" href="/">Check a message</a>
                    </div>
                    """;
        } else {
            String rows = recent.stream().map(Pages::recentRow).collect(Collectors.joining());
            body = """
                    <section class="strip strip-flush" aria-label="Totals">
                      <div class="strip-cell"><strong>%d</strong><span class="mono">Checks</span></div>
                      <div class="strip-cell risk-high"><strong>%d</strong><span class="mono">High risk</span></div>
                      <div class="strip-cell risk-suspicious"><strong>%d</strong><span class="mono">Suspicious</span></div>
                      <div class="strip-cell risk-low"><strong>%d</strong><span class="mono">Low risk</span></div>
                    </section>

                    <div class="two-col two-col-wide">
                      <section class="panel">
                        <h2 class="panel-title">Risk mix</h2>
                        %s
                      </section>
                      <section class="panel">
                        <div class="panel-head">
                          <h2 class="panel-title">Recent checks</h2>
                          <a class="mono small-link" href="/history">See all</a>
                        </div>
                        <ul class="recent">%s</ul>
                      </section>
                    </div>
                    """.formatted(stats.total(), stats.high(), stats.suspicious(), stats.low(),
                    mixBar(stats), rows);
        }

        String subtitle = stats.isEmpty() ? "Your checks, counted from the saved history."
                : "Based on " + stats.total() + " saved " + (stats.total() == 1 ? "check" : "checks") + ".";

        String content = """
                <section class="page-head">
                  <p class="section-tag mono">Dashboard</p>
                  <h1 class="page-title"><span>Your scan</span> <span class="tint">activity</span></h1>
                  <p class="lead">%s</p>
                </section>
                %s
                """.formatted(Html.escape(subtitle), body);

        return layout("Dashboard", "Dashboard", "<div class=\"container\">" + content + "</div>");
    }

    /** One horizontal bar split into High / Suspicious / Low, each as wide as its share. */
    static String mixBar(DashboardStats stats) {
        int[] counts = {stats.high(), stats.suspicious(), stats.low()};
        String[] classes = {"risk-high", "risk-suspicious", "risk-low"};
        String[] names = {"High risk", "Suspicious", "Low risk"};

        StringBuilder segments = new StringBuilder();
        StringBuilder legend = new StringBuilder();
        for (int i = 0; i < counts.length; i++) {
            double share = 100.0 * counts[i] / stats.total();
            if (counts[i] > 0) {
                segments.append("<span class=\"mix-part ").append(classes[i]).append("\" style=\"width: ")
                        .append(String.format(Locale.ROOT, "%.2f", share)).append("%\"></span>");
            }
            legend.append("<li class=\"").append(classes[i]).append("\"><span class=\"swatch\"></span>")
                    .append("<span>").append(names[i]).append("</span><span class=\"mono\">")
                    .append(counts[i]).append(" · ").append(Math.round(share)).append("%</span></li>");
        }
        return "<div class=\"mix\" role=\"img\" aria-label=\"" + stats.high() + " high risk, "
                + stats.suspicious() + " suspicious, " + stats.low() + " low risk\">" + segments + "</div>"
                + "<ul class=\"mix-legend\">" + legend + "</ul>";
    }

    private static String recentRow(ScanRecord record) {
        return """
                <li>
                  <div class="recent-main">
                    %s
                    <span class="recent-signs">%s</span>
                  </div>
                  <div class="recent-side">
                    <span class="mono recent-score">%d</span>
                    <span class="mono muted">%s</span>
                  </div>
                </li>
                """.formatted(Html.badge(record.riskLevel()), signsFound(record),
                record.score(), Html.date(record.scannedAt()));
    }

    /** "Urgency, Suspicious link", or a muted "No warning signs" when nothing was found. */
    private static String signsFound(ScanRecord record) {
        return record.categories().isEmpty()
                ? "<span class=\"muted\">No warning signs</span>"
                : Html.escape(record.categoryLabels());
    }

    // =====================================================================
    // 4. HISTORY
    // =====================================================================

    public static String history(List<ScanRecord> records) {
        String body;
        if (records.isEmpty()) {
            body = """
                    <div class="panel empty-panel">
                      <p>No checks saved yet. Every message you check is listed here, without its full text.</p>
                      <a class="btn btn-primary" href="/">Check a message</a>
                    </div>
                    """;
        } else {
            String rows = records.stream().map(Pages::historyRow).collect(Collectors.joining());
            body = """
                    <section class="panel table-panel">
                      <div class="panel-head">
                        <span class="mono muted">%d saved %s</span>
                        <form method="post" action="/history/clear" data-confirm="Delete all %d saved checks? This cannot be undone.">
                          <button class="btn btn-ghost btn-danger btn-small" type="submit">Delete all</button>
                        </form>
                      </div>
                      <div class="table-wrap">
                        <table>
                          <thead>
                            <tr><th scope="col">Checked</th><th scope="col">Result</th><th scope="col" class="num">Score</th><th scope="col">Signs found</th><th scope="col">Preview</th><th scope="col"><span class="visually-hidden">Actions</span></th></tr>
                          </thead>
                          <tbody>%s</tbody>
                        </table>
                      </div>
                    </section>
                    """.formatted(records.size(), records.size() == 1 ? "check" : "checks", records.size(), rows);
        }

        String content = """
                <section class="page-head">
                  <p class="section-tag mono">History</p>
                  <h1 class="page-title"><span>Every check,</span> <span class="tint">nothing more</span></h1>
                  <p class="lead">Only the result of each check is saved, never the full message.</p>
                </section>
                %s
                """.formatted(body);

        return layout("History", "History", "<div class=\"container\">" + content + "</div>");
    }

    private static String historyRow(ScanRecord record) {
        String preview = record.preview() == null
                ? "<span class=\"muted\">Not kept</span>"
                : Html.escape(record.preview());
        return """
                <tr>
                  <td class="nowrap mono">%s</td>
                  <td class="nowrap">%s</td>
                  <td class="num mono">%d</td>
                  <td>%s</td>
                  <td class="preview">%s</td>
                  <td>
                    <form method="post" action="/history/delete" data-confirm="Delete the check from %s?">
                      <input type="hidden" name="id" value="%d">
                      <button class="btn btn-ghost btn-danger btn-small" type="submit">Delete</button>
                    </form>
                  </td>
                </tr>
                """.formatted(Html.date(record.scannedAt()), Html.badge(record.riskLevel()), record.score(),
                signsFound(record), preview, Html.date(record.scannedAt()), record.id());
    }

    // =====================================================================
    // ERROR PAGE
    // =====================================================================

    public static String error(String title, String message) {
        String content = """
                <div class="container">
                  <section class="page-head">
                    <h1 class="page-title">%s</h1>
                    <p class="lead">%s</p>
                    <p><a class="btn btn-primary" href="/">Back to the scanner</a></p>
                  </section>
                </div>
                """.formatted(Html.escape(title), Html.escape(message));
        return layout(title, "", content);
    }
}
