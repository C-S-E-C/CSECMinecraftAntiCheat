package com.csec.anticheat.listeners;

import com.csec.anticheat.AntiCheatPlugin;
import com.csec.anticheat.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerConnectionListener implements Listener {
    
    private final AntiCheatPlugin plugin;
    private final PlayerDataManager playerDataManager;
    
    public PlayerConnectionListener(AntiCheatPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        
        // Bypass permission check
        if (player.hasPermission("csec.anticheat.bypass")) {
            plugin.getLogger().info("Player " + playerName + " has bypass permission, skipping check");
            return;
        }
        
        plugin.getLogger().info("Player " + playerName + " joined, checking anti-cheat client...");
        
        // Delay check to give client time to connect
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!playerDataManager.isPlayerAuthenticated(playerName)) {
                    plugin.getLogger().warning("Player " + playerName + " has no anti-cheat client, kicking...");
                    player.kickPlayer("§c§lCSEC Anti-Cheat System\n\n" +
                        "§eAnti-cheat client not detected\n\n" +
                        "§7Please ensure the CSEC anti-cheat client is running\n" +
                        "§7before joining the server.");
                } else {
                    plugin.getLogger().info("Player " + playerName + " passed anti-cheat verification");
                    player.sendMessage("§a§lCSEC Anti-Cheat §7» §aVerification successful!");
                }
            }
        }.runTaskLater(plugin, 60L); // 3 seconds (20 ticks = 1 second)
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String playerName = event.getPlayer().getName();
        
        // Save player data on disconnect
        playerDataManager.savePlayerData(playerName);
        plugin.getLogger().info("Saved data for disconnecting player: " + playerName);
    }
}
