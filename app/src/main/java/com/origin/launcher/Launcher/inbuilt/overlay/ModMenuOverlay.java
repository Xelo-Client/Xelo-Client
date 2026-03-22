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
    protected void onButtonClick() {
    if (dialog == null || !dialog.isShowing()) {
        if (dialog != null) dialog.hide();
        dialog = new ModMenuDialog(activity);
        dialog.show();
    } else {
        dialog.hide();
        }
    }
}