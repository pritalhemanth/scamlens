package com.scamlens.detector;

import com.scamlens.model.Category;

import java.util.List;

/** Finds threats: blocked accounts, legal action, disconnected services... */
public class ThreatDetector extends KeywordDetector {

    public ThreatDetector() {
        super(Category.THREAT, List.of(
                // "your account will be blocked", "card has been suspended"
                "(?:account|card|sim|number|wallet|upi)\\s+(?:will\\s+be|has\\s+been|is|is\\s+being|may\\s+be|would\\s+be|gets?)\\s+"
                        + "(?:permanently\\s+|temporarily\\s+)?"
                        + "(?:blocked|suspended|deactivated|closed|frozen|terminated|disabled|locked|restricted)",
                // "account suspended"
                "(?:account|card)\\s+(?:blocked|suspended|deactivated|frozen|locked)",
                // "to avoid account suspension" (missed in first test round, added as a fix)
                "(?:account|card|sim|service)\\s+(?:suspension|deactivation|blocking|closure)",
                "legal action",
                // "electricity will be disconnected", "service terminated"
                "(?:service|connection|electricity|power|gas)(?:\\s+\\w+){0,2}\\s+"
                        + "(?:terminated|disconnected|discontinued|cut\\s+off|suspended)",
                "service termination",
                "arrest warrant",
                "digital arrest",
                "(?:will|may) be arrested",
                "(?:police|cyber\\s?crime)\\s+(?:complaint|case|action)",
                "kyc\\s+(?:is\\s+|has\\s+)?(?:expired|pending|incomplete|not\\s+updated|suspended)"
        ));
    }
}
