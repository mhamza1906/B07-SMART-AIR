package com.example.smart_air;

import java.util.List;

public class ChildSummary {
    private final String childname;
    private final String todays_zone;
    private final String last_rescue;
    private final int weekly_rescue;
    private final String dob;
    private final List<Float> graphData;

    public ChildSummary(String childName, String todayZoneColor, String lastRescueTime, int weeklyRescueCount, String dob, List<Float> graphData) {
        this.childname = childName;
        this.todays_zone = todayZoneColor;
        this.last_rescue = lastRescueTime;
        this.weekly_rescue = weeklyRescueCount;
        this.dob = dob;
        this.graphData = graphData;
    }

    public String getChildName() {
        return childname;
    }


    public String getTodayZoneColor() {
        return todays_zone;
    }

    public String getLastRescueTime() {
        return last_rescue;
    }

    public int getWeeklyRescueCount() {
        return weekly_rescue;
    }

    public String getDob() {
        return dob;
    }

    public List<Float> getGraphData() {
        return graphData;
    }
}
