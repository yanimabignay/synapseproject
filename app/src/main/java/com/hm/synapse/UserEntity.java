package com.hm.synapse;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    private String id; // Firebase UID
    private String email;
    private String role; // student, freelancer, pro
    private int exp;
    private int level;
    private boolean twoFactorEnabled;

    public UserEntity(@NonNull String id, String email) {
        this.id = id;
        this.email = email;
        this.role = "general";
        this.exp = 0;
        this.level = 1;
        this.twoFactorEnabled = false;
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getExp() { return exp; }
    public void setExp(int exp) { this.exp = exp; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }
}
