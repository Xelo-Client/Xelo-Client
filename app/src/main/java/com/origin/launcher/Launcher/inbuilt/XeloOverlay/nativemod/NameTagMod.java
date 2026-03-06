package com.origin.launcher.Launcher.inbuilt.XeloOverlay.nativemod;

public class NameTagMod {

    public static boolean patch() {
        if (!InbuiltModsNative.loadLibrary()) return false;
        return patchNametag();
    }

    public static boolean unpatch() {
        if (!InbuiltModsNative.loadLibrary()) return false;
        return unpatchNametag();
    }

    public static native boolean patchNametag();
    public static native boolean unpatchNametag();
}