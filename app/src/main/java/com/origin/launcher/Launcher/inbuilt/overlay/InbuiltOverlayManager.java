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
import com.origin.launcher.Launcher.inbuilt.overlay.HotbarOneOverlay;
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
    private HotbarOneOverlay hotbarOneOverlay;
    private HotbarTwoOverlay   hotbarTwoOverlay;
    private HotbarThreeOverlay hotbarThreeOverlay;
    private HotbarFourOverlay  hotbarFourOverlay;
    private HotbarFiveOverlay  hotbarFiveOverlay;
    private HotbarSixOverlay   hotbarSixOverlay;
    private HotbarSevenOverlay hotbarSevenOverlay;
    private HotbarEightOverlay hotbarEightOverlay;
    private HotbarNineOverlay  hotbarNineOverlay;
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

    public ModMenuOverlay getModMenuOverlay() {
        return modMenuOverlay;
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

        if (modManager.isModAdded(ModIds.MOD_MENU)) {
            if (modMenuOverlay == null) modMenuOverlay = new ModMenuOverlay(activity);
            int[] menuPos = getStartPosition(ModIds.MOD_MENU, START_X, 10);
            modMenuOverlay.showImmediate(menuPos[0], menuPos[1]);
            overlays.add(modMenuOverlay);
            nextY += SPACING;
        }

        if (modManager.isModAdded(ModIds.QUICK_DROP)) {
            int[] pos = getStartPosition(ModIds.QUICK_DROP, START_X, nextY);
            if (quickDropOverlay == null) quickDropOverlay = new QuickDropOverlay(activity);
            quickDropOverlay.showImmediate(pos[0], pos[1]);
            overlays.add(quickDropOverlay);
            nextY += SPACING;
        }
        if (modManager.isModAdded(ModIds.CAMERA_PERSPECTIVE)) {
            int[] pos = getStartPosition(ModIds.CAMERA_PERSPECTIVE, START_X, nextY);
            if (cameraPerspectiveOverlay == null) cameraPerspectiveOverlay = new CameraPerspectiveOverlay(activity);
            cameraPerspectiveOverlay.showImmediate(pos[0], pos[1]);
            overlays.add(cameraPerspectiveOverlay);
            nextY += SPACING;
        }
        if (modManager.isModAdded(ModIds.TOGGLE_HUD)) {
            int[] pos = getStartPosition(ModIds.TOGGLE_HUD, START_X, nextY);
            if (toggleHudOverlay == null) toggleHudOverlay = new ToggleHudOverlay(activity);
            toggleHudOverlay.showImmediate(pos[0], pos[1]);
            overlays.add(toggleHudOverlay);
            nextY += SPACING;
        }
        if (modManager.isModAdded(ModIds.HOTBAR_ONE)) {
            int[] pos = getStartPosition(ModIds.HOTBAR_ONE, START_X, nextY);

            if (hotbarOneOverlay == null)
                hotbarOneOverlay = new HotbarOneOverlay(activity);
            
            hotbarOneOverlay.showImmediate(pos[0], pos[1]);
            overlays.add(hotbarOneOverlay);
            nextY += SPACING;
        }
        
if (modManager.isModAdded(ModIds.HOTBAR_TWO)) {
    int[] pos = getStartPosition(ModIds.HOTBAR_TWO, START_X, nextY);
    if (hotbarTwoOverlay == null) hotbarTwoOverlay = new HotbarTwoOverlay(activity);
    hotbarTwoOverlay.showImmediate(pos[0], pos[1]);
    overlays.add(hotbarTwoOverlay);
    nextY += SPACING;
}
if (modManager.isModAdded(ModIds.HOTBAR_THREE)) {
    int[] pos = getStartPosition(ModIds.HOTBAR_THREE, START_X, nextY);
    if (hotbarThreeOverlay == null) hotbarThreeOverlay = new HotbarThreeOverlay(activity);
    hotbarThreeOverlay.showImmediate(pos[0], pos[1]);
    overlays.add(hotbarThreeOverlay);
    nextY += SPACING;
}
if (modManager.isModAdded(ModIds.HOTBAR_FOUR)) {
    int[] pos = getStartPosition(ModIds.HOTBAR_FOUR, START_X, nextY);
    if (hotbarFourOverlay == null) hotbarFourOverlay = new HotbarFourOverlay(activity);
    hotbarFourOverlay.showImmediate(pos[0], pos[1]);
    overlays.add(hotbarFourOverlay);
    nextY += SPACING;
}
if (modManager.isModAdded(ModIds.HOTBAR_FIVE)) {
    int[] pos = getStartPosition(ModIds.HOTBAR_FIVE, START_X, nextY);
    if (hotbarFiveOverlay == null) hotbarFiveOverlay = new HotbarFiveOverlay(activity);
    hotbarFiveOverlay.showImmediate(pos[0], pos[1]);
    overlays.add(hotbarFiveOverlay);
    nextY += SPACING;
}
if (modManager.isModAdded(ModIds.HOTBAR_SIX)) {
    int[] pos = getStartPosition(ModIds.HOTBAR_SIX, START_X, nextY);
    if (hotbarSixOverlay == null) hotbarSixOverlay = new HotbarSixOverlay(activity);
    hotbarSixOverlay.showImmediate(pos[0], pos[1]);
    overlays.add(hotbarSixOverlay);
    nextY += SPACING;
}
if (modManager.isModAdded(ModIds.HOTBAR_SEVEN)) {
    int[] pos = getStartPosition(ModIds.HOTBAR_SEVEN, START_X, nextY);
    if (hotbarSevenOverlay == null) hotbarSevenOverlay = new HotbarSevenOverlay(activity);
    hotbarSevenOverlay.showImmediate(pos[0], pos[1]);
    overlays.add(hotbarSevenOverlay);
    nextY += SPACING;
}
if (modManager.isModAdded(ModIds.HOTBAR_EIGHT)) {
    int[] pos = getStartPosition(ModIds.HOTBAR_EIGHT, START_X, nextY);
    if (hotbarEightOverlay == null) hotbarEightOverlay = new HotbarEightOverlay(activity);
    hotbarEightOverlay.showImmediate(pos[0], pos[1]);
    overlays.add(hotbarEightOverlay);
    nextY += SPACING;
}
if (modManager.isModAdded(ModIds.HOTBAR_NINE)) {
    int[] pos = getStartPosition(ModIds.HOTBAR_NINE, START_X, nextY);
    if (hotbarNineOverlay == null) hotbarNineOverlay = new HotbarNineOverlay(activity);
    hotbarNineOverlay.showImmediate(pos[0], pos[1]);
    overlays.add(hotbarNineOverlay);
    nextY += SPACING;
}
        if (modManager.isModAdded(ModIds.AUTO_SPRINT)) {
            int[] pos = getStartPosition(ModIds.AUTO_SPRINT, START_X, nextY);
            if (autoSprintOverlay == null) autoSprintOverlay = new AutoSprintOverlay(activity, modManager.getAutoSprintKey());
            autoSprintOverlay.showImmediate(pos[0], pos[1]);
            overlays.add(autoSprintOverlay);
            nextY += SPACING;
        }
        if (modManager.isModAdded(ModIds.ZOOM)) {
            if (zoomOverlay == null) zoomOverlay = new ZoomOverlay(activity);
            zoomOverlay.initializeForKeyboard();
            int[] pos = getStartPosition(ModIds.ZOOM, START_X, nextY);
            zoomOverlay.showImmediate(pos[0], pos[1]);
            overlays.add(zoomOverlay);
            nextY += SPACING;
        }
        if (modManager.isModAdded(ModIds.FPS_DISPLAY)) {
            int[] pos = getStartPosition(ModIds.FPS_DISPLAY, START_X, nextY);
            if (fpsDisplayOverlay == null) fpsDisplayOverlay = new FpsDisplayOverlay(activity);
            fpsDisplayOverlay.showImmediate(pos[0], pos[1]);
            overlays.add(fpsDisplayOverlay);
            nextY += SPACING;
        }
        if (modManager.isModAdded(ModIds.CPS_DISPLAY)) {
            int[] pos = getStartPosition(ModIds.CPS_DISPLAY, START_X, nextY);
            if (cpsDisplayOverlay == null) cpsDisplayOverlay = new CpsDisplayOverlay(activity);
            cpsDisplayOverlay.showImmediate(pos[0], pos[1]);
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
            ModIds.MOD_MENU, ModIds.QUICK_DROP, ModIds.CAMERA_PERSPECTIVE, ModIds.TOGGLE_HUD,
            ModIds.AUTO_SPRINT, ModIds.ZOOM, ModIds.FPS_DISPLAY, ModIds.CPS_DISPLAY,
            ModIds.THIRD_PERSON_NAMETAG, ModIds.HOTBAR_ONE,
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
            ModIds.MOD_MENU, ModIds.QUICK_DROP, ModIds.CAMERA_PERSPECTIVE, ModIds.TOGGLE_HUD,
            ModIds.AUTO_SPRINT, ModIds.ZOOM, ModIds.FPS_DISPLAY, ModIds.CPS_DISPLAY,
            ModIds.THIRD_PERSON_NAMETAG,
            ModIds.HOTBAR_ONE, ModIds.HOTBAR_TWO, ModIds.HOTBAR_THREE, ModIds.HOTBAR_FOUR,
            ModIds.HOTBAR_FIVE, ModIds.HOTBAR_SIX, ModIds.HOTBAR_SEVEN, ModIds.HOTBAR_EIGHT,
            ModIds.HOTBAR_NINE,
        };
        for (String id : allIds) {
            modManager.removeMod(id);
        }
        showEnabledOverlays();
    }
}
