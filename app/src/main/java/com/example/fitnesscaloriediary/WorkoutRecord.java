package com.example.fitnesscaloriediary;

public class WorkoutRecord {
    private String date;
    private String type;
    private int duration;
    private int calories;

    public WorkoutRecord(String date, String type, int duration, int calories) {
        this.date = date;
        this.type = type;
        this.duration = duration;
        this.calories = calories;
    }

    public String getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public int getDuration() {
        return duration;
    }

    public int getCalories() {
        return calories;
    }
}