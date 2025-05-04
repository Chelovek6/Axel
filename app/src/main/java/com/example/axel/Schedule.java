package com.example.axel;

public class Schedule {
    private int id;
    private long startTime;
    private String daysOfWeek;
    private int duration;
    private String description;
    private String type;
    private boolean isActive;

    public Schedule() {}

    public Schedule(int id, long startTime, String daysOfWeek, int duration,
                    String description, String type, boolean isActive) {
        this.id = id;
        this.startTime = startTime;
        this.daysOfWeek = daysOfWeek;
        this.duration = duration;
        this.description = description;
        this.type = type;
        this.isActive = isActive;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public String getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}

