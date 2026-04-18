package com.origin.launcher.dialogs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.origin.launcher.R;

public class ButtonStyleDialog {

    public interface OnStyleSelectedListener {
        void onSelected(boolean usePng);
    }

    public static final String PREF_NAME = "button_icon_style_prefs";
    public static final String KEY_PREFIX = "use_png_";

    public static void show(Context context, String modId, boolean usePng, OnStyleSelectedListener listener) {
        String title = usePng ? "PNG Icon Mode" : "Native Icon Mode";
        String message = usePng
                ? "PNG based — Uses PNG icons. Texture can be replaced with .xtheme, May become blurry with size changes."
                : "Native — Uses vector drawable icon. Cannot be changed with .xtheme, Quality won't decrease with size changes.";

        int bgColor = ContextCompat.getColor(context, R.color.background);
        int onBgColor = ContextCompat.getColor(context, R.color.onBackground);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.ic_palette)
                .setPositiveButton("Apply", (d, which) -> {
                    setPng(context, modId, usePng);
                    if (listener != null) listener.onSelected(usePng);
                    d.dismiss();
                })
                .setNegativeButton(R.string.dialog_negative_cancel, null)
                .create();

        dialog.show();

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(bgColor);
        bg.setStroke(2, onBgColor);
        bg.setCornerRadius(28f);
        dialog.getWindow().setBackgroundDrawable(bg);

        dialog.getWindow().getDecorView().post(() -> {
            android.widget.TextView titleView = dialog.getWindow().getDecorView()
                    .findViewWithTag("alertTitle");
            applyColorToViews(dialog.getWindow().getDecorView(), onBgColor);
            applyButtonColors(dialog, onBgColor);
        });
    }

    private static void applyColorToViews(android.view.View view, int color) {
        if (view instanceof android.widget.TextView) {
            ((android.widget.TextView) view).setTextColor(color);
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyColorToViews(vg.getChildAt(i), color);
            }
        }
    }

    private static void applyButtonColors(AlertDialog dialog, int color) {
        android.widget.Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        android.widget.Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positive != null) positive.setTextColor(color);
        if (negative != null) negative.setTextColor(color);
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
