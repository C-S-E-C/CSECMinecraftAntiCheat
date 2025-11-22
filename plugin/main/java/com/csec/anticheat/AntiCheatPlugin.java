package com.csec.anticheat;

import com.csec.anticheat.commands.AntiCheatCommand;
import com.csec.anticheat.listeners.PlayerConnectionListener;
import com.csec.anticheat.network.SocketServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class AntiCheatPlugin extends JavaPlugin {
    
    private SocketServer socketServer;
    private PlayerDataManager playerDataManager;
    private File dataFolder;
    
    @Override
    public void onEnable() {
        getLogger().info("=================================");
        getLogger().info("CSEC Anti-Cheat Plugin Starting...");
        getLogger().info("=================================");
        
        // Create plugin data folder
        dataFolder = new File(getDataFolder(), "player_mods");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
            getLogger().info("Created player_mods directory");
        }
        
        // Initialize player data manager
        playerDataManager = new PlayerDataManager(this, dataFolder);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(
            new PlayerConnectionListener(this, playerDataManager), this);
        getLogger().info("Registered event listeners");
        
        // Register commands
        getCommand("anticheat").setExecutor(new AntiCheatCommand(this, playerDataManager));
        getLogger().info("Registered commands");
        
        // Start socket server
        socketServer = new SocketServer(this, playerDataManager);
        socketServer.start();
        
        getLogger().info("=================================");
        getLogger().info("CSEC Anti-Cheat Plugin Enabled!");
        getLogger().info("=================================");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("CSEC Anti-Cheat Plugin shutting down...");
        
        // Stop socket server
        if (socketServer != null) {
            socketServer.stop();
            getLogger().info("Socket server stopped");
        }
        
        // Save all player data
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
            getLogger().info("All player data saved");
        }
        
        getLogger().info("CSEC Anti-Cheat Plugin Disabled!");
    }
    
    public SocketServer getSocketServer() {
        return socketServer;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}
