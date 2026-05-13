package com.hm.synapse;

public class Task {
    private String id;
    private String title;
    private boolean completed;
    private boolean isStale;

    public Task(String id, String title, boolean completed, boolean isStale) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.isStale = isStale;
    }

    public String getTitle() { return title; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public boolean isStale() { return isStale; }
}
