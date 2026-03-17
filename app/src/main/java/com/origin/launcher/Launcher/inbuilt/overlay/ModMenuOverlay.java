package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import com.origin.launcher.ModMenuDialog;
import com.origin.launcher.R;

public class ModMenuOverlay extends BaseOverlayButton {

    private ModMenuDialog dialog;

    public ModMenuOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return "mod_menu";
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_modmenu;
    }

    @Override
    protected void onOverlayViewCreated(ImageButton btn) {
        btn.setBackgroundResource(R.drawable.round_button_bg);
    }

    @Override
    @Override
    protected void onButtonClick() {
    if (dialog == null || !dialog.isShowing()) {
        if (dialog != null) dialog.hide();
        dialog = new ModMenuDialog(activity);
        dialog.show();
        animateShow();
    } else {
        dialog.hide();
        animateHide();
        }
    }
    
    private void animateShow() {
        if (overlayView == null) return;
        overlayView.setAlpha(0.5f);
        overlayView.setScaleX(0.8f);
        overlayView.setScaleY(0.8f);
        Animation popUp = AnimationUtils.loadAnimation(activity, R.anim.pop_up);
        overlayView.startAnimation(popUp);
    }
    
    private void animateHide() {
        if (overlayView == null) return;
        Animation popDown = AnimationUtils.loadAnimation(activity, R.anim.pop_down);
        popDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        overlayView.startAnimation(popDown);
    }
}