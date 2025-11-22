package com.csec.anticheat.commands;

import com.csec.anticheat.AntiCheatPlugin;
import com.csec.anticheat.PlayerData;
import com.csec.anticheat.PlayerDataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AntiCheatCommand implements CommandExecutor {
    
    private final AntiCheatPlugin plugin;
    private final PlayerDataManager playerDataManager;
    
    public AntiCheatCommand(AntiCheatPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("csec.anticheat.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "status":
                showStatus(sender);
                break;
                
            case "check":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /anticheat check <player>");
                    return true;
                }
                checkPlayer(sender, args[1]);
                break;
                
            case "reload":
                sender.sendMessage("§aReloading anti-cheat plugin...");
                plugin.onDisable();
                plugin.onEnable();
                sender.sendMessage("§aAnti-cheat plugin reloaded!");
                break;
                
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== CSEC Anti-Cheat Commands ===");
        sender.sendMessage("§e/anticheat status §7- Show system status");
        sender.sendMessage("§e/anticheat check <player> §7- Check player info");
        sender.sendMessage("§e/anticheat reload §7- Reload the plugin");
    }
    
    private void showStatus(CommandSender sender) {
        int connectedClients = plugin.getSocketServer().getConnectedClientsCount();
        int authenticatedPlayers = playerDataManager.getAuthenticatedPlayers().size();
        int totalTracked = playerDataManager.getTotalPlayersTracked();
        
        sender.sendMessage("§6§l=== CSEC Anti-Cheat Status ===");
        sender.sendMessage("§eConnected Clients: §a" + connectedClients);
        sender.sendMessage("§eAuthenticated Players: §a" + authenticatedPlayers);
        sender.sendMessage("§eTotal Players Tracked: §a" + totalTracked);
        sender.sendMessage("§eAuthenticated: §a" + String.join(", ", playerDataManager.getAuthenticatedPlayers()));
    }
    
    private void checkPlayer(CommandSender sender, String playerName) {
        PlayerData data = playerDataManager.getPlayerData(playerName);
        
        if (data == null) {
            sender.sendMessage("§cNo data found for player: " + playerName);
            return;
        }
        
        sender.sendMessage("§6§l=== Player Info: " + playerName + " ===");
        sender.sendMessage("§eUUID: §7" + data.getUuid());
        sender.sendMessage("§eAuthenticated: §" + (data.isAuthenticated() ? "aYes" : "cNo"));
        sender.sendMessage("§eConnection Count: §7" + data.getConnectionCount());
        sender.sendMessage("§eLast Update: §7" + new java.util.Date(data.getLastUpdateTime()));
        
        List<String> mods = data.getDetectedMods();
        sender.sendMessage("§eMods (" + mods.size() + "): §7" + 
            (mods.isEmpty() ? "None" : String.join(", ", mods)));
        
        List<String> cheats = data.getCheatHistory();
        if (!cheats.isEmpty()) {
            sender.sendMessage("§cCheat History:");
            for (String cheat : cheats) {
                sender.sendMessage("  §7" + cheat);
            }
        } else {
            sender.sendMessage("§aNo cheats detected");
        }
    }
}
