package com.origin.launcher.manager;

import com.origin.launcher.Launcher.MinecraftActivity;
import com.origin.launcher.Launcher.MinecraftActivityState;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.util.List;

public class IntentHandler extends BaseThemedActivity {
    private static final String TAG = "IntentHandler";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleDeepLink(intent);
    }

    @SuppressLint("IntentReset")
    private void handleDeepLink(Intent originalIntent) {
        Intent newIntent = new Intent(originalIntent);
        if (isMinecraftActivityRunning()) {
            newIntent.setClass(this, MinecraftActivity.class);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {

            if (isMinecraftResourceFile(originalIntent)) {
                newIntent.setClassName(this, "com.origin.launcher.activity.MainActivity");
            } else {
                if (isMcRunning()) {
                    newIntent.setClassName(this, "com.mojang.minecraftpe.Launcher");
                } else {
                    newIntent.setClassName(this, "com.origin.launcher.activity.MainActivity");
                }
            }
        }

        startActivity(newIntent);
        finish();
    }

    private boolean isMinecraftResourceFile(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            String path = data.getPath();
            if (path != null) {
                String lowerPath = path.toLowerCase();
                return lowerPath.endsWith(".mcworld") ||
                        lowerPath.endsWith(".mcpack") ||
                        lowerPath.endsWith(".mcaddon") ||
                        lowerPath.endsWith(".mctemplate");
            }
        }
        return false;
    }

    private boolean isMinecraftActivityRunning() {
        if (MinecraftActivityState.isRunning()) return true;
        try {
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();
                for (ActivityManager.AppTask task : tasks) {
                    ActivityManager.RecentTaskInfo taskInfo = task.getTaskInfo();
                    if (taskInfo.baseActivity != null && 
                        taskInfo.baseActivity.getClassName().equals(MinecraftActivity.class.getName())) {
                        return true;
                    }
                    if (taskInfo.topActivity != null && 
                        taskInfo.topActivity.getClassName().equals(MinecraftActivity.class.getName())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if MinecraftActivity is running", e);
        }
        return false;
    }

    private boolean isMcRunning() {
        try {
            Class<?> clazz = Class.forName("com.mojang.minecraftpe.Launcher", false, getClassLoader());
            Log.d(TAG, "Minecraft PE Launcher class exists!");
            return true;
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "Minecraft PE Launcher class not found.");
            return false;
        }
    }
}