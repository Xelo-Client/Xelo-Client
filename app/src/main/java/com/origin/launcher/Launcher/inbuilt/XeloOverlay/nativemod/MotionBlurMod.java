package com.origin.launcher.Launcher.inbuilt.XeloOverlay.nativemod;

public class MotionBlurMod {

    private static boolean loaded = false;

    private static boolean loadLibrary() {
        if (loaded) return true;
        try {
            System.loadLibrary("MotionBlur");
            loaded = true;
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static boolean enable() {
        if (!loadLibrary()) return false;
        setEnabled(true);
        return true;
    }

    public static boolean disable() {
        if (!loadLibrary()) return false;
        setEnabled(false);
        return true;
    }

    public static native void setEnabled(boolean enabled);
}