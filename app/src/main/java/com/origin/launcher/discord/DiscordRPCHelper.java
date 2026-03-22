package com.origin.launcher.discord;

import android.content.Context;
import android.util.Log;

/**
 * Helper class to manage Discord RPC throughout the application
 */
public class DiscordRPCHelper {
    private static final String TAG = "DiscordRPCHelper";
    private static DiscordManager discordManager;
    private static DiscordRPC discordRPC;
    private static DiscordRPCHelper instance;
    
    private DiscordRPCHelper() {}
    
    public static DiscordRPCHelper getInstance() {
        if (instance == null) {
            instance = new DiscordRPCHelper();
        }
        return instance;
    }
    
    /**
     * Initialize the Discord manager (call this from MainActivity or SettingsFragment)
     */
    public void initialize(DiscordManager manager) {
        discordManager = manager;
        Log.d(TAG, "DiscordRPCHelper initialized");
    }
    
    /**
     * Initialize with Discord RPC instance
     */
    public void initializeRPC(DiscordRPC rpc) {
        discordRPC = rpc;
        Log.d(TAG, "DiscordRPCHelper RPC initialized");
    }
    
    /**
     * Update Discord Rich Presence status
     * @param activity What the user is doing (e.g., "Playing Minecraft", "In Menu")
     * @param details Additional details (e.g., "Survival Mode", "World: My World")
     */
    public void updatePresence(String activity, String details) {
        if (discordRPC != null && discordRPC.isConnected()) {
            discordRPC.updatePresence(activity, details);
            Log.d(TAG, "Updated Discord presence: " + activity + " - " + details);
        } else if (discordManager != null && discordManager.isLoggedIn() && discordManager.isRPCConnected()) {
            discordManager.updatePresence(activity, details);
            Log.d(TAG, "Updated Discord presence via manager: " + activity + " - " + details);
        } else {
            Log.d(TAG, "Cannot update presence: Discord not connected or RPC not active");
        }
    }
    
    /**
     * Update presence for game-related activities
     */
    public void updateGamePresence(String gameMode, String worldName) {
        if (worldName != null && !worldName.isEmpty()) {
            updatePresence("Playing Minecraft", gameMode + " • " + worldName);
        } else {
            updatePresence("Playing Minecraft", gameMode);
        }
    }
    
    /**
     * Update presence for menu navigation
     */
    public void updateMenuPresence(String currentMenu) {
        updatePresence("In Menu", currentMenu);
    }
    
    /**
     * Update presence for idle state
     */
    public void updateIdlePresence() {
        updatePresence("Using Xelo Client", "Idle");
    }
    
    /**
     * Check if Discord RPC is available
     */
    public boolean isRPCAvailable() {
        return (discordRPC != null && discordRPC.isConnected()) || 
               (discordManager != null && discordManager.isLoggedIn() && discordManager.isRPCConnected());
    }
    
    /**
     * Check if user is logged into Discord
     */
    public boolean isLoggedIn() {
        return discordManager != null && discordManager.isLoggedIn();
    }
    
    /**
     * Get the current Discord user (if logged in)
     */
    public DiscordManager.DiscordUser getCurrentUser() {
        if (discordManager != null) {
            return discordManager.getCurrentUser();
        }
        return null;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (discordManager != null) {
            discordManager.destroy();
        }
        if (discordRPC != null) {
            discordRPC.destroy();
        }
        discordManager = null;
        discordRPC = null;
        instance = null;
        Log.d(TAG, "DiscordRPCHelper cleaned up");
    }
}