package com.origin.launcher.manager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import android.widget.ProgressBar;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.MinecraftLauncher;
import com.origin.launcher.versions.GameVersion;
import com.origin.launcher.versions.VersionManager;
import com.origin.launcher.dialogs.CustomAlertDialog;

import java.util.concurrent.ExecutorService;

public class ResourcepackHandler {

    private final Activity activity;
    private final MinecraftLauncher minecraftLauncher;
    private final ExecutorService executor;

    public ResourcepackHandler(Activity activity, MinecraftLauncher minecraftLauncher,
                               ExecutorService executor) {
        this.activity = activity;
        this.minecraftLauncher = minecraftLauncher;
        this.executor = executor;
    }

    public void checkIntentForResourcepack() {
        Intent intent = activity.getIntent();
        Uri data = intent.getData();
        if (data != null) {
            String path = data.getPath();
            if (path != null && isMinecraftResourceFile(path)) {
                new CustomAlertDialog(activity)
                        .setTitleText(activity.getString(R.string.resourcepack_detected_title))
                        .setMessage(activity.getString(R.string.resourcepack_detected_message, path))
                        .setPositiveButton(activity.getString(R.string.launch_now), (d) -> launchMinecraft(intent))
                        .setNegativeButton(activity.getString(R.string.launch_later), null)
                        .show();
            }
        }
    }

    private boolean isMinecraftResourceFile(String path) {
        String lowerPath = path.toLowerCase();
        return lowerPath.endsWith(".mcworld") ||
                lowerPath.endsWith(".mcpack") ||
                lowerPath.endsWith(".mcaddon") ||
                lowerPath.endsWith(".mctemplate");
    }

    private void launchMinecraft(Intent intent) {
        executor.execute(() -> {
            VersionManager versionManager = VersionManager.get(activity);
            GameVersion currentVersion = versionManager.getSelectedVersion();

            minecraftLauncher.launch(intent, currentVersion);
            activity.runOnUiThread(() -> {
            });
        });
    }
}