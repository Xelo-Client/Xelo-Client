package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModSizeStore;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CpsDisplayOverlay {
    private static final String MOD_ID = ModIds.CPS_DISPLAY;
    private static final int UPDATE_INTERVAL = 100;
    private static final double CPS_WINDOW = 1.0;
    private static final float DRAG_THRESHOLD = 10f;

    private final Activity activity;
    private final WindowManager windowManager;
    private final InbuiltModSizeStore sizeStore;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Double> clickTimes = new ArrayList<>();

    private View overlayView;
    private TextView statsText;
    private WindowManager.LayoutParams wmParams;
    private boolean isShowing = false;
    private Runnable pendingShowRunnable;

    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private long touchDownTime = 0;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isShowing) return;
            updateDisplay();
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    public CpsDisplayOverlay(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        this.sizeStore = InbuiltModSizeStore.getInstance();
        sizeStore.init(activity.getApplicationContext());
        setupGameInputListener();
    }

    private void setupGameInputListener() {
        View decorView = activity.getWindow().getDecorView();
        decorView.setOnTouchListener((v, event) -> {
            if (isShowing) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                    registerClick();
                }
            }
            return false;
        });
    }

    public void registerClick() {
        synchronized (clickTimes) {
            clickTimes.add(getTime());
        }
    }

    private double getTime() {
        return System.nanoTime() / 1_000_000_000.0;
    }

    private int getCps() {
        synchronized (clickTimes) {
            double now = getTime();
            Iterator<Double> it = clickTimes.iterator();
            while (it.hasNext()) {
                if (now - it.next() > CPS_WINDOW) {
                    it.remove();
                }
            }
            return clickTimes.size();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * activity.getResources().getDisplayMetrics().density);
    }

    public void show(int startX, int startY) {
        if (isShowing) return;
        if (pendingShowRunnable != null) {
            handler.removeCallbacks(pendingShowRunnable);
        }
        pendingShowRunnable = () -> showInternal(startX, startY);
        handler.postDelayed(pendingShowRunnable, 500);
    }

    private void showInternal(int startX, int startY) {
        pendingShowRunnable = null;
        if (isShowing || activity.isFinishing() || activity.isDestroyed()) return;

        try {
            overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_stats_display, null);
            statsText = overlayView.findViewById(R.id.stats_text);
            statsText.setText("CPS: 0");

            int sizeDp = sizeStore.getSize(MOD_ID);
            float scale = sizeStore.getScale(MOD_ID);
            int widthPx = dpToPx(sizeDp);
            int heightPx = dpToPx((int) (sizeDp * 0.6f));

            wmParams = new WindowManager.LayoutParams(
                (int) (widthPx * scale),
                (int) (heightPx * scale),
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );
            wmParams.gravity = Gravity.TOP | Gravity.START;

            float savedX = sizeStore.getPositionX(MOD_ID);
            float savedY = sizeStore.getPositionY(MOD_ID);
            wmParams.x = savedX >= 0 ? (int) savedX : startX;
            wmParams.y = savedY >= 0 ? (int) savedY : startY;
            wmParams.token = activity.getWindow().getDecorView().getWindowToken();

            overlayView.setAlpha(sizeStore.getOpacity(MOD_ID) / 100f);
            overlayView.setOnTouchListener(this::handleTouch);
            windowManager.addView(overlayView, wmParams);
            isShowing = true;

            handler.post(updateRunnable);
        } catch (Exception e) {
            showFallback(startX, startY);
        }
    }

    private void showFallback(int startX, int startY) {
        if (isShowing) return;
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_stats_display, null);
        statsText = overlayView.findViewById(R.id.stats_text);
        statsText.setText("CPS: 0");

        int sizeDp = sizeStore.getSize(MOD_ID);
        float scale = sizeStore.getScale(MOD_ID);
        int widthPx = dpToPx(sizeDp);
        int heightPx = dpToPx((int) (sizeDp * 0.6f));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            (int) (widthPx * scale),
            (int) (heightPx * scale)
        );
        params.gravity = Gravity.TOP | Gravity.START;

        float savedX = sizeStore.getPositionX(MOD_ID);
        float savedY = sizeStore.getPositionY(MOD_ID);
        params.leftMargin = savedX >= 0 ? (int) savedX : startX;
        params.topMargin = savedY >= 0 ? (int) savedY : startY;

        overlayView.setAlpha(sizeStore.getOpacity(MOD_ID) / 100f);
        overlayView.setOnTouchListener(this::handleTouchFallback);
        rootView.addView(overlayView, params);
        isShowing = true;
        wmParams = null;

        handler.post(updateRunnable);
    }

    private void updateDisplay() {
        if (statsText != null) {
            statsText.setText("CPS: " + getCps());
        }
    }

    private boolean handleTouch(View v, MotionEvent event) {
        if (sizeStore.isLocked(MOD_ID)) return true;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialX = wmParams.x;
                initialY = wmParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = false;
                touchDownTime = SystemClock.uptimeMillis();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    isDragging = true;
                }
                if (isDragging && windowManager != null && overlayView != null) {
                    wmParams.x = (int) (initialX + dx);
                    wmParams.y = (int) (initialY + dy);
                    windowManager.updateViewLayout(overlayView, wmParams);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    sizeStore.setPositionX(MOD_ID, (float) wmParams.x);
                    sizeStore.setPositionY(MOD_ID, (float) wmParams.y);
                }
                isDragging = false;
                return true;
        }
        return false;
    }

    private boolean handleTouchFallback(View v, MotionEvent event) {
        if (sizeStore.isLocked(MOD_ID)) return true;

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) overlayView.getLayoutParams();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.leftMargin;
                initialY = params.topMargin;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = false;
                touchDownTime = SystemClock.uptimeMillis();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    isDragging = true;
                }
                if (isDragging) {
                    params.leftMargin = (int) (initialX + dx);
                    params.topMargin = (int) (initialY + dy);
                    overlayView.setLayoutParams(params);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    sizeStore.setPositionX(MOD_ID, (float) params.leftMargin);
                    sizeStore.setPositionY(MOD_ID, (float) params.topMargin);
                }
                isDragging = false;
                return true;
        }
        return false;
    }

    public void hide() {
        if (pendingShowRunnable != null) {
            handler.removeCallbacks(pendingShowRunnable);
            pendingShowRunnable = null;
        }
        if (!isShowing) return;
        isShowing = false;
        handler.removeCallbacks(updateRunnable);
        try {
            if (wmParams != null && windowManager != null) {
                windowManager.removeView(overlayView);
            } else {
                ViewGroup rootView = activity.findViewById(android.R.id.content);
                if (rootView != null) {
                    rootView.removeView(overlayView);
                }
            }
        } catch (Exception e) {}
        overlayView = null;
        statsText = null;
        synchronized (clickTimes) {
            clickTimes.clear();
        }
    }
}