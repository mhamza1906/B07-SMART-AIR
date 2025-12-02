package com.example.smart_air;

import java.util.List;

public class DailyCheckinHistoryItem {
    private final String date;
    private final String author;
    private final List<String> symptoms;
    private final List<String> triggers;

    public DailyCheckinHistoryItem(String date, String author, List<String> symptoms, List<String> triggers) {
        this.date = date;
        this.author = author;
        this.symptoms = symptoms;
        this.triggers = triggers;
    }

    public String getDate() {
        return date;
    }

    public String getAuthor() {
        return author;
    }

    public List<String> getSymptoms() {
        return symptoms;
    }

    public List<String> getTriggers() {
        return triggers;
    }
}
