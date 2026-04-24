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

public class HotbarNineOverlay extends BaseOverlayButton {

    private boolean lastInWorldState = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

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

    public HotbarNineOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.HOTBAR_NINE;
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_hotbar_nine_selector;
    }

    @Override
    protected void onOverlayViewCreated(ImageButton btn) {
        handler.removeCallbacks(worldPoller);
        lastInWorldState = XeloCore.isInWorld();
        if (!lastInWorldState) {
            hideOutOfWorld();
        }
        handler.post(worldPoller);
    }

    @Override
    protected void onButtonClick() {
        sendKey(KeyEvent.KEYCODE_9);
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
