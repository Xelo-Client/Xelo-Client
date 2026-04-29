package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.Launcher.inbuilt.XeloOverlay.nativemod.XeloCore;
import com.origin.launcher.dialogs.ButtonStyleDialog;

public class ToggleHudOverlay extends BaseOverlayButton {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean lastInWorldState = false;

    private final Runnable worldPoller = new Runnable() {
        @Override
        public void run() {
            boolean inWorld = XeloCore.isHudClear();
            if (inWorld != lastInWorldState) {
                lastInWorldState = inWorld;
                if (inWorld) {
                    showInWorld();
                } else {
                    hideOutOfWorld();
                }
            }
            handler.postDelayed(this, 50);
        }
    };

    public ToggleHudOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.TOGGLE_HUD;
    }

    @Override
    protected int getIconResource() {
        boolean usePng = ButtonStyleDialog.isUsingPng(activity, ModIds.TOGGLE_HUD);
        return usePng ? R.drawable.ic_hud_selector : R.drawable.ic_hud;
    }

    @Override
    protected void onOverlayViewCreated(ImageButton btn) {
        applyIconPadding(btn);
        handler.removeCallbacks(worldPoller);
        lastInWorldState = XeloCore.isHudClear();
        if (!lastInWorldState) {
            hideOutOfWorld();
        }
        handler.post(worldPoller);
    }

    @Override
    protected void onButtonClick() {
        sendKey(KeyEvent.KEYCODE_F1);
        updateButtonState(true);
        overlayView.postDelayed(() -> updateButtonState(false), 150);
    }

    private void applyIconPadding(ImageButton btn) {
        boolean usePng = ButtonStyleDialog.isUsingPng(activity, ModIds.TOGGLE_HUD);
        int p = usePng ? 0 : dpToPx(6);
        btn.setPadding(p, p, p, p);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }

    private void updateButtonState(boolean active) {
        if (overlayView != null) {
            ImageButton btn = overlayView.findViewById(R.id.mod_overlay_button);
            if (btn != null) {
                boolean usePng = ButtonStyleDialog.isUsingPng(activity, ModIds.TOGGLE_HUD);
                btn.setActivated(active);
                btn.setAlpha(getButtonAlpha() * (active ? 1.1f : 1.0f));
                if (usePng) {
                    btn.setBackgroundResource(R.drawable.bg_overlay_button_png);
                } else {
                    btn.setBackgroundResource(active ? R.drawable.bg_overlay_button_active : R.drawable.bg_overlay_button);
                }
            }
        }
    }

    private void hideOutOfWorld() {
        if (overlayView == null) return;
        ImageButton btn = overlayView.findViewById(R.id.mod_overlay_button);
        if (btn != null) {
            activity.runOnUiThread(() -> btn.setVisibility(View.GONE));
        }
    }

    private void showInWorld() {
        if (overlayView == null) return;
        ImageButton btn = overlayView.findViewById(R.id.mod_overlay_button);
        if (btn != null) {
            activity.runOnUiThread(() -> btn.setVisibility(View.VISIBLE));
        }
    }

    public void destroy() {
        handler.removeCallbacks(worldPoller);
        hide();
    }
}
