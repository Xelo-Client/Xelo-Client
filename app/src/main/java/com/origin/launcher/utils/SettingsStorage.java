package com.origin.launcher.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class SettingsStorage {
    private static final String SP_NAME = "feature_settings";
    private static final String KEY_SETTINGS_JSON = "settings_json";
    private static final Gson gson = new Gson();

    public static void save(Context context, FeatureSettings settings) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(settings);
        sp.edit().putString(KEY_SETTINGS_JSON, json).apply();
    }

    public static FeatureSettings load(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_SETTINGS_JSON, null);
        if (json == null) {
            return new FeatureSettings();
        }
        try {
            return gson.fromJson(json, FeatureSettings.class);
        } catch (JsonSyntaxException e) {
            return new FeatureSettings();
        }
    }

    public static void clear(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_SETTINGS_JSON).apply();
    }
}