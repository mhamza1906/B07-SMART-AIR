package com.example.smart_air;

public class PlannedSchedule {
    private String date;
    private boolean taken;
    private int controllerNum;

    public PlannedSchedule(String d, boolean t, int c) {
        this.date = d;
        this.taken = t;
        this.controllerNum = c;
    }

    public String getDate() {
        return date;
    }

    public boolean getTaken() {
        return taken;
    }

    public int getControllerNum() {
        return controllerNum;
    }
}
