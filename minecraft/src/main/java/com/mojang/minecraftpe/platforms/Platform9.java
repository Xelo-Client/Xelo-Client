package com.mojang.minecraftpe.platforms;

import android.os.Build;
import android.view.View;

public class Platform9 extends Platform {
    @Override
    public void onAppStart(View view) {
    }

    @Override
    public void onViewFocusChanged(boolean z) {
    }

    @Override
    public void onVolumePressed() {
    }

    @Override
    public String getABIS() {
        return Build.CPU_ABI;
    }
}