package com.csec.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final AntiCheatPlugin plugin;
    private final File dataFolder;
    private final Map<String, PlayerData> playerDataMap;
    private final Set<String> authenticatedPlayers;
    
    public PlayerDataManager(AntiCheatPlugin plugin, File dataFolder) {
        this.plugin = plugin;
        this.dataFolder = dataFolder;
        this.playerDataMap = new ConcurrentHashMap<>();
        this.authenticatedPlayers = ConcurrentHashMap.newKeySet();
        
        // Start auto-save task (every 5 minutes)
        startAutoSaveTask();
    }
    
    public PlayerData getOrCreatePlayerData(String playerName, String uuid) {
        return playerDataMap.computeIfAbsent(playerName, k -> {
            PlayerData data = new PlayerData(playerName, uuid);
            data.incrementConnectionCount();
            return data;
        });
    }
    
    public PlayerData getPlayerData(String playerName) {
        return playerDataMap.get(playerName);
    }
    
    public void authenticatePlayer(String playerName) {
        authenticatedPlayers.add(playerName);
        PlayerData data = playerDataMap.get(playerName);
        if (data != null) {
            data.setAuthenticated(true);
        }
    }
    
    public void unauthenticatePlayer(String playerName) {
        authenticatedPlayers.remove(playerName);
        PlayerData data = playerDataMap.get(playerName);
        if (data != null) {
            data.setAuthenticated(false);
        }
    }
    
    public boolean isPlayerAuthenticated(String playerName) {
        return authenticatedPlayers.contains(playerName);
    }
    
    public Set<String> getAuthenticatedPlayers() {
        return new HashSet<>(authenticatedPlayers);
    }
    
    public void savePlayerData(String playerName) {
        PlayerData data = playerDataMap.get(playerName);
        if (data == null) {
            return;
        }
        
        File playerFile = new File(dataFolder, playerName + "_mods.txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(playerFile))) {
            writer.print(data.toFileFormat());
            plugin.getLogger().info("Saved mod data for player: " + playerName);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + playerName + ": " + e.getMessage());
        }
    }
    
    public void saveAllPlayerData() {
        for (String playerName : playerDataMap.keySet()) {
            savePlayerData(playerName);
        }
    }
    
    private void startAutoSaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAllPlayerData();
                plugin.getLogger().info("Auto-saved all player data");
            }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L); // Every 5 minutes (6000 ticks)
    }
    
    public int getTotalPlayersTracked() {
        return playerDataMap.size();
    }
}
