package com.origin.launcher.dialogs;

import android.content.Context;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.origin.launcher.R;

public class ButtonStyleDialog {

    public interface OnStyleSelectedListener {
        void onSelected(boolean usePng);
    }

    public static final String PREF_NAME = "button_icon_style_prefs";
    public static final String KEY_PREFIX = "use_png_";

    public static void show(Context context, String modId, OnStyleSelectedListener listener) {
        boolean currentlyPng = isUsingPng(context, modId);

        String[] options = {
            "Native  —  Uses vector drawable icon. Cannot be changed with .xtheme And Quality won't decrease with size changes",
            "PNG based  —  Uses PNG icons. Texture can be replaced with .xtheme And may become blurry with size changes"
        };

        int[] selectedIndex = {currentlyPng ? 1 : 0};

        new MaterialAlertDialogBuilder(context, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle("Button Icon Style")
                .setIcon(R.drawable.ic_palette)
                .setSingleChoiceItems(options, selectedIndex[0], (dialog, which) -> {
                    selectedIndex[0] = which;
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    boolean usePng = selectedIndex[0] == 1;
                    setPng(context, modId, usePng);
                    if (listener != null) listener.onSelected(usePng);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_negative_cancel, null)
                .show();
    }

    public static boolean isUsingPng(Context context, String modId) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PREFIX + modId, false);
    }

    public static void setPng(Context context, String modId, boolean usePng) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PREFIX + modId, usePng)
                .apply();
    }
}
