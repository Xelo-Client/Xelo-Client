package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.Launcher.inbuilt.XeloOverlay.nativemod.XeloCore;
import com.origin.launcher.Launcher.inbuilt.XeloOverlay.nativemod.ZoomMod;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.dialogs.ButtonStyleDialog;

public class ZoomOverlay extends BaseOverlayButton {

    private static final String TAG = "ZoomOverlay";
    private static final long HOLD_THRESHOLD_MS = 150;

    private boolean isZooming = false;
    private boolean initialized = false;
    private boolean isHolding = false;
    private boolean lastInWorldState = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable holdRunnable = () -> {
        isHolding = true;
        if (!isZooming) {
            isZooming = true;
            applyZoomLevel();
            ZoomMod.nativeOnKeyDown();
            updateButtonState(true);
        }
    };

    private final Runnable worldPoller = new Runnable() {
        @Override
        public void run() {
            boolean inWorld = XeloCore.isInWorld();
            if (inWorld != lastInWorldState) {
                lastInWorldState = inWorld;
                if (inWorld) {
                    showInWorld();
                } else {
                    hideOutOfWorld();
                }
            }
            handler.postDelayed(this, 50);
        }
    };

    public ZoomOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.ZOOM;
    }

    @Override
    protected int getIconResource() {
        boolean usePng = ButtonStyleDialog.isUsingPng(activity, ModIds.ZOOM);
        return usePng ? R.drawable.ic_zoom_selector : R.drawable.ic_zoom;
    }

    @Override
    public void show(int startX, int startY) {
        if (!initialized) {
            initializeNative();
        }
        super.show(startX, startY);
    }

    public void initializeForKeyboard() {
        if (!initialized) {
            initializeNative();
        }
    }

    private void initializeNative() {
        handler.postDelayed(() -> {
            if (ZoomMod.init()) {
                initialized = true;
                applyZoomLevel();
                Log.i(TAG, "Zoom native initialized successfully");
            } else {
                Log.e(TAG, "Failed to initialize zoom native");
            }
        }, 1000);
    }

    private void applyZoomLevel() {
        int zoomPercent = InbuiltModManager.getInstance(activity).getZoomLevel();
        long normalFov = 5360000000L;
        long maxZoomFov = 5310000000L;
        long zoomLevel = normalFov - (long) ((normalFov - maxZoomFov) * zoomPercent / 100.0);
        ZoomMod.nativeSetZoomLevel(zoomLevel);
    }

    private boolean isHoldModeEnabled() {
        return InbuiltModManager.getInstance(activity).getZoomHoldMode();
    }

    @Override
    protected void onTouchDown(MotionEvent event) {
        if (!isHoldModeEnabled() || !initialized) return;
        isHolding = false;
        handler.postDelayed(holdRunnable, HOLD_THRESHOLD_MS);
    }

    @Override
    protected void onTouchUp(MotionEvent event, boolean wasDragging) {
        if (!isHoldModeEnabled()) return;
        handler.removeCallbacks(holdRunnable);
        if (wasDragging) {
            if (isHolding && isZooming) {
                isZooming = false;
                ZoomMod.nativeOnKeyUp();
                updateButtonState(false);
            }
            isHolding = false;
            return;
        }
        if (isHolding) {
            if (isZooming) {
                isZooming = false;
                ZoomMod.nativeOnKeyUp();
                updateButtonState(false);
            }
            isHolding = false;
        }
    }

    @Override
    protected void onTouchCancel(MotionEvent event) {
        if (!isHoldModeEnabled()) return;
        handler.removeCallbacks(holdRunnable);
        if (isHolding && isZooming) {
            isZooming = false;
            ZoomMod.nativeOnKeyUp();
            updateButtonState(false);
        }
        isHolding = false;
    }

    @Override
    protected void onDragStarted() {
        if (!isHoldModeEnabled()) return;
        handler.removeCallbacks(holdRunnable);
        if (isHolding && isZooming) {
            isZooming = false;
            ZoomMod.nativeOnKeyUp();
            updateButtonState(false);
        }
        isHolding = false;
    }

    @Override
    protected void onButtonClick() {
        if (!isHoldModeEnabled()) {
            toggleZoom();
        }
    }

    @Override
    protected void onOverlayViewCreated(ImageButton btn) {
        applyIconPadding(btn);
        handler.removeCallbacks(worldPoller);
        lastInWorldState = XeloCore.isInWorld();
        if (!lastInWorldState) {
            hideOutOfWorld();
        }
        handler.post(worldPoller);
    }

    public void onKeyDown() {
        if (!initialized) {
            Log.w(TAG, "Zoom not initialized yet");
            return;
        }
        if (isZooming) return;
        isZooming = true;
        applyZoomLevel();
        ZoomMod.nativeOnKeyDown();
        updateButtonState(true);
    }

    public void onKeyUp() {
        if (!initialized || !isZooming) return;
        isZooming = false;
        ZoomMod.nativeOnKeyUp();
        updateButtonState(false);
    }

    private void toggleZoom() {
        if (!initialized) {
            Log.w(TAG, "Zoom not initialized yet");
            return;
        }
        isZooming = !isZooming;
        if (isZooming) {
            applyZoomLevel();
            ZoomMod.nativeOnKeyDown();
            updateButtonState(true);
        } else {
            ZoomMod.nativeOnKeyUp();
            updateButtonState(false);
        }
    }

    private void applyIconPadding(ImageButton btn) {
        boolean usePng = ButtonStyleDialog.isUsingPng(activity, ModIds.ZOOM);
        int p = usePng ? 0 : dpToPx(6);
        btn.setPadding(p, p, p, p);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }

    private void updateButtonState(boolean active) {
        if (overlayView != null) {
            ImageButton btn = overlayView.findViewById(R.id.mod_overlay_button);
            if (btn != null) {
                boolean usePng = ButtonStyleDialog.isUsingPng(activity, ModIds.ZOOM);
                btn.setActivated(active);
                btn.setAlpha(getButtonAlpha() * (active ? 1.1f : 1.0f));
                if (usePng) {
                    btn.setBackgroundResource(R.drawable.bg_overlay_button_png);
                } else {
                    btn.setBackgroundResource(active ? R.drawable.bg_overlay_button_active : R.drawable.bg_overlay_button);
                }
            }
        }
    }

    private void hideOutOfWorld() {
        if (overlayView == null) return;
        activity.runOnUiThread(() -> {
            overlayView.setVisibility(View.GONE);
            handler.removeCallbacks(holdRunnable);
            if (isZooming && initialized) {
                isZooming = false;
                ZoomMod.nativeOnKeyUp();
                updateButtonState(false);
            }
            isHolding = false;
        });
    }

    private void showInWorld() {
        if (overlayView == null) return;
        activity.runOnUiThread(() -> overlayView.setVisibility(View.VISIBLE));
    }

    public void onScroll(float delta) {
        if (initialized && isZooming) {
            ZoomMod.nativeOnScroll(delta);
        }
    }

    @Override
    public void hide() {
        if (isZooming && initialized) {
            ZoomMod.nativeOnKeyUp();
            isZooming = false;
        }
        handler.removeCallbacks(holdRunnable);
        super.hide();
    }

    public void destroy() {
        handler.removeCallbacks(holdRunnable);
        handler.removeCallbacks(worldPoller);
        hide();
    }

    public boolean isZooming() {
        return isZooming;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
