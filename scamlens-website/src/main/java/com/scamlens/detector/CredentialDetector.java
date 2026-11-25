package com.scamlens.detector;

import com.scamlens.model.Category;

import java.util.List;

/**
 * Finds REQUESTS for secret details.
 *
 * Context rule: just mentioning "OTP" is not enough. There must be an
 * action word (share, send, enter, verify...) asking for the secret.
 *
 *   "Your OTP is 482913"            -> not flagged (just information)
 *   "Do not share your OTP"         -> not flagged (negated, safe advice)
 *   "Share your OTP to verify"      -> flagged
 */
public class CredentialDetector extends KeywordDetector {

    private static final String ACTION =
            "(?:share|send|enter|provide|verify|confirm|tell|give|update|submit|type|reply\\s+with)";

    private static final String SECRET =
            "(?:otp|one[- ]time\\s+password|password|passcode|upi\\s+pin|atm\\s+pin|m?pin|cvv"
            + "|card\\s+(?:number|details)|bank(?:ing)?\\s+details|account\\s+(?:number|details)"
            + "|login\\s+details|credentials|net\\s?banking\\s+details)";

    public CredentialDetector() {
        super(Category.CREDENTIAL, List.of(
                // action + optional "us/me" + optional "your/the" + optional one word + secret
                // e.g. "share your OTP", "enter the 6-digit OTP", "send us your banking details"
                ACTION + "\\s+(?:(?:us|me)\\s+)?(?:(?:your|the|ur|this)\\s+)?(?:[\\w-]+\\s+)?" + SECRET,
                // e.g. "share it with us"
                "(?:share|send|forward|tell)\\s+(?:it|this|the\\s+code)\\s+(?:with|to)\\s+(?:us|me|our\\s+\\w+)"
        ));
    }
}
