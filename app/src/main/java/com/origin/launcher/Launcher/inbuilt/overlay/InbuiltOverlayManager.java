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
import com.origin.launcher.Launcher.inbuilt.XeloOverlay.nativemod.NameTagMod;

import java.util.ArrayList;
import java.util.List;

public class InbuiltOverlayManager {
    private static volatile InbuiltOverlayManager instance;
    private final Activity activity;
    private final List<Object> overlays = new ArrayList<>();
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
    private ModMenuOverlay modMenuOverlay;

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
        for (Object overlay : new ArrayList<>(overlays)) {
            if (overlay instanceof BaseOverlayButton) {
                ((BaseOverlayButton) overlay).hide();
            } else {
                try {
                    overlay.getClass().getMethod("hide").invoke(overlay);
                } catch (Exception ignored) {}
            }
        }
        overlays.clear();
        int nextY = 150;

        if (modMenuOverlay == null) modMenuOverlay = new ModMenuOverlay(activity);
        int[] menuPos = getStartPosition("mod_menu", START_X, 10);
        modMenuOverlay.show(menuPos[0], menuPos[1]);
        overlays.add(modMenuOverlay);
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
            overlays.add(fpsDisplayOverlay);
            nextY += SPACING;
        }
        if (modManager.isModAdded(ModIds.CPS_DISPLAY)) {
            int[] pos = getStartPosition(ModIds.CPS_DISPLAY, START_X, nextY);
            if (cpsDisplayOverlay == null) cpsDisplayOverlay = new CpsDisplayOverlay(activity);
            cpsDisplayOverlay.show(pos[0], pos[1]);
            overlays.add(cpsDisplayOverlay);
            nextY += SPACING;
        }
    }

    public void hideAllOverlays() {
        for (Object overlay : new ArrayList<>(overlays)) {
            if (overlay instanceof BaseOverlayButton) {
                ((BaseOverlayButton) overlay).hide();
            } else {
                try {
                    overlay.getClass().getMethod("hide").invoke(overlay);
                } catch (Exception ignored) {}
            }
        }
        overlays.clear();
    }

    public void tick() {
        for (Object overlay : overlays) {
            if (overlay instanceof BaseOverlayButton) {
                ((BaseOverlayButton) overlay).tick();
            }
        }
    }

    public void toggleMod(String modId) {
        boolean targetEnabled = !modManager.isModAdded(modId);
        if (targetEnabled) {
            modManager.addMod(modId);
            modManager.applyAllPatches();
        } else {
            modManager.removeAllPatches();
            modManager.removeMod(modId);
        }
        showEnabledOverlays();
    }

    public void refreshPositions() {
        showEnabledOverlays();
    }

    public void hideForCustomize() {
        hideAllOverlays();
    }

    public void showAfterCustomize() {
        showEnabledOverlays();
    }

    public void updatePosition(String modId, float x, float y) {
        InbuiltModSizeStore.getInstance().setPositionX(modId, x);
        InbuiltModSizeStore.getInstance().setPositionY(modId, y);
        refreshPositions();
    }

    public void enableAllMods() {
        String[] allIds = {
            ModIds.QUICK_DROP, ModIds.CAMERA_PERSPECTIVE, ModIds.TOGGLE_HUD,
            ModIds.AUTO_SPRINT, ModIds.ZOOM, ModIds.FPS_DISPLAY, ModIds.CPS_DISPLAY,
            ModIds.THIRD_PERSON_NAMETAG,
        };
        for (String id : allIds) {
            modManager.addMod(id);
        }
        modManager.applyAllPatches();
        showEnabledOverlays();
    }

    public void disableAllMods() {
        modManager.removeAllPatches();
        String[] allIds = {
            ModIds.QUICK_DROP, ModIds.CAMERA_PERSPECTIVE, ModIds.TOGGLE_HUD,
            ModIds.AUTO_SPRINT, ModIds.ZOOM, ModIds.FPS_DISPLAY, ModIds.CPS_DISPLAY,
            ModIds.THIRD_PERSON_NAMETAG,
        };
        for (String id : allIds) {
            modManager.removeMod(id);
        }
        showEnabledOverlays();
    }
}
