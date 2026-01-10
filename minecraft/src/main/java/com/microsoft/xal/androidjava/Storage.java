package com.microsoft.xal.androidjava;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class Storage {
    @NotNull
    public static String getStoragePath(@NotNull Context context) {
        boolean takeover = false;
        try {
            SharedPreferences sp = context.getSharedPreferences("feature_settings", Context.MODE_PRIVATE);
            String json = sp.getString("settings_json", null);
            if (json != null) {
                JSONObject obj = new JSONObject(json);
                takeover = obj.optBoolean("launcherManagedMcLoginEnabled", false);
            }
        } catch (Throwable ignored) {
        }

        File xalRoot;
        if (takeover) {
            File internalXal = new File(context.getApplicationContext().getFilesDir(), "xal");

            if (!internalXal.exists()) internalXal.mkdirs();
            xalRoot = internalXal;

            String activeMsUserId = null;
            File accountsFile = new File(xalRoot, "Xal.Accounts.json");
            if (accountsFile.exists()) {
                try (FileInputStream fis = new FileInputStream(accountsFile)) {
                    byte[] buf = new byte[(int) Math.min(accountsFile.length(), 1024 * 1024)];
                    int read = fis.read(buf);
                    String body = new String(buf, 0, Math.max(0, read), StandardCharsets.UTF_8);
                    JSONArray arr = new JSONArray(body);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject a = arr.optJSONObject(i);
                        if (a == null) continue;
                        if (a.optBoolean("active", false)) {
                            String msUserId = a.optString("msUserId", null);
                            if (!msUserId.isEmpty()) {
                                activeMsUserId = msUserId;
                                break;
                            }
                        }
                    }
                    if (activeMsUserId == null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject a = arr.optJSONObject(i);
                            if (a == null) continue;
                            String msUserId = a.optString("msUserId", null);
                            if (!msUserId.isEmpty()) {
                                activeMsUserId = msUserId;
                                break;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            if (activeMsUserId != null && !activeMsUserId.isEmpty()) {
                String b64 = Base64.encodeToString(
                        activeMsUserId.getBytes(StandardCharsets.UTF_8),
                       Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP
                );
                File userDir = new File(xalRoot, b64);
                if (!userDir.exists()) {
                    try {
                        userDir.mkdirs();
                    } catch (Exception e) {

                    }
                }
                return userDir.getAbsolutePath();
            }
            return xalRoot.getAbsolutePath();
        }

        return context.getApplicationContext().getFilesDir().getPath();
    }
}
