package com.origin.launcher.discord;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import androidx.fragment.app.Fragment;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscordManager {
    private static final String TAG = "DiscordManager";
    
    private static final String PREFS_NAME = "discord_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_DISCRIMINATOR = "discriminator";
    private static final String KEY_AVATAR = "avatar";
    
    private Context context;
    private Fragment fragment; // Add fragment reference for startActivityForResult
    private SharedPreferences prefs;
    private ExecutorService executor;
    private Handler mainHandler;
    private DiscordLoginCallback callback;
    private DiscordRPC discordRPC;
    
    // RPC state
    private boolean rpcConnected = false;
    
    public interface DiscordLoginCallback {
        void onLoginSuccess(DiscordUser user);
        void onLoginError(String error);
        void onLogout();
        void onRPCConnected();
        void onRPCDisconnected();
    }
    
    public static class DiscordUser {
        public String id;
        public String username;
        public String discriminator;
        public String avatar;
        public String displayName;
        
        public DiscordUser(String id, String username, String discriminator, String avatar) {
            this.id = id;
            this.username = username;
            this.discriminator = discriminator != null ? discriminator : "0";
            this.avatar = avatar != null ? avatar : "";
            this.displayName = username + (this.discriminator.equals("0") ? "" : "#" + this.discriminator);
        }
    }
    
    public DiscordManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.discordRPC = new DiscordRPC(context);
        this.discordRPC.setCallback(new DiscordRPC.DiscordRPCCallback() {
            @Override
            public void onConnected() {
                rpcConnected = true;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onRPCConnected();
                    }
                });
            }
            
            @Override
            public void onDisconnected() {
                rpcConnected = false;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onRPCDisconnected();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Discord RPC error: " + error);
                rpcConnected = false;
            }
            
            @Override
            public void onPresenceUpdated() {
                Log.d(TAG, "Discord presence updated");
            }
        });
    }
    
    public void setCallback(DiscordLoginCallback callback) {
        this.callback = callback;
    }
    
    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
    }
    
    public boolean isLoggedIn() {
        return prefs.contains(KEY_ACCESS_TOKEN) && prefs.contains(KEY_USER_ID);
    }
    
    public boolean isRPCConnected() {
        return rpcConnected;
    }
    
    public DiscordUser getCurrentUser() {
        if (!isLoggedIn()) return null;
        
        return new DiscordUser(
            prefs.getString(KEY_USER_ID, ""),
            prefs.getString(KEY_USERNAME, ""),
            prefs.getString(KEY_DISCRIMINATOR, "0"),
            prefs.getString(KEY_AVATAR, "")
        );
    }
    
    public void login() {
        Log.d(TAG, "Starting Discord login process");
        
        // Check if we have both Activity and Fragment references
        if (fragment != null) {
            try {
                Intent intent = new Intent(fragment.getContext(), DiscordLoginActivity.class);
                Log.d(TAG, "Starting DiscordLoginActivity via Fragment");
                fragment.startActivityForResult(intent, DiscordLoginActivity.DISCORD_LOGIN_REQUEST_CODE);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error starting Discord login activity via Fragment", e);
            }
        }
        
        // Fallback to Activity if Fragment not available
        if (context instanceof Activity) {
            try {
                Activity activity = (Activity) context;
                Intent intent = new Intent(activity, DiscordLoginActivity.class);
                Log.d(TAG, "Starting DiscordLoginActivity via Activity");
                activity.startActivityForResult(intent, DiscordLoginActivity.DISCORD_LOGIN_REQUEST_CODE);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error starting Discord login activity via Activity", e);
            }
        }
        
        // If neither worked, report error
        Log.e(TAG, "Cannot start login activity - no valid context");
        if (callback != null) {
            mainHandler.post(() -> callback.onLoginError("Invalid context for login"));
        }
    }
    
    public void handleLoginResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "handleLoginResult: requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + (data != null ? "present" : "null"));
        
        if (requestCode == DiscordLoginActivity.DISCORD_LOGIN_REQUEST_CODE) {
            Log.d(TAG, "Processing Discord login result");
            if (resultCode == Activity.RESULT_OK && data != null) {
                String accessToken = data.getStringExtra("access_token");
                String userId = data.getStringExtra("user_id");
                String username = data.getStringExtra("username");
                String discriminator = data.getStringExtra("discriminator");
                String avatar = data.getStringExtra("avatar");
                boolean success = data.getBooleanExtra("success", false);
                
                Log.d(TAG, "Login result data - success: " + success + 
                          ", accessToken: " + (accessToken != null ? "present" : "null") + 
                          ", userId: " + (userId != null ? "present" : "null") + 
                          ", username: " + (username != null ? "present" : "null"));
                
                if (success && accessToken != null && userId != null && username != null) {
                    // Save user info
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(KEY_ACCESS_TOKEN, accessToken);
                    editor.putString(KEY_USER_ID, userId);
                    editor.putString(KEY_USERNAME, username);
                    editor.putString(KEY_DISCRIMINATOR, discriminator != null ? discriminator : "0");
                    editor.putString(KEY_AVATAR, avatar != null ? avatar : "");
                    boolean saved = editor.commit(); // Use commit() instead of apply() for immediate save
                    
                    Log.d(TAG, "User data saved successfully: " + saved);
                    
                    // Set access token for RPC
                    discordRPC.setAccessToken(accessToken);
                    discordRPC.setUserId(userId);
                    
                    DiscordUser user = new DiscordUser(userId, username, discriminator, avatar);
                    
                    if (callback != null) {
                        mainHandler.post(() -> callback.onLoginSuccess(user));
                    }
                    
                    // Start RPC connection after successful login
                    startRPC();
                    
                    Log.i(TAG, "Discord login successful for user: " + user.displayName);
                } else {
                    Log.e(TAG, "Invalid login response - missing required data");
                    if (callback != null) {
                        mainHandler.post(() -> callback.onLoginError("Invalid login response"));
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                String error = data != null ? data.getStringExtra("error") : "Login cancelled";
                Log.d(TAG, "Login cancelled: " + error);
                if (callback != null) {
                    mainHandler.post(() -> callback.onLoginError(error));
                }
            } else {
                Log.e(TAG, "Unexpected result code: " + resultCode);
                if (callback != null) {
                    mainHandler.post(() -> callback.onLoginError("Unexpected result: " + resultCode));
                }
            }
        } else {
            Log.d(TAG, "Not a Discord login request code, ignoring");
        }
    }
    
    public void startRPC() {
        if (!isLoggedIn()) {
            Log.w(TAG, "Cannot start RPC: not logged in");
            return;
        }
        
        Log.d(TAG, "Starting Discord RPC connection");
        discordRPC.connect();
    }
    
    public void stopRPC() {
        Log.d(TAG, "Stopping Discord RPC connection");
        discordRPC.disconnect();
    }
    
    public void updatePresence(String activity, String details) {
        if (!isLoggedIn()) {
            Log.d(TAG, "Cannot update presence: not logged in");
            return;
        }
        
        if (!rpcConnected) {
            Log.d(TAG, "Cannot update presence: RPC not connected, attempting to connect...");
            startRPC();
            return;
        }
        
        Log.d(TAG, "Updating Discord presence: " + activity + " - " + details);
        discordRPC.updatePresence(activity, details);
    }
    
    public void logout() {
        // Stop RPC first
        stopRPC();
        
        // Clear WebView data (similar to your friend's approach)
        executor.execute(() -> {
            try {
                clearDiscordWebViewData();
            } catch (Exception e) {
                Log.w(TAG, "Error clearing WebView data: " + e.getMessage());
            }
            
            // Clear all stored data
            prefs.edit().clear().commit(); // Use commit() for immediate clear
            
            Log.i(TAG, "Discord logout successful");
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onLogout();
                }
            });
        });
    }
    
    private void clearDiscordWebViewData() {
        try {
            // Clear WebView storage (similar to your friend's logout method)
            java.io.File webViewDir = new java.io.File(context.getFilesDir().getParentFile(), "app_webview");
            java.io.File cacheDir = new java.io.File(context.getFilesDir().getParentFile(), "cache");
            java.io.File sharedPrefsDir = new java.io.File(context.getFilesDir().getParentFile(), "shared_prefs");
            
            deleteRecursively(webViewDir);
            deleteRecursively(cacheDir);
            // Don't delete all shared prefs, just Discord-related ones
            // deleteRecursively(sharedPrefsDir);
            
        } catch (Exception e) {
            Log.w(TAG, "Error clearing WebView data", e);
        }
    }
    
    private boolean deleteRecursively(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            java.io.File[] children = dir.listFiles();
            if (children != null) {
                for (java.io.File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        return dir != null && dir.delete();
    }
    
    public void destroy() {
        stopRPC();
        
        if (discordRPC != null) {
            discordRPC.destroy();
        }
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public DiscordRPC getDiscordRPC() {
        return discordRPC;
    }
    
    // Helper method to get token (similar to your friend's approach)
    public String getStoredToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }
}