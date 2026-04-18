package com.origin.launcher.Launcher.inbuilt.XeloOverlay.nativemod;

public class PauseScreenNative {

    static {
        System.loadLibrary("xelocore");
    }

    public static native boolean isPauseVisible();

    public static native boolean isHudVisible();
}
