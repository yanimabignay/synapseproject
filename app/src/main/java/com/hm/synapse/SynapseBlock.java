package com.hm.synapse;

import java.util.HashMap;
import java.util.Map;

public class SynapseBlock {
    public enum Type { HEADER, TEXT, TODO, FINANCE, AI_SUGGESTION }

    private String id;
    private Type type;
    private String content;
    private boolean completed;
    private boolean firstTimeCompleted;
    private double amount; // For Finance
    private long timestamp;
    private Map<String, Object> metadata;

    public SynapseBlock() {} // Firebase requirement

    public SynapseBlock(String id, Type type, String content) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public Type getType() { return type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isCompleted() { return completed; }
    public boolean isFirstTimeCompleted() { return firstTimeCompleted; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setFirstTimeCompleted(boolean firstTimeCompleted) { this.firstTimeCompleted = firstTimeCompleted; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public long getTimestamp() { return timestamp; }
}
