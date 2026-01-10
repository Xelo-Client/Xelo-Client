package com.mojang.minecraftpe.platforms;

import android.view.View;

public abstract class Platform {
    public abstract String getABIS();

    public abstract void onAppStart(View view);

    public abstract void onViewFocusChanged(boolean z);

    public abstract void onVolumePressed();

    public static Platform createPlatform(boolean z) {
        return new Platform19(z);
    }
}