package com.scamlens.model;

/**
 * Totals shown on the Dashboard. Always calculated from real database rows.
 */
public record DashboardStats(int total, int high, int suspicious, int low) {

    public boolean isEmpty() {
        return total == 0;
    }
}
