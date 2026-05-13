package com.hm.synapse;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "blocks")
public class SynapseBlockEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String parentId;
    private String userId;
    private String type;
    private String content;
    private boolean completed;
    private boolean firstTimeCompleted;
    private boolean expAwarded; // Track if EXP was already granted for this task
    private double amount;
    private long timestamp;
    private String priority;
    private Long dueDate;
    private long lastAccessed;
    private int day, month, year;

    public SynapseBlockEntity(@NonNull String id, String userId, String type, String content) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.lastAccessed = System.currentTimeMillis();
        this.priority = "medium";
        this.expAwarded = false;
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public boolean isFirstTimeCompleted() { return firstTimeCompleted; }
    public void setFirstTimeCompleted(boolean firstTimeCompleted) { this.firstTimeCompleted = firstTimeCompleted; }

    public boolean isExpAwarded() { return expAwarded; }
    public void setExpAwarded(boolean expAwarded) { this.expAwarded = expAwarded; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Long getDueDate() { return dueDate; }
    public void setDueDate(Long dueDate) { this.dueDate = dueDate; }
    public long getLastAccessed() { return lastAccessed; }
    public void setLastAccessed(long lastAccessed) { this.lastAccessed = lastAccessed; }
    public int getDay() {return day;}
    public void setDay(int day) {this.day = day;}
    public int getMonth() {return month;}
    public void setMonth(int month) {this.month = month;}
    public int getYear() {return year;}
    public void setYear(int year) {this.year = year;}
}
