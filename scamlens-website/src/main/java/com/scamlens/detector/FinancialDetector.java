package com.scamlens.detector;

import com.scamlens.model.Category;

import java.util.List;

/**
 * Finds requests to send money or pay a fee.
 *
 * Context rule: "Payment of Rs 500 received" is NOT flagged.
 * Only requests like "transfer money" or "pay a processing fee" are.
 */
public class FinancialDetector extends KeywordDetector {

    public FinancialDetector() {
        super(Category.FINANCIAL, List.of(
                "transfer\\s+(?:the\\s+)?(?:money|amount|funds|payment)",
                "send\\s+(?:the\\s+)?(?:money|payment|amount|funds)",
                "pay\\s+(?:a\\s+|the\\s+|an\\s+)?(?:small\\s+|nominal\\s+)?"
                        + "(?:processing|registration|delivery|redelivery|customs|clearance|verification"
                        + "|activation|service|release|security|handling)\\s+(?:fee|charges?|amount|deposit)",
                "(?:processing|registration|customs|clearance|activation|release|redelivery)\\s+(?:fee|charges?)",
                "refund\\s+(?:processing\\s+)?fee",
                "advance\\s+(?:payment|fee)",
                "(?:accept|approve)\\s+(?:the\\s+)?(?:upi\\s+)?collect\\s+request",
                "scan\\s+(?:the|this)\\s+qr(?:\\s+code)?\\s+to\\s+(?:receive|get|claim)",
                "pay\\s+now"
        ));
    }
}
