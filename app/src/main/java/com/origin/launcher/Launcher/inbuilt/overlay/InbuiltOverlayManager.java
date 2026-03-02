package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.KeyEvent;

import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModSizeStore;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.Launcher.inbuilt.overlay.AutoSprintOverlay;
import com.origin.launcher.Launcher.inbuilt.overlay.CameraPerspectiveOverlay;
import com.origin.launcher.Launcher.inbuilt.overlay.CpsDisplayOverlay;
import com.origin.launcher.Launcher.inbuilt.overlay.FpsDisplayOverlay;
import com.origin.launcher.Launcher.inbuilt.overlay.ModMenuOverlay;
import com.origin.launcher.Launcher.inbuilt.overlay.QuickDropOverlay;
import com.origin.launcher.Launcher.inbuilt.overlay.ToggleHudOverlay;
import com.origin.launcher.Launcher.inbuilt.overlay.ZoomOverlay;

import java.util.ArrayList;
import java.util.List;

public class InbuiltOverlayManager {
    private static volatile InbuiltOverlayManager instance;
    private final Activity activity;
    private final List<BaseOverlayButton> overlays = new ArrayList<>();
    private final InbuiltModManager modManager;
    private static final int SPACING = 70;
    private static final int START_X = 50;
    private QuickDropOverlay quickDropOverlay;
    private CameraPerspectiveOverlay cameraPerspectiveOverlay;
    private ToggleHudOverlay toggleHudOverlay;
    private AutoSprintOverlay autoSprintOverlay;
    private ZoomOverlay zoomOverlay;
    private FpsDisplayOverlay fpsDisplayOverlay;
    private CpsDisplayOverlay cpsDisplayOverlay;

    public InbuiltOverlayManager(Activity activity) {
        this.activity = activity;
        this.modManager = InbuiltModManager.getInstance(activity);
        instance = this;
    }

    public boolean handleKeyEvent(int keyCode, int action) {
        boolean zoomEnabled = modManager.isModAdded(ModIds.ZOOM);
        if (!zoomEnabled || zoomOverlay == null) {
            return false;
        }
        int zoomKeybind = modManager.getZoomKeybind();
        if (keyCode == zoomKeybind) {
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

    public boolean handleTouchEvent(MotionEvent event) {
        if (cpsDisplayOverlay != null) {
            return cpsDisplayOverlay.handleTouchEvent(event);
        }
        return false;
    }

    public boolean handleMouseEvent(MotionEvent event) {
        if (cpsDisplayOverlay != null) {
            return cpsDisplayOverlay.handleMouseEvent(event);
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
        for (BaseOverlayButton overlay : new ArrayList<>(overlays)) {
            overlay.hide();
        }
        overlays.clear();
        int nextY = 150;

        ModMenuOverlay modMenu = new ModMenuOverlay(activity);
        int[] menuPos = getStartPosition("mod_menu", START_X, 20);
        modMenu.show(menuPos[0], menuPos[1]);
        overlays.add(modMenu);
        nextY += SPACING;

        if (modManager.isModAdded(ModIds.QUICK_DROP)) {
            int[] pos = getStartPosition(ModIds.QUICK_DROP, START_X, nextY);
            if (quickDropOverlay == null) quickDropOverlay = new QuickDropOverlay(activity);
            quickDropOverlay.show(pos[0], pos[1]);
            overlays.add(quickDropOverlay);
            nextY += SPACING;
        }
        if (modManager.isModAdded(ModIds.CAMERA_PERSPECTIVE)) {
            int[] pos = getStartPosition(ModIds.CAMERA_PERSPECTIVE, START_X, nextY);
            if (cameraPerspectiveOverlay == null) cameraPerspectiveOverlay = new CameraPerspectiveOverlay(activity);
            cameraPerspectiveOverlay.show(pos[0], pos[1]);
            overlays.add(cameraPerspectiveOverlay);
            nextY += SPACING;
        }
        if (modManager.isModAdded(ModIds.TOGGLE_HUD)) {
            int[] pos = getStartPosition(ModIds.TOGGLE_HUD, START_X, nextY);
            if (toggleHudOverlay == null) toggleHudOverlay = new ToggleHudOverlay(activity);
            toggleHudOverlay.show(pos[0], pos[1]);
            overlays.add(toggleHudOverlay);
            nextY += SPACING;
        }
        if (modManager.isModAdded(ModIds.AUTO_SPRINT)) {
            int[] pos = getStartPosition(ModIds.AUTO_SPRINT, START_X, nextY);
            if (autoSprintOverlay == null) autoSprintOverlay = new AutoSprintOverlay(activity, modManager.getAutoSprintKey());
            autoSprintOverlay.show(pos[0], pos[1]);
            overlays.add(autoSprintOverlay);
            nextY += SPACING;
        }
        if (modManager.isModAdded(ModIds.ZOOM)) {
            if (zoomOverlay == null) zoomOverlay = new ZoomOverlay(activity);
            zoomOverlay.initializeForKeyboard();
            int[] pos = getStartPosition(ModIds.ZOOM, START_X, nextY);
            zoomOverlay.show(pos[0], pos[1]);
            overlays.add(zoomOverlay);
            nextY += SPACING;
        }
        if (modManager.isModAdded(ModIds.FPS_DISPLAY)) {
            int[] pos = getStartPosition(ModIds.FPS_DISPLAY, START_X, nextY);
            if (fpsDisplayOverlay == null) fpsDisplayOverlay = new FpsDisplayOverlay(activity);
            fpsDisplayOverlay.show(pos[0], pos[1]);
            nextY += SPACING;
        }
        if (modManager.isModAdded(ModIds.CPS_DISPLAY)) {
            int[] pos = getStartPosition(ModIds.CPS_DISPLAY, START_X, nextY);
            if (cpsDisplayOverlay == null) cpsDisplayOverlay = new CpsDisplayOverlay(activity);
            cpsDisplayOverlay.show(pos[0], pos[1]);
            nextY += SPACING;
        }
    }

    public void hideAllOverlays() {
        for (BaseOverlayButton overlay : new ArrayList<>(overlays)) {
            overlay.hide();
        }
        overlays.clear();
    }

    public void tick() {
        for (BaseOverlayButton overlay : overlays) {
            overlay.tick();
        }
    }

    public void toggleMod(String modId) {
        if (modManager.isModAdded(modId)) {
            modManager.removeMod(modId);
        } else {
            modManager.addMod(modId);
        }
        showEnabledOverlays();
    }

    public void enableAllMods() {
        String[] allIds = {
            ModIds.QUICK_DROP, ModIds.CAMERA_PERSPECTIVE, ModIds.TOGGLE_HUD,
            ModIds.AUTO_SPRINT, ModIds.ZOOM, ModIds.FPS_DISPLAY, ModIds.CPS_DISPLAY
        };
        for (String id : allIds) {
            modManager.addMod(id);
        }
        showEnabledOverlays();
    }

    public void disableAllMods() {
        String[] allIds = {
            ModIds.QUICK_DROP, ModIds.CAMERA_PERSPECTIVE, ModIds.TOGGLE_HUD,
            ModIds.AUTO_SPRINT, ModIds.ZOOM, ModIds.FPS_DISPLAY, ModIds.CPS_DISPLAY
        };
        for (String id : allIds) {
            modManager.removeMod(id);
        }
        showEnabledOverlays();
    }
}