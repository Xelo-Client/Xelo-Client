package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.view.KeyEvent;
import android.widget.ImageButton;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;

public class CameraPerspectiveOverlay extends BaseOverlayButton {

    public CameraPerspectiveOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.CAMERA_PERSPECTIVE;
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_camera_selector;
    }

    @Override
    protected void onOverlayViewCreated(ImageButton btn) {}

    @Override
    protected void onButtonClick() {
        sendKey(KeyEvent.KEYCODE_F5);
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