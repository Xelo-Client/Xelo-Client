package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.Launcher.inbuilt.XeloOverlay.nativemod.XeloCore;
import com.origin.launcher.dialogs.ButtonStyleDialog;

public class QuickDropOverlay extends BaseOverlayButton {

    private static final long HOLD_THRESHOLD_MS = 300;

    private boolean isHolding = false;
    private boolean lastInWorldState = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable holdRunnable = () -> {
        isHolding = true;
        sendKeyDown(KeyEvent.KEYCODE_CTRL_LEFT);
        sendKeyDown(KeyEvent.KEYCODE_Q);
        updateButtonState(true);
    };

    private final Runnable worldPoller = new Runnable() {
        @Override
        public void run() {
            boolean inWorld = XeloCore.isInWorld();
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

    public QuickDropOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.QUICK_DROP;
    }

    @Override
    protected int getIconResource() {
        boolean usePng = ButtonStyleDialog.isUsingPng(activity, ModIds.QUICK_DROP);
        return usePng ? R.drawable.ic_quick_drop_selector : R.drawable.ic_quick_drop;
    }

    @Override
    protected void onOverlayViewCreated(ImageButton btn) {
        applyIconPadding(btn);
        handler.removeCallbacks(worldPoller);
        lastInWorldState = XeloCore.isInWorld();
        if (!lastInWorldState) {
            hideOutOfWorld();
        }
        handler.post(worldPoller);
    }

    @Override
    protected void onTouchDown(MotionEvent event) {
        isHolding = false;
        handler.postDelayed(holdRunnable, HOLD_THRESHOLD_MS);
    }

    @Override
    protected void onTouchUp(MotionEvent event, boolean wasDragging) {
        handler.removeCallbacks(holdRunnable);
        if (wasDragging) {
            if (isHolding) {
                sendKeyUp(KeyEvent.KEYCODE_Q);
                sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT);
                updateButtonState(false);
            }
            isHolding = false;
            return;
        }
        if (isHolding) {
            sendKeyUp(KeyEvent.KEYCODE_Q);
            sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT);
            updateButtonState(false);
            isHolding = false;
        }
    }

    @Override
    protected void onTouchCancel(MotionEvent event) {
        handler.removeCallbacks(holdRunnable);
        if (isHolding) {
            sendKeyUp(KeyEvent.KEYCODE_Q);
            sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT);
            updateButtonState(false);
        }
        isHolding = false;
    }

    @Override
    protected void onDragStarted() {
        handler.removeCallbacks(holdRunnable);
        if (isHolding) {
            sendKeyUp(KeyEvent.KEYCODE_Q);
            sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT);
            updateButtonState(false);
        }
        isHolding = false;
    }

    @Override
    protected void onButtonClick() {
        sendKey(KeyEvent.KEYCODE_Q);
        updateButtonState(true);
        overlayView.postDelayed(() -> updateButtonState(false), 150);
    }

    private void applyIconPadding(ImageButton btn) {
        boolean usePng = ButtonStyleDialog.isUsingPng(activity, ModIds.QUICK_DROP);
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
                boolean usePng = ButtonStyleDialog.isUsingPng(activity, ModIds.QUICK_DROP);
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
            activity.runOnUiThread(() -> {
                btn.setVisibility(View.GONE);
                handler.removeCallbacks(holdRunnable);
                if (isHolding) {
                    sendKeyUp(KeyEvent.KEYCODE_Q);
                    sendKeyUp(KeyEvent.KEYCODE_CTRL_LEFT);
                    updateButtonState(false);
                    isHolding = false;
                }
            });
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
        handler.removeCallbacks(holdRunnable);
        handler.removeCallbacks(worldPoller);
        hide();
    }
}
