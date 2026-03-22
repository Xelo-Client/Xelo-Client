package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.widget.ImageButton;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;

public class AutoSprintOverlay extends BaseOverlayButton {

    private boolean isActive = false;
    private int sprintKey;

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
        return R.drawable.ic_sprint_selector;
    }

    @Override
    protected void onOverlayViewCreated(ImageButton btn) {}

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

    @Override
    public void hide() {
        if (isActive) {
            sendKeyUp(sprintKey);
            isActive = false;
        }
        super.hide();
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