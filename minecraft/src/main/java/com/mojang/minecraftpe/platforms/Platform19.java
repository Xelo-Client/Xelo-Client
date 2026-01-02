package com.mojang.minecraftpe.platforms;

import android.os.Handler;
import android.view.View;

public class Platform19 extends Platform9 {
    private Runnable decorViewSettings;
    private View decoreView;
    private Handler eventHandler;

    @Override
    public void onVolumePressed() {
    }

    public Platform19(boolean z) {
        if (z) {
            this.eventHandler = new Handler();
        }
    }

    @Override
    public void onAppStart(View view) {
        if (this.eventHandler == null) {
            return;
        }
        this.decoreView = view;
        view.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int i) {
                Platform19.this.eventHandler.postDelayed(Platform19.this.decorViewSettings, 500L);
            }
        });
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Platform19.this.decoreView.setSystemUiVisibility(5894);
            }
        };
        this.decorViewSettings = runnable;
        this.eventHandler.post(runnable);
    }

    @Override
    public void onViewFocusChanged(boolean z) {
        Handler handler = this.eventHandler;
        if (handler == null || !z) {
            return;
        }
        handler.postDelayed(this.decorViewSettings, 500L);
    }
}