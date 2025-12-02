package com.example.smart_air;

import java.util.Date;

public class PefHistoryItem {
    private final Date timestamp;
    private final int percent;
    private final String zone;

    public PefHistoryItem(Date timestamp, int percent, String zone) {
        this.timestamp = timestamp;
        this.percent = percent;
        this.zone = zone;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public int getPercent() {
        return percent;
    }

    public String getZone() {
        return zone;
    }
}
