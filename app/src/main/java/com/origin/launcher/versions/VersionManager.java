package com.origin.launcher.versions;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.origin.launcher.R;
import com.origin.launcher.activity.MainActivity;
import com.origin.launcher.utils.ApkUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class VersionManager {
    private static final String PREFS_NAME = "version_manager";
    private static final String KEY_SELECTED_TYPE = "selected_type";
    private static final String KEY_SELECTED_PACKAGE = "selected_package";
    private static final int BUFFER_SIZE = 8192;

    private static VersionManager instance;
    private final Context context;
    private final List<GameVersion> installedVersions = new ArrayList<>();
    private GameVersion selectedVersion;
    private final SharedPreferences prefs;

    public static VersionManager get(Context ctx) {
        if (instance == null) instance = new VersionManager(ctx.getApplicationContext());
        return instance;
    }

    /**
     * Returns selected version's mods directory (if available).
     */
    public static String getSelectedModsDir(Context ctx) {
        GameVersion v = get(ctx).getSelectedVersion();
        if (v == null || v.modsDir == null) return null;
        return v.modsDir.getAbsolutePath();
    }

    private VersionManager(Context ctx) {
        this.context = ctx;
        this.prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadAllVersions();
    }

    private boolean isMinecraftPackage(String packageName) {
        return packageName.equals("com.mojang.minecraftpe") || packageName.startsWith("com.mojang.");
    }

    private String readFileToString(File file) {
        if (file == null || !file.exists()) return "";
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int len = in.read(data);
            return new String(data, 0, len, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean writeStringToFile(File file, String data) {
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write(data.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String inferAbiFromNativeLibDir(String nativeLibDir, GameVersion version) {
        // For installed packages we can use nativeLibraryDir, for others return unknown
        if (nativeLibDir == null) return "unknown";
        if (nativeLibDir.contains("arm64")) return "arm64-v8a";
        if (nativeLibDir.contains("armeabi")) return "armeabi-v7a";
        if (nativeLibDir.contains("x86_64")) return "x86_64";
        if (nativeLibDir.contains("x86")) return "x86";
        return "unknown";
    }

    private String getApkVersionName(File apkFile) {
        // APK parsing removed — keep stub returning "unknown" in case callers exist elsewhere
        return "unknown";
    }
    
    public void loadAllVersions() {
        installedVersions.clear();

        PackageManager pm = context.getPackageManager();
        List<PackageInfo> pkgs = pm.getInstalledPackages(0);

        for (PackageInfo pi : pkgs) {
            if (!isMinecraftPackage(pi.packageName)) continue;

            File versionDir = getVersionDirForPackage(pi.packageName);
            if (!versionDir.exists()) versionDir.mkdirs();

            File gamesDir = new File(versionDir, "games/com.mojang");
            if (!gamesDir.exists()) gamesDir.mkdirs();

            String displayName = pi.applicationInfo.loadLabel(pm) + " (" + pi.versionName + ")";

            GameVersion gv = new GameVersion(
                    pi.packageName + "_" + pi.versionCode,
                    displayName,
                    pi.versionName,
                    versionDir,
                    true,
                    pi.packageName,
                    inferAbiFromNativeLibDir(pi.applicationInfo.nativeLibraryDir, null)
            );

            installedVersions.add(gv);
        }

        restoreSelectedVersion();
    }

    @NonNull
    private File getVersionDirForPackage(String packageName) {
        return new File(Environment.getExternalStorageDirectory(),
                "games/xelo_client/minecraft/" + packageName);
    }

    private void restoreSelectedVersion() {
        String type = prefs.getString(KEY_SELECTED_TYPE, null);
        if (type != null && type.equals("installed")) {
            String pkg = prefs.getString(KEY_SELECTED_PACKAGE, null);
            for (GameVersion gv : installedVersions) {
                if (gv.packageName != null && gv.packageName.equals(pkg)) {
                    selectedVersion = gv;
                    break;
                }
            }
        }
    }

    public List<GameVersion> getInstalledVersions() {
        return installedVersions;
    }

    public GameVersion getSelectedVersion() {
        if (selectedVersion != null) return selectedVersion;
        if (!installedVersions.isEmpty()) {
            selectVersion(installedVersions.get(0));
            return installedVersions.get(0);
        }
        return null;
    }

    public void selectVersion(GameVersion version) {
        this.selectedVersion = version;
        SharedPreferences.Editor editor = prefs.edit();
        if (version != null && version.isInstalled) {
            editor.putString(KEY_SELECTED_TYPE, "installed");
            editor.putString(KEY_SELECTED_PACKAGE, version.packageName);
        } else {
            editor.remove(KEY_SELECTED_TYPE);
            editor.remove(KEY_SELECTED_PACKAGE);
        }
        editor.apply();
    }

    public void reload() {
        loadAllVersions();
    }
}