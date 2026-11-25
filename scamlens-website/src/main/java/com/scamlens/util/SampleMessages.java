package com.scamlens.util;

import java.util.List;

/**
 * Demo messages for the "Load sample" button.
 *
 * These are only example INPUTS, clearly labelled as samples in the GUI.
 * The results shown for them are calculated by the real detection engine,
 * never hard-coded.
 */
public final class SampleMessages {

    public record Sample(String title, String text) { }

    public static final List<Sample> ALL = List.of(
            new Sample("Fake bank KYC alert",
                    "URGENT: Your SBI account will be blocked within 24 hours due to pending KYC. "
                    + "Update now at http://sbi-kyc-update.xyz and share your OTP to verify."),
            new Sample("Lottery prize scam",
                    "Congratulations! You have won Rs 25,00,000 in the KBC lucky draw. "
                    + "Pay a small processing fee of Rs 4,999 to claim your prize. Offer valid today only."),
            new Sample("Parcel delivery fee",
                    "India Post: Your parcel is on hold due to an incomplete address. "
                    + "Pay the redelivery fee of Rs 25 immediately at bit.ly/ip-redeliver"),
            new Sample("Genuine bank OTP (safe)",
                    "123456 is your OTP for a transaction of Rs 2,500 at Amazon. Valid for 10 minutes. "
                    + "Do not share this OTP with anyone. - HDFC Bank"),
            new Sample("Message from a friend (safe)",
                    "Hey! Are we still meeting at the library at 5 pm today? "
                    + "I'll bring the notes for the Java project.")
    );

    private SampleMessages() { }
}
