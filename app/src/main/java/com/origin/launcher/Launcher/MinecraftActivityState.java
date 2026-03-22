package com.origin.launcher.Launcher;

import android.app.Activity;
import java.lang.ref.WeakReference;

public final class MinecraftActivityState {
    private static volatile boolean running = false;
    private static volatile boolean resumed = false;
    private static WeakReference<Activity> currentActivityRef;

    private MinecraftActivityState() {}

    public static void onCreated(Activity activity) {
        running = true;
        currentActivityRef = new WeakReference<>(activity);
    }

    public static void onResumed() {
        resumed = true;
    }

    public static void onPaused() {
        resumed = false;
    }

    public static void onDestroyed() {
        running = false;
        resumed = false;
        currentActivityRef = null;
    }

    public static boolean isRunning() {
        return running;
    }

    public static boolean isResumed() {
        return resumed;
    }

    public static Activity getCurrentActivity() {
        return currentActivityRef != null ? currentActivityRef.get() : null;
    }
}