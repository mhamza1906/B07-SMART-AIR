package com.example.smart_air;

import java.util.List;

public class ChildSummary {
    private String childname;
    private String todays_zone;
    private String last_rescue;
    private int weekly_rescue;
    private String dob;
    private List<Float> graphData;

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
