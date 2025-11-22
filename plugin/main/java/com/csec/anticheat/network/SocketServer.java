package com.csec.anticheat.network;

import com.csec.anticheat.AntiCheatPlugin;
import com.csec.anticheat.PlayerData;
import com.csec.anticheat.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SocketServer {
    private static final int PORT = 7878;
    private static final List<String> BANNED_MODS = Arrays.asList(
        "wurst", "meteor-client", "liquidbounce", "impact", "sigma"
    );
    
    private final AntiCheatPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private ServerSocket serverSocket;
    private final Set<Socket> connectedClients;
    private boolean isRunning;
    private Thread serverThread;
    
    public SocketServer(AntiCheatPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.connectedClients = ConcurrentHashMap.newKeySet();
        this.isRunning = false;
    }
    
    public void start() {
        if (isRunning) {
            plugin.getLogger().warning("Socket server is already running!");
            return;
        }
        
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            
            serverThread = new Thread(() -> {
                plugin.getLogger().info("Anti-cheat socket server started on port " + PORT);
                
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        plugin.getLogger().info("Client connected from: " + clientSocket.getInetAddress());
                        
                        connectedClients.add(clientSocket);
                        
                        // Handle each client in a new thread
                        new Thread(new ClientHandler(clientSocket)).start();
                        
                    } catch (IOException e) {
                        if (isRunning) {
                            plugin.getLogger().warning("Error accepting client connection: " + e.getMessage());
                        }
                    }
                }
            });
            
            serverThread.setName("CSEC-AntiCheat-Server");
            serverThread.start();
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to start socket server: " + e.getMessage());
        }
    }
    
    public void stop() {
        isRunning = false;
        
        // Close all client connections
        for (Socket client : connectedClients) {
            try {
                client.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        connectedClients.clear();
        
        // Close server socket
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        
        // Interrupt server thread
        if (serverThread != null) {
            serverThread.interrupt();
        }
        
        plugin.getLogger().info("Socket server stopped");
    }
    
    public int getConnectedClientsCount() {
        return connectedClients.size();
    }
    
    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private String playerName;
        private String uuid;
        
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }
        
        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(
                    clientSocket.getOutputStream(), true);
                
                // Read player authentication message
                String authMessage = reader.readLine();
                if (authMessage != null && authMessage.startsWith("AUTH:")) {
                    String[] parts = authMessage.substring(5).split(":");
                    playerName = parts[0];
                    uuid = parts.length > 1 ? parts[1] : UUID.randomUUID().toString();
                    
                    plugin.getLogger().info("Client authenticated: " + playerName);
                    playerDataManager.authenticatePlayer(playerName);
                    
                    // Create or get player data
                    PlayerData playerData = playerDataManager.getOrCreatePlayerData(playerName, uuid);
                    
                    writer.println("OK\n");
                    
                    // Handle client messages
                    String message;
                    while ((message = reader.readLine()) != null && isRunning) {
                        handleClientMessage(message, writer, playerData);
                    }
                }
                
            } catch (IOException e) {
                if (isRunning) {
                    plugin.getLogger().warning("Error in client communication: " + e.getMessage());
                }
            } finally {
                cleanup();
            }
        }
        
        private void handleClientMessage(String message, PrintWriter writer, PlayerData playerData) {
            if (message.startsWith("MODS:")) {
                // Parse mod list
                String modListStr = message.substring(5).trim();
                List<String> mods = parseModList(modListStr);
                
                plugin.getLogger().info("Received " + mods.size() + " mods from " + playerName);
                
                // Update player data
                playerData.setDetectedMods(mods);
                
                // Check for banned mods
                List<String> detectedCheats = new ArrayList<>();
                for (String mod : mods) {
                    for (String bannedMod : BANNED_MODS) {
                        if (mod.toLowerCase().contains(bannedMod)) {
                            detectedCheats.add(mod);
                        }
                    }
                }
                
                if (!detectedCheats.isEmpty()) {
                    plugin.getLogger().warning("CHEAT DETECTED for " + playerName + ": " + detectedCheats);
                    playerData.addCheatDetection("Detected mods: " + detectedCheats);
                    writer.println("CHEATER:" + String.join(",", detectedCheats) + "\n");
                    
                    // Kick player on main thread
                    kickPlayer(playerName, "Detected banned mods: " + String.join(", ", detectedCheats));
                } else {
                    writer.println("OK\n");
                }
                
                // Save player data
                playerDataManager.savePlayerData(playerName);
                
            } else if ("DETECT".equals(message)) {
                // Check if player is in game
                Player player = Bukkit.getPlayer(playerName);
                boolean isInGame = (player != null && player.isOnline());
                
                if (isInGame) {
                    writer.println("INGAME\n");
                } else {
                    writer.println("NOTINGAME\n");
                }
                
            } else {
                plugin.getLogger().info("Received from " + playerName + ": " + message);
                writer.println("RECEIVED:" + message + "\n");
            }
        }
        
        private List<String> parseModList(String modListStr) {
            List<String> mods = new ArrayList<>();
            
            // Remove brackets and split by comma
            modListStr = modListStr.trim();
            if (modListStr.startsWith("[")) {
                modListStr = modListStr.substring(1);
            }
            if (modListStr.endsWith("]")) {
                modListStr = modListStr.substring(0, modListStr.length() - 1);
            }
            
            String[] parts = modListStr.split(",");
            for (String part : parts) {
                String mod = part.trim().replace("'", "").replace("\"", "");
                if (!mod.isEmpty()) {
                    mods.add(mod);
                }
            }
            
            return mods;
        }
        
        private void kickPlayer(String playerName, String reason) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Player player = Bukkit.getPlayer(playerName);
                    if (player != null && player.isOnline()) {
                        player.kickPlayer("§c§lCSEC Anti-Cheat\n\n§e" + reason + 
                            "\n\n§7If you believe this is an error, please contact an administrator.");
                    }
                }
            }.runTask(plugin);
        }
        
        private void cleanup() {
            if (playerName != null) {
                playerDataManager.unauthenticatePlayer(playerName);
                playerDataManager.savePlayerData(playerName);
                plugin.getLogger().info("Client disconnected: " + playerName);
                
                // Kick player if still online
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Player player = Bukkit.getPlayer(playerName);
                        if (player != null && player.isOnline() && 
                            !player.hasPermission("csec.anticheat.bypass")) {
                            player.kickPlayer("§c§lCSEC Anti-Cheat\n\n§eClient disconnected\n§7Please restart the anti-cheat client.");
                        }
                    }
                }.runTask(plugin);
            }
            
            connectedClients.remove(clientSocket);
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
