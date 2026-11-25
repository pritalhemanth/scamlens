package com.scamlens.detector;

import com.scamlens.model.Category;

import java.util.List;

/** Finds fake prizes: "you have won", "claim your prize", "lottery"... */
public class RewardDetector extends KeywordDetector {

    public RewardDetector() {
        super(Category.REWARD, List.of(
                // (?!['’]t) stops "you won't believe this" from matching (bug found in testing)
                "you(?:'ve|’ve|\\s+have)?\\s+(?:just\\s+)?won(?!['’]t)",
                "you\\s+are\\s+(?:a|the|our)\\s+(?:lucky\\s+)?winner",
                "claim\\s+(?:your|the|this)\\s+(?:free\\s+)?(?:prize|reward|gift|cashback|bonus|winnings)",
                "lottery(?:\\s+winner)?",
                "lucky\\s+(?:draw|winner|customer)",
                "free\\s+(?:reward|gift|iphone|recharge|voucher|prize)",
                "jackpot",
                "cash\\s+prize",
                "(?:selected|chosen)\\s+(?:as|for)\\s+(?:a\\s+|the\\s+)?(?:winner|reward|prize|gift)"
        ));
    }
}
