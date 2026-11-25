package com.scamlens.detector;

import com.scamlens.model.Category;
import com.scamlens.model.DetectionResult;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Finds links in a message and checks them for warning signs.
 *
 * SAFETY: links are only READ as text and parsed with java.net.URI.
 * They are never opened, visited or downloaded.
 *
 * A normal link such as https://www.amazon.in/orders is NOT flagged.
 * A link is flagged only if at least one rule below matches it.
 */
public class URLAnalyzer implements Detector {

    /** Finds http(s) links, www. links, IP addresses and bare domains like bit.ly/abc. */
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s<>\"']+"
            + "|www\\.[^\\s<>\"']+"
            + "|\\b\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?(?:/[^\\s<>\"']*)?"
            + "|\\b[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.(?:com|in|net|org|co|info|biz|xyz|top|click|link|live|online"
            + "|site|club|tk|ml|ga|cf|gq|ly|me|io|cc|gl|gd|at|id)\\b(?:/[^\\s<>\"']*)?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[.,!?;:)\\]}'\"]+$");
    private static final Pattern IP_ADDRESS = Pattern.compile("\\d{1,3}(?:\\.\\d{1,3}){3}");

    private static final Set<String> SHORTENERS = Set.of(
            "bit.ly", "tinyurl.com", "t.co", "goo.gl", "is.gd", "cutt.ly", "rb.gy",
            "ow.ly", "shorturl.at", "tiny.cc", "rebrand.ly", "s.id", "t.ly");

    private static final List<String> RISKY_ENDINGS = List.of(
            ".xyz", ".top", ".click", ".tk", ".ml", ".ga", ".cf", ".gq", ".link", ".live", ".club", ".online", ".site");

    /** Brand name -> the brand's real domains. */
    private static final Map<String, List<String>> BRANDS = Map.ofEntries(
            Map.entry("sbi", List.of("sbi.co.in", "onlinesbi.sbi", "sbi.bank.in", "sbicard.com")),
            Map.entry("hdfc", List.of("hdfcbank.com", "hdfc.com", "hdfcbank.bank.in")),
            Map.entry("icici", List.of("icicibank.com", "icici.bank.in")),
            Map.entry("axisbank", List.of("axisbank.com")),
            Map.entry("paytm", List.of("paytm.com", "paytm.in")),
            Map.entry("phonepe", List.of("phonepe.com")),
            Map.entry("amazon", List.of("amazon.in", "amazon.com")),
            Map.entry("flipkart", List.of("flipkart.com")),
            Map.entry("paypal", List.of("paypal.com")),
            Map.entry("netflix", List.of("netflix.com")),
            Map.entry("airtel", List.of("airtel.in", "airtel.com")),
            Map.entry("indiapost", List.of("indiapost.gov.in")),
            Map.entry("incometax", List.of("incometax.gov.in")));

    /** A link after parsing. */
    record Link(String text, String host, boolean insecure, boolean hasUserInfo) { }

    /** One check: a reason to show the user + a test (a Predicate) that decides if it applies. */
    record UrlRule(String reason, Predicate<Link> test) { }

    /** FUNCTIONAL style: each rule is data — a reason plus a Predicate (lambda). */
    private static final List<UrlRule> RULES = List.of(
            new UrlRule("uses a link shortener that hides the real website",
                    link -> SHORTENERS.contains(withoutWww(link.host()))),
            new UrlRule("points to a raw IP address instead of a website name",
                    link -> IP_ADDRESS.matcher(link.host()).matches()),
            new UrlRule("uses insecure http:// instead of https://",
                    Link::insecure),
            new UrlRule("uses a domain ending that is often used by scam sites",
                    link -> RISKY_ENDINGS.stream().anyMatch(link.host()::endsWith)),
            new UrlRule("uses a brand name but is not that brand's official website",
                    URLAnalyzer::impersonatesBrand),
            new UrlRule("swaps letters for numbers to look like a real brand",
                    URLAnalyzer::isLookalike),
            new UrlRule("has an unusually complicated address (many hyphens or sub-domains)",
                    link -> count(link.host(), '-') >= 3 || count(link.host(), '.') >= 4),
            new UrlRule("hides the real website behind an @ sign",
                    Link::hasUserInfo)
    );

    @Override
    public Category category() {
        return Category.URL;
    }

    @Override
    public Optional<DetectionResult> detect(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }

        List<String> links = findLinks(message);
        List<String> flaggedLinks = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        for (String link : links) {
            List<String> problems = findProblems(link);
            if (!problems.isEmpty()) {
                flaggedLinks.add(link);
                reasons.add("• " + link + " " + String.join("; ", problems) + ".");
            }
        }

        if (flaggedLinks.isEmpty()) {
            return Optional.empty();
        }
        String explanation = Category.URL.explanation() + "\n" + String.join("\n", reasons);
        return Optional.of(new DetectionResult(Category.URL, flaggedLinks, explanation));
    }

    /** All links in the message, with trailing punctuation removed. */
    List<String> findLinks(String message) {
        return URL_PATTERN.matcher(message).results()
                .map(MatchResult::group)
                .map(link -> TRAILING_PUNCTUATION.matcher(link).replaceAll(""))
                .filter(link -> !link.isEmpty())
                .distinct()
                .toList();
    }

    /** Runs every rule against one link and returns the reasons that apply. */
    List<String> findProblems(String linkText) {
        Optional<Link> parsed = parse(linkText);
        if (parsed.isEmpty()) {
            return List.of("is malformed and cannot be checked safely");
        }
        Link link = parsed.get();
        return RULES.stream()
                .filter(rule -> rule.test().test(link))
                .map(UrlRule::reason)
                .toList();
    }

    /** Parses a link with java.net.URI. Returns empty if the link is malformed. */
    static Optional<Link> parse(String text) {
        String lower = text.toLowerCase();
        boolean hasScheme = lower.startsWith("http://") || lower.startsWith("https://");
        String withScheme = hasScheme ? text : "https://" + text;
        try {
            URI uri = new URI(withScheme);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new Link(text, host.toLowerCase(),
                    lower.startsWith("http://"), uri.getRawUserInfo() != null));
        } catch (URISyntaxException e) {
            return Optional.empty();   // malformed link: handled, never crashes
        }
    }

    private static boolean impersonatesBrand(Link link) {
        return BRANDS.entrySet().stream()
                .anyMatch(brand -> link.host().contains(brand.getKey())
                        && !isOfficial(link.host(), brand.getValue()));
    }

    private static boolean isOfficial(String host, List<String> officialDomains) {
        return officialDomains.stream()
                .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
    }

    /** "amaz0n.in" becomes "amazon.in" after swapping numbers back to letters -> lookalike. */
    private static boolean isLookalike(Link link) {
        String host = link.host();
        String normalised = host.replace('0', 'o').replace('1', 'l').replace('3', 'e')
                .replace('4', 'a').replace('5', 's').replace('7', 't');
        return !normalised.equals(host)
                && BRANDS.keySet().stream()
                        .anyMatch(brand -> normalised.contains(brand) && !host.contains(brand));
    }

    private static String withoutWww(String host) {
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    private static long count(String text, char c) {
        return text.chars().filter(ch -> ch == c).count();
    }
}
