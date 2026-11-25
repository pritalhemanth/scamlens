package com.scamlens.model;

/**
 * The six kinds of scam warning sign ScamLens looks for.
 *
 * Each category stores everything about itself in one place:
 *   - label        : name shown in the GUI
 *   - points       : how much it adds to the risk score (PRD section 10.2)
 *   - explanation  : why this sign is dangerous
 *   - advice       : the safety recommendation shown to the user
 *
 * Keeping the weights here (encapsulation) means there is only one place
 * to change them.
 */
public enum Category {

    URGENCY("Urgency", 10,
            "The message pressures you to act quickly so that you don't stop and think.",
            "Slow down. Genuine organisations give you time to check a message before you act."),

    THREAT("Threat", 15,
            "The message threatens a penalty, such as a blocked account, legal action or a cut-off service.",
            "Don't panic. Contact the organisation using the phone number on its official website or on your card."),

    REWARD("Fake reward", 10,
            "The message promises a prize, lottery win or free gift.",
            "You cannot win a contest you never entered. Ignore prize claims, especially ones that ask for money or details."),

    CREDENTIAL("Credential request", 25,
            "The message asks you to share or enter an OTP, PIN, password or bank details.",
            "Never share an OTP, PIN, password or card details. Banks and companies never ask for them by message or call."),

    FINANCIAL("Financial request", 15,
            "The message asks you to send money or pay a fee.",
            "Never pay a fee to receive a prize, refund or parcel. Check any payment request in the official app or website."),

    URL("Suspicious link", 25,
            "The message contains a link with warning signs.",
            "Don't open the link. Type the official website address yourself or use the official app.");

    private final String label;
    private final int points;
    private final String explanation;
    private final String advice;

    Category(String label, int points, String explanation, String advice) {
        this.label = label;
        this.points = points;
        this.explanation = explanation;
        this.advice = advice;
    }

    public String label()       { return label; }
    public int points()         { return points; }
    public String explanation() { return explanation; }
    public String advice()      { return advice; }
}
