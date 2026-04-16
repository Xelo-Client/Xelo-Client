package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.Launcher.inbuilt.XeloOverlay.nativemod.PauseScreenNative;
import com.origin.launcher.dialogs.ButtonStyleDialog;

public class ToggleHudOverlay extends BaseOverlayButton {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean lastPauseState = false;

    private final Runnable pausePoller = new Runnable() {
        @Override
        public void run() {
            boolean paused = PauseScreenNative.isPauseVisible();
            if (paused != lastPauseState) {
                lastPauseState = paused;
                if (paused) {
                    hideDuringPause();
                } else {
                    showAfterPause();
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
        handler.removeCallbacks(pausePoller);
        handler.post(pausePoller);
    }

    @Override
    protected void onButtonClick() {
        sendKey(KeyEvent.KEYCODE_F1);
        updateButtonState(true);
        overlayView.postDelayed(() -> updateButtonState(false), 150);
    }

    private void updateButtonState(boolean active) {
        if (overlayView != null) {
            ImageButton btn = overlayView.findViewById(R.id.mod_overlay_button);
            if (btn != null) {
                btn.setActivated(active);
                btn.setAlpha(getButtonAlpha() * (active ? 1.1f : 1.0f));
                btn.setBackgroundResource(
                        active ? R.drawable.bg_overlay_button_active
                               : R.drawable.bg_overlay_button
                );
            }
        }
    }

    private void hideDuringPause() {
        if (overlayView == null) return;
        ImageButton btn = overlayView.findViewById(R.id.mod_overlay_button);
        if (btn != null) {
            activity.runOnUiThread(() -> btn.setVisibility(View.GONE));
        }
    }

    private void showAfterPause() {
        if (overlayView == null) return;
        ImageButton btn = overlayView.findViewById(R.id.mod_overlay_button);
        if (btn != null) {
            activity.runOnUiThread(() -> btn.setVisibility(View.VISIBLE));
        }
    }

    public void destroy() {
        handler.removeCallbacks(pausePoller);
        hide();
    }
}