package com.mojang.minecraftpe.platforms;

import android.os.Build;

public class Platform21 extends Platform19 {
    public Platform21(boolean z) {
        super(z);
    }
    @Override
    public String getABIS() {
        return Build.SUPPORTED_ABIS.toString();
    }
}