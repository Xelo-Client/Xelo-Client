package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.ImageButton;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;

public class QuickDropOverlay extends BaseOverlayButton {

    private static final long HOLD_THRESHOLD_MS = 300;

    private boolean isHolding = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable holdRunnable = () -> {
        isHolding = true;
        sendKeyDown(KeyEvent.KEYCODE_CTRL_LEFT);
        sendKeyDown(KeyEvent.KEYCODE_Q);
        updateButtonState(true);
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
        return R.drawable.ic_quick_drop_selector;
    }

    @Override
    protected void onOverlayViewCreated(ImageButton btn) {}

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
}
