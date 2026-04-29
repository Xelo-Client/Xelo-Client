package com.origin.launcher.Launcher.inbuilt.XeloOverlay.nativemod;

public class XeloCore {

    static {
        System.loadLibrary("xelocore");
    }

    public static native boolean isPauseVisible();

    public static native boolean isInWorld();

    public static native boolean isHudClear();
}
