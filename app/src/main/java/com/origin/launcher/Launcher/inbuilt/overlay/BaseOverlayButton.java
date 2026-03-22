package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.origin.launcher.R;
import com.origin.launcher.ThemeManager;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModSizeStore;

public abstract class BaseOverlayButton {

    protected final Activity activity;
    protected View overlayView;
    protected WindowManager windowManager;
    protected WindowManager.LayoutParams wmParams;

    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private long touchDownTime = 0;

    private static final long TAP_TIMEOUT = 200;
    private static final float DRAG_THRESHOLD = 10f;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isShowing = false;

    public BaseOverlayButton(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        InbuiltModSizeStore.getInstance().init(activity.getApplicationContext());
    }

    protected int getButtonSizePx() {
        int sizeDp = InbuiltModManager.getInstance(activity).getOverlayButtonSize(getModId());
        float density = activity.getResources().getDisplayMetrics().density;
        return (int) (sizeDp * density);
    }

    protected float getButtonAlpha() {
        int opacity = InbuiltModManager.getInstance(activity).getOverlayButtonOpacity(getModId());
        return opacity / 100f;
    }

    protected abstract String getModId();

    public void tick() {}

    public void show(int startX, int startY) {
        if (isShowing) return;
        handler.post(() -> showInternal(startX, startY));
    }

    // Applies themed pressed/unpressed bitmaps if available, otherwise falls back to the default selector drawable
    private void applyThemedIcon(ImageButton btn) {
        Bitmap normal = ThemeManager.getInstance().getOverlayButtonBitmap(getModId());
        Bitmap pressed = ThemeManager.getInstance().getOverlayButtonPressedBitmap(getModId());

        if (normal != null && pressed != null) {
            // Build a StateListDrawable matching the selector pattern: activated/pressed → pressed bitmap, default → normal bitmap
            StateListDrawable stateDrawable = new StateListDrawable();
            stateDrawable.addState(new int[]{android.R.attr.state_activated}, new BitmapDrawable(activity.getResources(), pressed));
            stateDrawable.addState(new int[]{android.R.attr.state_pressed}, new BitmapDrawable(activity.getResources(), pressed));
            stateDrawable.addState(new int[]{}, new BitmapDrawable(activity.getResources(), normal));
            btn.setImageDrawable(stateDrawable);
        } else {
            btn.setImageResource(getIconResource());
        }
    }

    private void showInternal(int startX, int startY) {
        if (isShowing || activity.isFinishing() || activity.isDestroyed()) return;
        try {
            overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_mod_button, null);
            ImageButton btn = (ImageButton) overlayView;
            applyThemedIcon(btn);

            int buttonSize = getButtonSizePx();
            btn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
            btn.setAlpha(getButtonAlpha());
            onOverlayViewCreated(btn);

            wmParams = new WindowManager.LayoutParams(
                    buttonSize,
                    buttonSize,
                    WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );
            wmParams.gravity = Gravity.TOP | Gravity.START;
            wmParams.x = startX;
            wmParams.y = startY;
            wmParams.token = activity.getWindow().getDecorView().getWindowToken();

            btn.setOnTouchListener(this::handleTouch);
            windowManager.addView(overlayView, wmParams);
            isShowing = true;
        } catch (Exception e) {
            showFallback(startX, startY);
        }
    }

    private void showFallback(int startX, int startY) {
        if (isShowing) return;
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_mod_button, null);
        ImageButton btn = (ImageButton) overlayView;
        applyThemedIcon(btn);

        int buttonSize = getButtonSizePx();
        btn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        btn.setAlpha(getButtonAlpha());
        onOverlayViewCreated(btn);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                buttonSize,
                buttonSize
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.leftMargin = startX;
        params.topMargin = startY;

        btn.setOnTouchListener(this::handleTouchFallback);
        rootView.addView(overlayView, params);
        isShowing = true;
        wmParams = null;
    }

    public void hide() {
        if (!isShowing || overlayView == null) return;
        try {
            if (wmParams != null && windowManager != null) {
                windowManager.removeView(overlayView);
            } else {
                ViewGroup rootView = activity.findViewById(android.R.id.content);
                if (rootView != null) {
                    rootView.removeView(overlayView);
                }
            }
        } catch (Exception ignored) {}
        overlayView = null;
        isShowing = false;
    }

    private boolean handleTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialX = wmParams.x;
                initialY = wmParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = false;
                touchDownTime = SystemClock.uptimeMillis();
                v.getParent().requestDisallowInterceptTouchEvent(true);
                onTouchDown(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (InbuiltModSizeStore.getInstance().isLocked(getModId())) {
                    return true;
                }
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    if (!isDragging) {
                        isDragging = true;
                        onDragStarted();
                    }
                }
                if (isDragging && windowManager != null && overlayView != null) {
                    wmParams.x = (int) (initialX + dx);
                    wmParams.y = (int) (initialY + dy);
                    windowManager.updateViewLayout(overlayView, wmParams);
                }
                return true;
            case MotionEvent.ACTION_UP:
                long elapsed = SystemClock.uptimeMillis() - touchDownTime;
                if (isDragging) {
                    InbuiltModSizeStore store = InbuiltModSizeStore.getInstance();
                    store.setPositionX(getModId(), wmParams.x);
                    store.setPositionY(getModId(), wmParams.y);
                    onTouchUp(event, true);
                } else if (elapsed < TAP_TIMEOUT) {
                    onTouchUp(event, false);
                    handler.post(this::onButtonClick);
                } else {
                    onTouchUp(event, false);
                }
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                onTouchCancel(event);
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return false;
        }
        return false;
    }

    private boolean handleTouchFallback(View v, MotionEvent event) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) overlayView.getLayoutParams();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.leftMargin;
                initialY = params.topMargin;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = false;
                touchDownTime = SystemClock.uptimeMillis();
                v.getParent().requestDisallowInterceptTouchEvent(true);
                onTouchDown(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (InbuiltModSizeStore.getInstance().isLocked(getModId())) {
                    return true;
                }
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    if (!isDragging) {
                        isDragging = true;
                        onDragStarted();
                    }
                }
                if (isDragging) {
                    params.leftMargin = (int) (initialX + dx);
                    params.topMargin = (int) (initialY + dy);
                    overlayView.setLayoutParams(params);
                }
                return true;
            case MotionEvent.ACTION_UP:
                long elapsed = SystemClock.uptimeMillis() - touchDownTime;
                if (isDragging) {
                    InbuiltModSizeStore store = InbuiltModSizeStore.getInstance();
                    store.setPositionX(getModId(), params.leftMargin);
                    store.setPositionY(getModId(), params.topMargin);
                    onTouchUp(event, true);
                } else if (elapsed < TAP_TIMEOUT) {
                    onTouchUp(event, false);
                    handler.post(this::onButtonClick);
                } else {
                    onTouchUp(event, false);
                }
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            case MotionEvent.ACTION_CANCEL:
                onTouchCancel(event);
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return false;
    }

    protected void onTouchDown(MotionEvent event) {}
    protected void onTouchUp(MotionEvent event, boolean wasDragging) {}
    protected void onTouchCancel(MotionEvent event) {}
    protected void onDragStarted() {}

    protected void sendKey(int keyCode) {
        handler.post(() -> {
            long time = SystemClock.uptimeMillis();
            KeyEvent down = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            KeyEvent up = new KeyEvent(time, time + 10, KeyEvent.ACTION_UP, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            activity.dispatchKeyEvent(down);
            activity.dispatchKeyEvent(up);
        });
    }

    protected void sendKeyDown(int keyCode) {
        handler.post(() -> {
            long time = SystemClock.uptimeMillis();
            KeyEvent down = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            activity.dispatchKeyEvent(down);
        });
    }

    protected void sendKeyUp(int keyCode) {
        handler.post(() -> {
            long time = SystemClock.uptimeMillis();
            KeyEvent up = new KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            activity.dispatchKeyEvent(up);
        });
    }

    protected abstract int getIconResource();
    protected void onOverlayViewCreated(ImageButton btn) {}
    protected abstract void onButtonClick();
}
