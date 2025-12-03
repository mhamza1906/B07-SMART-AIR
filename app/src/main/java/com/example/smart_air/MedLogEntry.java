package com.example.smart_air;

public class MedLogEntry {
    private final String date;
    private final String time;
    private final String type;
    private final int numberOfDoses;
    private final int preBreathRating;
    private final int postBreathRating;
    private final String postCheck;

    public MedLogEntry(String date, String time, String type, int numberOfDoses, int preBreathRating, int postBreathRating, String postCheck) {
        this.date = date;
        this.time = time;
        this.type = type;
        this.numberOfDoses = numberOfDoses;
        this.preBreathRating = preBreathRating;
        this.postBreathRating = postBreathRating;
        this.postCheck = postCheck;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public int getNumberOfDoses() {
        return numberOfDoses;
    }

    public int getPreBreathRating() {
        return preBreathRating;
    }

    public int getPostBreathRating() {
        return postBreathRating;
    }

    public String getPostCheck() {
        return postCheck;
    }
}
