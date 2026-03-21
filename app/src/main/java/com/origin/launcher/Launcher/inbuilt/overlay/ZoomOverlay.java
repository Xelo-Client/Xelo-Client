package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageButton;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.Launcher.inbuilt.XeloOverlay.nativemod.ZoomMod;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;

public class ZoomOverlay extends BaseOverlayButton {
    private static final String TAG = "ZoomOverlay";
    private static final long HOLD_THRESHOLD_MS = 300;
    private static final float DRAG_THRESHOLD = 10f;

    private boolean isZooming = false;
    private boolean initialized = false;
    private boolean isHolding = false;
    private boolean isDraggingOverride = false;
    private float touchStartX, touchStartY;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable holdRunnable = () -> {
        if (!isDraggingOverride) {
            isHolding = true;
            if (!isZooming) {
                isZooming = true;
                applyZoomLevel();
                ZoomMod.nativeOnKeyDown();
                updateButtonState(true);
            }
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
        return R.drawable.ic_zoom_selector;
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
    protected void onButtonClick() {
        if (!isHoldModeEnabled()) {
            toggleZoom();
        }
    }

    @Override
    protected void onOverlayViewCreated(ImageButton btn) {
        btn.setOnTouchListener((v, event) -> {
            if (isHoldModeEnabled()) {
                return handleHoldModeTouch(event);
            }
            return false;
        });
    }

    private boolean handleHoldModeTouch(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getRawX();
                touchStartY = event.getRawY();
                isDraggingOverride = false;
                isHolding = false;
                handler.postDelayed(holdRunnable, HOLD_THRESHOLD_MS);
                return false;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getRawX() - touchStartX);
                float dy = Math.abs(event.getRawY() - touchStartY);
                if (dx > DRAG_THRESHOLD || dy > DRAG_THRESHOLD) {
                    isDraggingOverride = true;
                    handler.removeCallbacks(holdRunnable);
                }
                return false;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(holdRunnable);
                if (isDraggingOverride) {
                    isDraggingOverride = false;
                    isHolding = false;
                    return false;
                }
                if (isHolding) {
                    if (isZooming) {
                        isZooming = false;
                        ZoomMod.nativeOnKeyUp();
                        updateButtonState(false);
                    }
                    isHolding = false;
                    return true;
                }
                return false;
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(holdRunnable);
                if (isHolding && isZooming) {
                    isZooming = false;
                    ZoomMod.nativeOnKeyUp();
                    updateButtonState(false);
                }
                isDraggingOverride = false;
                isHolding = false;
                return false;
        }
        return false;
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

    private void updateButtonState(boolean active) {
        if (overlayView instanceof ImageButton) {
            ImageButton btn = (ImageButton) overlayView;
            btn.setActivated(active);
            btn.setAlpha(getButtonAlpha() * (active ? 1.1f : 1.0f));
            btn.setBackgroundResource(active ? R.drawable.bg_overlay_button_active : R.drawable.bg_overlay_button);
        }
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

    public boolean isZooming() {
        return isZooming;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
