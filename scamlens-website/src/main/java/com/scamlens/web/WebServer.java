package com.scamlens.web;

import com.scamlens.analyzer.MessageAnalyzer;
import com.scamlens.model.AnalysisResult;
import com.scamlens.service.HistoryService;
import com.scamlens.util.SampleMessages;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The web server: it listens for requests from the browser and decides what to do.
 * This is the CONTROLLER of the website.
 *
 * EVENT-DRIVEN: we register a handler for each address (route). The server waits,
 * and when a request arrives (an event), Java calls the matching handler.
 *
 *   GET  /                 -> Scanner page (also loads samples: /?sample=2)
 *   POST /analyze          -> runs MessageAnalyzer, saves to history, shows the Results page
 *   GET  /dashboard        -> Dashboard page
 *   GET  /history          -> History page
 *   POST /history/delete   -> deletes one scan
 *   POST /history/clear    -> deletes all scans
 *   GET  /static/...       -> styles.css and app.js from the "public" folder
 *
 * It contains NO detection logic and NO file-handling logic: it delegates to
 * MessageAnalyzer and HistoryService, and Pages builds the HTML.
 */
public class WebServer {

    /** Largest request body accepted (a 5,000-character message fits easily). */
    private static final int MAX_BODY_BYTES = 100_000;

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "svg", "image/svg+xml",
            "ico", "image/x-icon");

    private final MessageAnalyzer analyzer;
    private final HistoryService history;
    private final Path publicFolder;
    private HttpServer server;

    public WebServer(MessageAnalyzer analyzer, HistoryService history, Path publicFolder) {
        this.analyzer = analyzer;
        this.history = history;
        this.publicFolder = publicFolder.toAbsolutePath().normalize();
    }

    /** Starts listening on the given port. */
    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        // Register one handler per route (method references = FUNCTIONAL style)
        server.createContext("/", safe(this::handleScanner));
        server.createContext("/analyze", safe(this::handleAnalyze));
        server.createContext("/dashboard", safe(this::handleDashboard));
        server.createContext("/history", safe(this::handleHistory));
        server.createContext("/history/delete", safe(this::handleDelete));
        server.createContext("/history/clear", safe(this::handleClear));
        server.createContext("/static/", safe(this::handleStatic));

        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public int port() {
        return server.getAddress().getPort();
    }

    // =====================================================================
    // HANDLERS (one per page or action)
    // =====================================================================

    /** GET / : the Scanner page. /?sample=N loads demo sample N into the box. */
    private void handleScanner(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().equals("/")) {
            sendHtml(exchange, 404, Pages.error("Page not found", "That page does not exist."));
            return;
        }
        if (!requireMethod(exchange, "GET")) return;

        FormData query = FormData.parse(exchange.getRequestURI().getRawQuery());
        int sampleIndex = query.get("sample").map(WebServer::toSampleIndex).orElse(-1);
        String message = sampleIndex >= 0 ? SampleMessages.ALL.get(sampleIndex).text() : "";

        sendHtml(exchange, 200, Pages.scanner(message, null, sampleIndex, false));
    }

    /** POST /analyze : the main action. Form -> MessageAnalyzer -> Results page. */
    private void handleAnalyze(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) return;

        String body = readBody(exchange);
        if (body == null) {
            sendHtml(exchange, 413, Pages.scanner("", "The message is too long to send.", -1, false));
            return;
        }
        FormData form = FormData.parse(body);
        String message = form.get("message").orElse("");
        boolean savePreview = form.isChecked("savePreview");

        AnalysisResult result;
        try {
            result = analyzer.analyze(message);                 // all detection happens in the engine
        } catch (IllegalArgumentException invalidInput) {
            // Show the scanner again with the error and the user's text kept in the box
            sendHtml(exchange, 400, Pages.scanner(message, invalidInput.getMessage(), -1, savePreview));
            return;
        }

        String savedStatus;
        try {
            history.save(result, savePreview, visitorId(exchange));
            savedStatus = savePreview
                    ? "Saved to history with a short masked preview."
                    : "Saved to history (no message text stored).";
        } catch (IOException e) {
            savedStatus = "Not saved to history: " + e.getMessage();   // still show the result
        }

        sendHtml(exchange, 200, Pages.results(result, savedStatus));
    }

    /** GET /dashboard */
    private void handleDashboard(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET")) return;
        String visitor = visitorId(exchange);
        sendHtml(exchange, 200, Pages.dashboard(history.getStats(visitor), history.findRecent(visitor, 5)));
    }

    /** GET /history */
    private void handleHistory(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().equals("/history")) {
            sendHtml(exchange, 404, Pages.error("Page not found", "That page does not exist."));
            return;
        }
        if (!requireMethod(exchange, "GET")) return;
        sendHtml(exchange, 200, Pages.history(history.findAll(visitorId(exchange))));
    }

    /** POST /history/delete with id=5 */
    private void handleDelete(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) return;
        String body = readBody(exchange);
        long id;
        try {
            id = Long.parseLong(FormData.parse(body).get("id").orElse(""));
        } catch (NumberFormatException e) {
            sendHtml(exchange, 400, Pages.error("Could not delete", "The scan id was missing or invalid."));
            return;
        }
        history.delete(id, visitorId(exchange));   // only deletes it if it is this visitor's
        redirect(exchange, "/history");   // show the updated list
    }

    /** POST /history/clear */
    private void handleClear(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) return;
        readBody(exchange);
        history.deleteAll(visitorId(exchange));   // only this visitor's scans
        redirect(exchange, "/history");
    }

    /** GET /static/styles.css etc. Only files inside the public folder can be served. */
    private void handleStatic(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET")) return;
        String name = exchange.getRequestURI().getPath().substring("/static/".length());
        Path file = publicFolder.resolve(name).normalize();

        // SECURITY: block tricks like /static/../src/... that try to leave the public folder
        if (!file.startsWith(publicFolder) || !Files.isRegularFile(file)) {
            sendText(exchange, 404, "text/plain; charset=utf-8", "Not found");
            return;
        }
        String extension = name.substring(name.lastIndexOf('.') + 1);
        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type",
                CONTENT_TYPES.getOrDefault(extension, "application/octet-stream"));
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    /** A handler that may throw IOException (so we can use method references above). */
    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }

    /**
     * Wraps a handler so any unexpected error shows a friendly error page
     * instead of breaking the connection.
     */
    private HttpHandler safe(Handler handler) {
        return exchange -> {
            try {
                handler.handle(exchange);
            } catch (Exception e) {
                System.err.println("Error while handling " + exchange.getRequestURI() + ": " + e);
                try {
                    sendHtml(exchange, 500, Pages.error("Something went wrong",
                            "The server could not complete the request. Please try again."));
                } catch (IOException ignored) {
                    // the browser has gone away; nothing more we can do
                }
            } finally {
                exchange.close();
            }
        };
    }

    // ---------- visitors (who owns which history) ----------

    /** Name of the cookie that remembers which visitor a browser is. */
    static final String VISITOR_COOKIE = "scamlens_visitor";

    /** A visitor id is a random UUID: 36 characters of hex digits and dashes. */
    private static final Pattern VISITOR_ID = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    /**
     * Returns this browser's visitor id, read from its cookie.
     * A browser without a (valid) cookie gets a new random id, sent back in a Set-Cookie header,
     * so its next requests carry the same id. There is no login: the cookie IS the identity.
     *
     * Call it ONCE per request and keep the result in a local variable.
     * (Not exchange.setAttribute: in Java's HttpServer those attributes are shared by every
     * request to the same path, so one visitor's id would leak to the next. Test WEB04 caught this.)
     */
    private static String visitorId(HttpExchange exchange) {
        return readCookie(exchange, VISITOR_COOKIE)
                .filter(value -> VISITOR_ID.matcher(value).matches())   // ignore made-up values
                .orElseGet(() -> {
                    String fresh = UUID.randomUUID().toString();
                    boolean https = "https".equalsIgnoreCase(exchange.getRequestHeaders().getFirst("X-Forwarded-Proto"));
                    exchange.getResponseHeaders().add("Set-Cookie", VISITOR_COOKIE + "=" + fresh
                            + "; Path=/; Max-Age=31536000; HttpOnly; SameSite=Lax" + (https ? "; Secure" : ""));
                    return fresh;
                });
    }

    /** Finds one cookie in the request's Cookie header, e.g. "a=1; scamlens_visitor=3f2a…". */
    static Optional<String> readCookie(HttpExchange exchange, String name) {
        return exchange.getRequestHeaders().getOrDefault("Cookie", List.of()).stream()
                .flatMap(header -> Arrays.stream(header.split(";")))
                .map(String::trim)
                .filter(pair -> pair.startsWith(name + "="))
                .map(pair -> pair.substring(name.length() + 1))
                .map(value -> value.replace("\"", ""))   // some clients send v="abc" instead of v=abc
                .findFirst();
    }

    private boolean requireMethod(HttpExchange exchange, String method) throws IOException {
        if (exchange.getRequestMethod().equalsIgnoreCase(method)) {
            return true;
        }
        exchange.getResponseHeaders().set("Allow", method);
        sendText(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
        return false;
    }

    /** Reads the request body. Returns null if it is larger than MAX_BODY_BYTES. */
    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            byte[] bytes = in.readNBytes(MAX_BODY_BYTES + 1);
            if (bytes.length > MAX_BODY_BYTES) {
                return null;
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static void sendHtml(HttpExchange exchange, int status, String html) throws IOException {
        sendText(exchange, status, "text/html; charset=utf-8", html);
    }

    private static void sendText(HttpExchange exchange, int status, String type, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** Sends the browser to another page (used after delete, so refresh doesn't repeat it). */
    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
    }

    /** "2" -> 2, anything invalid -> 0 (the first sample). */
    private static int toSampleIndex(String text) {
        try {
            int index = Integer.parseInt(text);
            return (index >= 0 && index < SampleMessages.ALL.size()) ? index : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
