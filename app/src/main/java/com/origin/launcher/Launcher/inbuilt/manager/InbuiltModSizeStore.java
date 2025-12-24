package com.origin.launcher.Launcher.inbuilt.manager;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class InbuiltModSizeStore {

    private static InbuiltModSizeStore instance;

    private static final String PREFS = "inbuilt_mod_sizes";

    private SharedPreferences prefs;
    private final Map<String, Float> scales = new HashMap<>();
    private final Map<String, Integer> sizes = new HashMap<>();
    private final Map<String, Float> posX = new HashMap<>();
    private final Map<String, Float> posY = new HashMap<>();
    private final Map<String, Boolean> locks = new HashMap<>();

    private InbuiltModSizeStore() { }

    public static InbuiltModSizeStore getInstance() {
        if (instance == null) {
            instance = new InbuiltModSizeStore();
        }
        return instance;
    }

    public void init(Context appContext) {
        if (prefs != null) return;

        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> e : all.entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();
            if (value instanceof Float) {
                float f = (Float) value;
                if (key.startsWith("pos_x_")) {
                    posX.put(key.substring("pos_x_".length()), f);
                } else if (key.startsWith("pos_y_")) {
                    posY.put(key.substring("pos_y_".length()), f);
                } else {
                    scales.put(key, f);
                }
            } else if (value instanceof Double) {
                float f = ((Double) value).floatValue();
                if (key.startsWith("pos_x_")) {
                    posX.put(key.substring("pos_x_".length()), f);
                } else if (key.startsWith("pos_y_")) {
                    posY.put(key.substring("pos_y_".length()), f);
                } else {
                    scales.put(key, f);
                }
            } else if (value instanceof Integer) {
                if (key.startsWith("size_")) {
                    sizes.put(key.substring("size_".length()), (Integer) value);
                }
            } else if (value instanceof Boolean) {
                if (key.startsWith("lock_")) {
                    locks.put(key.substring("lock_".length()), (Boolean) value);
                }
            }
        }
    }

    public float getScale(String id) {
        Float v = scales.get(id);
        if (v != null) return v;
        if (prefs != null) {
            float stored = prefs.getFloat(id, 1.0f);
            scales.put(id, stored);
            return stored;
        }
        return 1.0f;
    }

    public void setScale(String id, float scale) {
        scales.put(id, scale);
        if (prefs != null) {
            prefs.edit().putFloat(id, scale).apply();
        }
    }

    public int getSize(String id) {
        Integer v = sizes.get(id);
        if (v != null) return v;
        if (prefs != null && prefs.contains("size_" + id)) {
            int stored = prefs.getInt("size_" + id, 40);
            sizes.put(id, stored);
            return stored;
        }
        return 40;
    }

    public void setSize(String id, int sizeDp) {
        sizes.put(id, sizeDp);
        if (prefs != null) {
            prefs.edit().putInt("size_" + id, sizeDp).apply();
        }
    }

    public void setPositionX(String id, float x) {
        posX.put(id, x);
        if (prefs != null) {
            prefs.edit().putFloat("pos_x_" + id, x).apply();
        }
    }

    public void setPositionY(String id, float y) {
        posY.put(id, y);
        if (prefs != null) {
            prefs.edit().putFloat("pos_y_" + id, y).apply();
        }
    }

    public float getPositionX(String id) {
        Float v = posX.get(id);
        if (v != null) return v;
        if (prefs != null && prefs.contains("pos_x_" + id)) {
            float stored = prefs.getFloat("pos_x_" + id, -1f);
            posX.put(id, stored);
            return stored;
        }
        return -1f;
    }

    public float getPositionY(String id) {
        Float v = posY.get(id);
        if (v != null) return v;
        if (prefs != null && prefs.contains("pos_y_" + id)) {
            float stored = prefs.getFloat("pos_y_" + id, -1f);
            posY.put(id, stored);
            return stored;
        }
        return -1f;
    }

    public boolean isLocked(String id) {
        Boolean v = locks.get(id);
        if (v != null) return v;
        if (prefs != null && prefs.contains("lock_" + id)) {
            boolean stored = prefs.getBoolean("lock_" + id, false);
            locks.put(id, stored);
            return stored;
        }
        return false;
    }

    public void setLocked(String id, boolean locked) {
        locks.put(id, locked);
        if (prefs != null) {
            prefs.edit().putBoolean("lock_" + id, locked).apply();
        }
    }
}