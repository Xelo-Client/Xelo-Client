package com.origin.launcher.discord;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.json.JSONArray;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import android.os.Handler;
import android.os.Looper;

public class DiscordRPC {
    private static final String TAG = "DiscordRPC";
    private static final String GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json";
    private static final String APPLICATION_ID = "1403634750559752296"; // Your Application ID
    
    private static final String PREFS_NAME = "discord_rpc_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";
    
    private Context context;
    private SharedPreferences prefs;
    private WebSocketClient webSocket;
    private ExecutorService executor;
    private ScheduledExecutorService heartbeatExecutor;
    private Handler mainHandler;
    private DiscordRPCCallback callback;
    
    // RPC state
    private AtomicBoolean connected = new AtomicBoolean(false);
    private AtomicBoolean connecting = new AtomicBoolean(false);
    private AtomicLong sequence = new AtomicLong(0);
    private AtomicLong heartbeatInterval = new AtomicLong(41250); // Default interval
    private String sessionId;
    private String currentActivity = "";
    private String currentDetails = "";
    private long startTime;
    
    // Heartbeat tracking
    private volatile boolean heartbeatAcknowledged = true;
    private volatile long lastHeartbeat = 0;
    private volatile boolean shouldReconnect = true;
    
    public interface DiscordRPCCallback {
        void onConnected();
        void onDisconnected();
        void onError(String error);
        void onPresenceUpdated();
    }
    
    public DiscordRPC(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
        this.heartbeatExecutor = Executors.newScheduledThreadPool(1);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.startTime = System.currentTimeMillis();
    }
    
    public void setCallback(DiscordRPCCallback callback) {
        this.callback = callback;
    }
    
    public boolean isConnected() {
        return connected.get();
    }
    
    public void connect() {
        if (connected.get() || connecting.get()) {
            Log.d(TAG, "Already connected or connecting to Discord RPC");
            return;
        }
        
        String accessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
        if (accessToken == null) {
            Log.e(TAG, "No access token available for RPC connection");
            if (callback != null) {
                mainHandler.post(() -> callback.onError("No access token available"));
            }
            return;
        }
        
        connecting.set(true);
        shouldReconnect = true;
        
        executor.execute(() -> {
            try {
                Log.d(TAG, "Connecting to Discord Gateway...");
                
                URI uri = new URI(GATEWAY_URL);
                webSocket = new WebSocketClient(uri) {
                    @Override
                    public void onOpen(ServerHandshake handshake) {
                        Log.d(TAG, "Discord Gateway WebSocket opened");
                        connecting.set(false);
                    }
                    
                    @Override
                    public void onMessage(String message) {
                        handleMessage(message);
                    }
                    
                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        Log.d(TAG, "Discord Gateway WebSocket closed: " + code + " - " + reason);
                        connected.set(false);
                        connecting.set(false);
                        
                        // Stop heartbeat
                        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
                            heartbeatExecutor.shutdownNow();
                            heartbeatExecutor = Executors.newScheduledThreadPool(1);
                        }
                        
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onDisconnected();
                            }
                        });
                        
                        // Attempt reconnection if needed
                        if (shouldReconnect && code != 1000) {
                            reconnectWithDelay();
                        }
                    }
                    
                    @Override
                    public void onError(Exception ex) {
                        Log.e(TAG, "Discord Gateway WebSocket error", ex);
                        connected.set(false);
                        connecting.set(false);
                        
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onError("Connection error: " + ex.getMessage());
                            }
                        });
                    }
                };
                
                webSocket.connect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error connecting to Discord Gateway", e);
                connected.set(false);
                connecting.set(false);
                
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Connection error: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    private void reconnectWithDelay() {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Reconnecting to Discord Gateway in 5 seconds...");
                Thread.sleep(5000);
                if (shouldReconnect) {
                    connect();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private void handleMessage(String message) {
        try {
            JSONObject payload = new JSONObject(message);
            int op = payload.getInt("op");
            
            switch (op) {
                case 10: // Hello
                    handleHello(payload);
                    break;
                case 11: // Heartbeat ACK
                    handleHeartbeatAck();
                    break;
                case 0: // Dispatch
                    handleDispatch(payload);
                    break;
                case 1: // Heartbeat request
                    sendHeartbeat();
                    break;
                case 7: // Reconnect
                    Log.d(TAG, "Discord requested reconnection");
                    reconnect();
                    break;
                case 9: // Invalid session
                    Log.w(TAG, "Invalid session, reconnecting...");
                    reconnect();
                    break;
                default:
                    Log.d(TAG, "Unhandled opcode: " + op);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling message: " + message, e);
        }
    }
    
    private void handleHello(JSONObject payload) {
        try {
            JSONObject data = payload.getJSONObject("d");
            long interval = data.getLong("heartbeat_interval");
            heartbeatInterval.set(interval);
            
            Log.d(TAG, "Received hello, heartbeat interval: " + interval + "ms");
            
            // Start heartbeat
            startHeartbeat();
            
            // Send identify
            sendIdentify();
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling hello", e);
        }
    }
    
    private void sendIdentify() {
        try {
            String accessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
            if (accessToken == null) {
                Log.e(TAG, "No access token for identify");
                return;
            }
            
            JSONObject properties = new JSONObject();
            properties.put("os", "Android");
            properties.put("browser", "Xelo Client");
            properties.put("device", "Xelo Client");
            
            JSONObject identify = new JSONObject();
            identify.put("op", 2); // Identify opcode
            
            JSONObject data = new JSONObject();
            data.put("token", accessToken);
            data.put("properties", properties);
            data.put("intents", 0); // No intents needed for RPC
            
            // Set initial presence
            JSONObject presence = new JSONObject();
            presence.put("status", "online");
            presence.put("since", 0);
            presence.put("activities", createActivityArray());
            presence.put("afk", false);
            data.put("presence", presence);
            
            identify.put("d", data);
            
            Log.d(TAG, "Sending identify payload");
            webSocket.send(identify.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending identify", e);
        }
    }
    
    private JSONArray createActivityArray() {
        try {
            JSONArray activities = new JSONArray();
            
            if (!currentActivity.isEmpty() || !currentDetails.isEmpty()) {
                JSONObject activity = new JSONObject();
                activity.put("name", "Xelo Client");
                activity.put("type", 0); // Playing
                activity.put("application_id", APPLICATION_ID); // Add Application ID
                activity.put("details", currentDetails.isEmpty() ? "Using Xelo Client" : currentDetails);
                activity.put("state", currentActivity.isEmpty() ? "Best MCPE Client" : currentActivity);
                
                // Add timestamps
                JSONObject timestamps = new JSONObject();
                timestamps.put("start", startTime);
                activity.put("timestamps", timestamps);
                
                // Add assets with your uploaded image
                JSONObject assets = new JSONObject();
                assets.put("large_image", "untitled224_20250731110425"); // Your uploaded asset key (remove .png extension)
                assets.put("large_text", "Xelo Client - Best MCPE Client");
                // Optional: Add small image if you have one
                // assets.put("small_image", "small_icon_key");
                // assets.put("small_text", "Version 1.0");
                activity.put("assets", assets);
                
                activities.put(activity);
                
                Log.d(TAG, "Created activity with image asset: untitled224_20250731110425");
            }
            
            return activities;
        } catch (Exception e) {
            Log.e(TAG, "Error creating activity array", e);
            return new JSONArray();
        }
    }
    
    private void handleHeartbeatAck() {
        Log.d(TAG, "Heartbeat acknowledged");
        heartbeatAcknowledged = true;
    }
    
    private void handleDispatch(JSONObject payload) {
        try {
            String event = payload.getString("t");
            JSONObject data = payload.getJSONObject("d");
            
            if (payload.has("s") && !payload.isNull("s")) {
                sequence.set(payload.getLong("s"));
            }
            
            switch (event) {
                case "READY":
                    handleReady(data);
                    break;
                case "PRESENCE_UPDATE":
                    handlePresenceUpdate(data);
                    break;
                default:
                    Log.d(TAG, "Unhandled dispatch event: " + event);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling dispatch", e);
        }
    }
    
    private void handleReady(JSONObject data) {
        try {
            sessionId = data.getString("session_id");
            connected.set(true);
            
            Log.i(TAG, "Discord RPC connected successfully! Session ID: " + sessionId);
            
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onConnected();
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling ready", e);
        }
    }
    
    private void handlePresenceUpdate(JSONObject data) {
        Log.d(TAG, "Presence update received");
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onPresenceUpdated();
            }
        });
    }
    
    private void startHeartbeat() {
        if (heartbeatExecutor.isShutdown()) {
            heartbeatExecutor = Executors.newScheduledThreadPool(1);
        }
        
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (connected.get() && webSocket != null && webSocket.isOpen()) {
                if (!heartbeatAcknowledged) {
                    Log.w(TAG, "Previous heartbeat not acknowledged, reconnecting...");
                    reconnect();
                    return;
                }
                
                sendHeartbeat();
            }
        }, 0, heartbeatInterval.get(), TimeUnit.MILLISECONDS);
    }
    
    private void sendHeartbeat() {
        try {
            JSONObject heartbeat = new JSONObject();
            heartbeat.put("op", 1); // Heartbeat opcode
            heartbeat.put("d", sequence.get() == 0 ? JSONObject.NULL : sequence.get());
            
            Log.d(TAG, "Sending heartbeat, sequence: " + sequence.get());
            webSocket.send(heartbeat.toString());
            
            heartbeatAcknowledged = false;
            lastHeartbeat = System.currentTimeMillis();
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending heartbeat", e);
        }
    }
    
    public void updatePresence(String activity, String details) {
        currentActivity = activity != null ? activity : "";
        currentDetails = details != null ? details : "";
        
        if (!connected.get()) {
            Log.d(TAG, "Not connected, caching presence update");
            return;
        }
        
        executor.execute(() -> {
            try {
                Log.d(TAG, "Updating Discord presence: " + activity + " - " + details);
                
                JSONObject presence = new JSONObject();
                presence.put("op", 3); // Presence Update opcode
                
                JSONObject data = new JSONObject();
                data.put("status", "online");
                data.put("since", 0);
                data.put("activities", createActivityArray());
                data.put("afk", false);
                
                presence.put("d", data);
                
                webSocket.send(presence.toString());
                
                Log.i(TAG, "Discord presence updated successfully with image asset");
                
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onPresenceUpdated();
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating presence", e);
            }
        });
    }
    
    private void reconnect() {
        Log.d(TAG, "Reconnecting to Discord RPC...");
        disconnect();
        
        // Wait a bit before reconnecting
        executor.execute(() -> {
            try {
                Thread.sleep(2000);
                if (shouldReconnect) {
                    connect();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    public void disconnect() {
        Log.d(TAG, "Disconnecting from Discord RPC...");
        
        shouldReconnect = false;
        connected.set(false);
        connecting.set(false);
        heartbeatAcknowledged = true;
        
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.close();
        }
        
        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdown();
            try {
                if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    heartbeatExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void destroy() {
        disconnect();
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void setAccessToken(String accessToken) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply();
    }
    
    public void setUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }
    
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
}