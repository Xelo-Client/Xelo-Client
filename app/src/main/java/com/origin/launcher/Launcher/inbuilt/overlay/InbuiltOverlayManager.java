package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.view.KeyEvent;

import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModSizeStore;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.Launcher.inbuilt.overlay.FpsDisplayOverlay;
import com.origin.launcher.Launcher.inbuilt.overlay.CpsDisplayOverlay;

import java.util.ArrayList;
import java.util.List;

public class InbuiltOverlayManager {
    private static volatile InbuiltOverlayManager instance;
    private final Activity activity;
    private final List<BaseOverlayButton> overlays = new ArrayList<>();
    private int nextY = 150;
    private static final int SPACING = 70;
    private static final int START_X = 50;
    private FpsDisplayOverlay fpsDisplayOverlay;
    private CpsDisplayOverlay cpsDisplayOverlay;
    private ZoomOverlay zoomOverlay;

    public InbuiltOverlayManager(Activity activity) {
        this.activity = activity;
        instance = this;
    }
    
    public boolean handleKeyEvent(int keyCode, int action) {
    boolean zoomEnabled = InbuiltModManager.getInstance(activity).isModAdded(ModIds.ZOOM);
    
    if (!zoomEnabled || zoomOverlay == null) {
        return false;
    }

    if (keyCode == android.view.KeyEvent.KEYCODE_C) {
        if (action == android.view.KeyEvent.ACTION_DOWN) {
            zoomOverlay.onKeyDown();
            return true;
        } else if (action == android.view.KeyEvent.ACTION_UP) {
            zoomOverlay.onKeyUp();
            return true;
        }
    }
    return false;
}

public boolean handleScrollEvent(float scrollDelta) {
    if (zoomOverlay != null && zoomOverlay.isZooming()) {
        zoomOverlay.onScroll(scrollDelta);
        return true;
    }
    return false;
}

public ZoomOverlay getZoomOverlay() {
    return zoomOverlay;
}

    public static InbuiltOverlayManager getInstance() {
        return instance;
    }

    private int[] getStartPosition(String modId, int defaultX, int defaultY) {
        InbuiltModSizeStore store = InbuiltModSizeStore.getInstance();
        float savedX = store.getPositionX(modId);
        float savedY = store.getPositionY(modId);
        int x = savedX >= 0f ? (int) savedX : defaultX;
        int y = savedY >= 0f ? (int) savedY : defaultY;
        return new int[]{x, y};
    }

    public void showEnabledOverlays() {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        nextY = 150;

        if (manager.isModAdded(ModIds.QUICK_DROP)) {
            int[] pos = getStartPosition(ModIds.QUICK_DROP, START_X, nextY);
            QuickDropOverlay overlay = new QuickDropOverlay(activity);
            overlay.show(pos[0], pos[1]);
            overlays.add(overlay);
            nextY += SPACING;
        }
        if (manager.isModAdded(ModIds.CAMERA_PERSPECTIVE)) {
            int[] pos = getStartPosition(ModIds.CAMERA_PERSPECTIVE, START_X, nextY);
            CameraPerspectiveOverlay overlay = new CameraPerspectiveOverlay(activity);
            overlay.show(pos[0], pos[1]);
            overlays.add(overlay);
            nextY += SPACING;
        }
        if (manager.isModAdded(ModIds.TOGGLE_HUD)) {
            int[] pos = getStartPosition(ModIds.TOGGLE_HUD, START_X, nextY);
            ToggleHudOverlay overlay = new ToggleHudOverlay(activity);
            overlay.show(pos[0], pos[1]);
            overlays.add(overlay);
            nextY += SPACING;
        }
        if (manager.isModAdded(ModIds.AUTO_SPRINT)) {
            int[] pos = getStartPosition(ModIds.AUTO_SPRINT, START_X, nextY);
            AutoSprintOverlay overlay = new AutoSprintOverlay(activity, manager.getAutoSprintKey());
            overlay.show(pos[0], pos[1]);
            overlays.add(overlay);
            nextY += SPACING;
        }
        if (manager.isModAdded(ModIds.ZOOM)) {
    zoomOverlay = new ZoomOverlay(activity);
    zoomOverlay.initializeForKeyboard();
    int[] pos = getStartPosition(ModIds.ZOOM, START_X, nextY);
    zoomOverlay.show(pos[0], pos[1]);
    overlays.add(zoomOverlay);
    nextY += SPACING;
        }
        if (manager.isModAdded(ModIds.FPS_DISPLAY)) {
            fpsDisplayOverlay = new FpsDisplayOverlay(activity);
            fpsDisplayOverlay.show(START_X, nextY);
            nextY += SPACING;
        }
        if (manager.isModAdded(ModIds.CPS_DISPLAY)) {
            cpsDisplayOverlay = new CpsDisplayOverlay(activity);
            cpsDisplayOverlay.show(START_X, nextY);
            nextY += SPACING;
        }
    }

    public void hideAllOverlays() {
        for (BaseOverlayButton overlay : overlays) {
            overlay.hide();
        }
        overlays.clear();
        instance = null;
    }
}