package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import com.origin.launcher.Launcher.inbuilt.XeloOverlay.nativemod.PauseScreenNative;
import com.origin.launcher.dialogs.ModMenuDialog;
import com.origin.launcher.R;

public class ModMenuOverlay extends BaseOverlayButton {

    private ModMenuDialog dialog;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean lastPauseState = false;

    private final Runnable pausePoller = new Runnable() {
        @Override
        public void run() {
            boolean paused = PauseScreenNative.isPauseVisible();
            if (paused != lastPauseState) {
                lastPauseState = paused;
                if (paused) {
                    showOverlay();
                } else {
                    hideOverlay();
                }
            }
            handler.postDelayed(this, 100);
        }
    };

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
        hideOverlay();
        handler.post(pausePoller);
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

    private void showOverlay() {
        ImageButton btn = getOverlayButton();
        if (btn != null) {
            activity.runOnUiThread(() -> btn.setVisibility(android.view.View.VISIBLE));
        }
    }

    private void hideOverlay() {
        ImageButton btn = getOverlayButton();
        if (btn != null) {
            activity.runOnUiThread(() -> {
                btn.setVisibility(android.view.View.GONE);
                if (dialog != null && dialog.isShowing()) {
                    dialog.hide();
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(pausePoller);
        if (dialog != null && dialog.isShowing()) {
            dialog.hide();
        }
        super.onDestroy();
    }
}
