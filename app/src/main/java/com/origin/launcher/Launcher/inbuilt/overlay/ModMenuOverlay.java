package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
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
        return R.mipmap.ic_launcher;
    }

    @Override
    protected void onButtonClick() {
        if (dialog == null || !dialog.isShowing()) {
            dialog = new ModMenuDialog(activity);
            dialog.show();
        } else {
            dialog.hide();
        }
    }
}