package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.Launcher.inbuilt.XeloOverlay.nativemod.PauseScreenNative;
import com.origin.launcher.dialogs.ButtonStyleDialog;

public class AutoSprintOverlay extends BaseOverlayButton {

    private boolean isActive = false;
    private int sprintKey;

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

    public AutoSprintOverlay(Activity activity, int sprintKey) {
        super(activity);
        this.sprintKey = sprintKey;
    }

    @Override
    protected String getModId() {
        return ModIds.AUTO_SPRINT;
    }

    @Override
    protected int getIconResource() {
        boolean usePng = ButtonStyleDialog.isUsingPng(activity, ModIds.AUTO_SPRINT);
        return usePng ? R.drawable.ic_sprint_selector : R.drawable.ic_sprint;
    }

    @Override
    protected void onOverlayViewCreated(ImageButton btn) {
        applyIconPadding(btn);
        handler.removeCallbacks(pausePoller);
        lastPauseState = PauseScreenNative.isPauseVisible();
        if (lastPauseState) {
            hideDuringPause();
        }
        handler.post(pausePoller);
    }

    @Override
    protected void onButtonClick() {
        isActive = !isActive;
        if (isActive) {
            sendKeyDown(sprintKey);
            updateButtonState(true);
        } else {
            sendKeyUp(sprintKey);
            updateButtonState(false);
        }
    }

    private void applyIconPadding(ImageButton btn) {
        boolean usePng = ButtonStyleDialog.isUsingPng(activity, ModIds.AUTO_SPRINT);
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
                boolean usePng = ButtonStyleDialog.isUsingPng(activity, ModIds.AUTO_SPRINT);
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

    private void hideDuringPause() {
        if (overlayView == null) return;
        ImageButton btn = overlayView.findViewById(R.id.mod_overlay_button);
        if (btn != null) {
            activity.runOnUiThread(() -> {
                btn.setVisibility(View.GONE);
                if (isActive) {
                    sendKeyUp(sprintKey);
                    isActive = false;
                    updateButtonState(false);
                }
            });
        }
    }

    private void showAfterPause() {
        if (overlayView == null) return;
        ImageButton btn = overlayView.findViewById(R.id.mod_overlay_button);
        if (btn != null) {
            activity.runOnUiThread(() -> btn.setVisibility(View.VISIBLE));
        }
    }

    @Override
    public void hide() {
        if (isActive) {
            sendKeyUp(sprintKey);
            isActive = false;
        }
        super.hide();
    }

    public void destroy() {
        handler.removeCallbacks(pausePoller);
        hide();
    }

    private int tickCount = 0;

    @Override
    public void tick() {
        if (!isActive) return;

        tickCount++;
        if (tickCount >= 20) {
            tickCount = 0;
            sendKeyDown(sprintKey);
        }
    }
}