package com.csec.anticheat;

import java.util.*;

public class PlayerData {
    private final String playerName;
    private final String uuid;
    private List<String> detectedMods;
    private List<String> cheatHistory;
    private long lastUpdateTime;
    private int connectionCount;
    private boolean isAuthenticated;
    
    public PlayerData(String playerName, String uuid) {
        this.playerName = playerName;
        this.uuid = uuid;
        this.detectedMods = new ArrayList<>();
        this.cheatHistory = new ArrayList<>();
        this.lastUpdateTime = System.currentTimeMillis();
        this.connectionCount = 0;
        this.isAuthenticated = false;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public String getUuid() {
        return uuid;
    }
    
    public List<String> getDetectedMods() {
        return new ArrayList<>(detectedMods);
    }
    
    public void setDetectedMods(List<String> mods) {
        this.detectedMods = new ArrayList<>(mods);
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public void addCheatDetection(String cheatType) {
        String timestamp = new Date().toString();
        cheatHistory.add(String.format("[%s] %s", timestamp, cheatType));
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public List<String> getCheatHistory() {
        return new ArrayList<>(cheatHistory);
    }
    
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public void incrementConnectionCount() {
        this.connectionCount++;
    }
    
    public int getConnectionCount() {
        return connectionCount;
    }
    
    public boolean isAuthenticated() {
        return isAuthenticated;
    }
    
    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }
    
    public String toFileFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("===========================================\n");
        sb.append("Player: ").append(playerName).append("\n");
        sb.append("UUID: ").append(uuid).append("\n");
        sb.append("Last Update: ").append(new Date(lastUpdateTime)).append("\n");
        sb.append("Connection Count: ").append(connectionCount).append("\n");
        sb.append("Authenticated: ").append(isAuthenticated ? "Yes" : "No").append("\n");
        sb.append("===========================================\n\n");
        
        sb.append("Detected Mods (").append(detectedMods.size()).append("):\n");
        sb.append("-------------------------------------------\n");
        if (detectedMods.isEmpty()) {
            sb.append("  No mods detected\n");
        } else {
            for (int i = 0; i < detectedMods.size(); i++) {
                sb.append(String.format("  %d. %s\n", i + 1, detectedMods.get(i)));
            }
        }
        sb.append("\n");
        
        sb.append("Cheat Detection History (").append(cheatHistory.size()).append("):\n");
        sb.append("-------------------------------------------\n");
        if (cheatHistory.isEmpty()) {
            sb.append("  No cheats detected\n");
        } else {
            for (String detection : cheatHistory) {
                sb.append("  ").append(detection).append("\n");
            }
        }
        
        sb.append("\n===========================================\n");
        return sb.toString();
    }
}
