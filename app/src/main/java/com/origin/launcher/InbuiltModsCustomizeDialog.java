package com.origin.launcher;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.origin.launcher.Adapter.InbuiltCustomizeAdapter;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModSizeStore;
import com.origin.launcher.Launcher.inbuilt.overlay.InbuiltOverlayManager;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.ThemeManager;
import com.origin.launcher.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InbuiltModsCustomizeDialog extends Dialog implements InbuiltCustomizeAdapter.Callback {

    private View lastSelectedButton;
    private MaterialButton lockButton;
    private boolean isLocked = false;

    private final Map<String, Integer> modSizes = new HashMap<>();
    private final Map<String, Integer> modOpacity = new HashMap<>();
    private final Map<String, View> modButtons = new HashMap<>();
    private final Map<String, Integer> modZoomKeybinds = new HashMap<>();
    private final Map<String, Integer> modZoomLevels = new HashMap<>();
    private String lastSelectedId = null;

    private static final int MIN_SIZE_DP = 32;
    private static final int MAX_SIZE_DP = 96;
    private static final int DEFAULT_SIZE_DP = 40;
    private static final int MIN_OPACITY = 20;
    private static final int MAX_OPACITY = 100;
    private static final int DEFAULT_OPACITY = 100;
    private static final int SEEKBAR_MAX = 100;

    private RecyclerView adapterRecyclerView;
    private InbuiltCustomizeAdapter adapter;
    private boolean isAdapterVisible = false;
    private boolean isResetting = false;
    private final boolean showBackground;
    private FrameLayout adapterContainer;
    private TextView emptyAdapterText;

    public InbuiltModsCustomizeDialog(@NonNull Context context, boolean showBackground) {
        super(context, R.style.Theme_Material3_Dark_NoActionBar_Custom);
        this.showBackground = showBackground;
    }

    private GradientDrawable makeBlackBg() {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(Color.BLACK);
        d.setCornerRadius(dpToPx(12));
        return d;
    }

    private void applyCyanHighlight(View v) {
        GradientDrawable highlight = new GradientDrawable();
        highlight.setShape(GradientDrawable.RECTANGLE);
        highlight.setColor(Color.TRANSPARENT);
        highlight.setStroke(dpToPx(2), Color.CYAN);
        highlight.setCornerRadius(dpToPx(8));
        v.setBackground(highlight);
    }

    private void selectButton(View v, String id) {
        if (lastSelectedButton != null && lastSelectedButton != v) {
            lastSelectedButton.setBackgroundResource(R.drawable.bg_overlay_button);
        }
        lastSelectedButton = v;
        lastSelectedId = id;
        applyCyanHighlight(v);
        boolean locked = InbuiltModSizeStore.getInstance().isLocked(id);
        isLocked = locked;
        lockButton.setText(locked ? "Locked" : "Lock");
        lockButton.setTextColor(locked ? Color.BLACK : Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(locked ? Color.GRAY : Color.BLACK);
        bg.setCornerRadius(dpToPx(12));
        lockButton.setBackground(bg);
        lockButton.setBackgroundTintList(null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_inbuilt_mods_customize);

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        Button resetButton = findViewById(R.id.reset_button);
        Button doneButton = findViewById(R.id.done_button);
        Button customizeButton = findViewById(R.id.opacity_button);
        FrameLayout grid = findViewById(R.id.inbuilt_buttons_grid);
        View bottomButtons = findViewById(R.id.bottom_buttons_container);

        lockButton = findViewById(R.id.lock_button);
        lockButton.setBackground(makeBlackBg());
        lockButton.setBackgroundTintList(null);
        lockButton.setTextColor(Color.WHITE);
        lockButton.setText("Lock");
        lockButton.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        lockButton.setMinHeight(dpToPx(48));
        lockButton.setMinWidth(dpToPx(80));
        lockButton.setStateListAnimator(null);

        lockButton.setOnClickListener(v -> {
            if (lastSelectedId == null) return;
            isLocked = !isLocked;
            lockButton.setText(isLocked ? "Locked" : "Lock");
            lockButton.setTextColor(isLocked ? Color.BLACK : Color.WHITE);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(isLocked ? Color.GRAY : Color.BLACK);
            bg.setCornerRadius(dpToPx(12));
            lockButton.setBackground(bg);
            lockButton.setBackgroundTintList(null);
            InbuiltModSizeStore.getInstance().setLocked(lastSelectedId, isLocked);
        });

        customizeButton.setText("Customize");

        resetButton.setBackground(makeBlackBg());
        resetButton.setBackgroundTintList(null);
        resetButton.setTextColor(Color.WHITE);

        customizeButton.setBackground(makeBlackBg());
        customizeButton.setBackgroundTintList(null);
        customizeButton.setTextColor(Color.WHITE);

        doneButton.setBackground(makeBlackBg());
        doneButton.setBackgroundTintList(null);
        doneButton.setTextColor(Color.WHITE);

        resetButton.setStateListAnimator(null);
        customizeButton.setStateListAnimator(null);
        doneButton.setStateListAnimator(null);

        int padding8dp = dpToPx(8);
        int padding16dp = dpToPx(16);
        int padding24dp = dpToPx(24);

        resetButton.setPadding(padding24dp, padding8dp, padding24dp, padding8dp);
        customizeButton.setPadding(padding16dp, padding8dp, padding16dp, padding8dp);
        doneButton.setPadding(padding24dp, padding8dp, padding24dp, padding8dp);

        resetButton.setMinHeight(dpToPx(48));
        customizeButton.setMinHeight(dpToPx(48));
        doneButton.setMinHeight(dpToPx(48));
        resetButton.setEllipsize(null);
        customizeButton.setEllipsize(null);
        doneButton.setEllipsize(null);

        int buttonWidth = dpToPx(100);
        customizeButton.setMinWidth(buttonWidth);
        resetButton.setMinWidth(buttonWidth);
        doneButton.setMinWidth(buttonWidth);

        adapter = new InbuiltCustomizeAdapter(this, MIN_SIZE_DP, MAX_SIZE_DP, MIN_OPACITY, MAX_OPACITY, SEEKBAR_MAX);

        adapterContainer = new FrameLayout(getContext());
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setShape(GradientDrawable.RECTANGLE);
        panelBg.setColor(Color.argb(220, 0, 0, 0));
        panelBg.setCornerRadius(dpToPx(16));
        adapterContainer.setBackground(panelBg);

        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(dpToPx(280), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.END);
        adapterContainer.setLayoutParams(containerParams);
        adapterContainer.setVisibility(View.GONE);

        adapterRecyclerView = new RecyclerView(getContext());
        FrameLayout.LayoutParams recyclerParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        adapterRecyclerView.setLayoutParams(recyclerParams);
        adapterRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        adapterRecyclerView.setAdapter(adapter);

        emptyAdapterText = new TextView(getContext());
        emptyAdapterText.setText(R.string.no_mods_enabled);
        emptyAdapterText.setTextSize(16);
        emptyAdapterText.setTextColor(Color.WHITE);
        emptyAdapterText.setGravity(Gravity.CENTER);
        emptyAdapterText.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24));
        FrameLayout.LayoutParams emptyParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        emptyAdapterText.setLayoutParams(emptyParams);
        emptyAdapterText.setVisibility(View.GONE);

        adapterContainer.addView(adapterRecyclerView);
        adapterContainer.addView(emptyAdapterText);

        FrameLayout panelContainer = findViewById(R.id.adapter_panel_container);
        panelContainer.addView(adapterContainer);
        panelContainer.bringToFront();

        ImageView rootTouch = findViewById(R.id.customize_background);
        if (!showBackground) {
            rootTouch.setImageResource(android.R.color.transparent);
        }
        rootTouch.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (lastSelectedButton != null) {
                    lastSelectedButton.setBackgroundResource(R.drawable.bg_overlay_button);
                    lastSelectedButton = null;
                }
                lastSelectedId = null;
                isLocked = false;
                lockButton.setText("Lock");
                lockButton.setTextColor(Color.WHITE);
                lockButton.setBackground(makeBlackBg());
                lockButton.setBackgroundTintList(null);
            }
            return false;
        });

        InbuiltModSizeStore.getInstance().init(getContext().getApplicationContext());

        InbuiltModManager gridManager = InbuiltModManager.getInstance(getContext());
        if (gridManager.isModAdded(ModIds.MOD_MENU)) addModButton(grid, R.drawable.ic_modmenu, ModIds.MOD_MENU);
        if (gridManager.isModAdded(ModIds.AUTO_SPRINT)) addModButton(grid, R.drawable.as_unpress, ModIds.AUTO_SPRINT);
        if (gridManager.isModAdded(ModIds.QUICK_DROP)) addModButton(grid, R.drawable.q_unpress, ModIds.QUICK_DROP);
        if (gridManager.isModAdded(ModIds.TOGGLE_HUD)) addModButton(grid, R.drawable.f1_unpress, ModIds.TOGGLE_HUD);
        if (gridManager.isModAdded(ModIds.CAMERA_PERSPECTIVE)) addModButton(grid, R.drawable.f5_unpress, ModIds.CAMERA_PERSPECTIVE);
        if (gridManager.isModAdded(ModIds.ZOOM)) addModButton(grid, R.drawable.zoom_unpress, ModIds.ZOOM);

        InbuiltModSizeStore sizeStore = InbuiltModSizeStore.getInstance();
        for (Map.Entry<String, View> e : modButtons.entrySet()) {
            String id = e.getKey();
            View btn = e.getValue();
            float sx = sizeStore.getPositionX(id);
            float sy = sizeStore.getPositionY(id);
            if (sx >= 0f && sy >= 0f) {
                grid.post(() -> {
                    int[] gridLocation = new int[2];
                    grid.getLocationOnScreen(gridLocation);
                    btn.setX(sx - gridLocation[0]);
                    btn.setY(sy - gridLocation[1]);
                });
            }
        }

        for (Map.Entry<String, Integer> e : modSizes.entrySet()) {
            int s = e.getValue();
            e.setValue(clampSize(s <= 0 ? DEFAULT_SIZE_DP : s));
        }

        for (Map.Entry<String, Integer> e : modOpacity.entrySet()) {
            int o = e.getValue();
            e.setValue(clampOpacity(o <= 0 ? DEFAULT_OPACITY : o));
        }

        adapter.submitList(getEnabledMods());

        customizeButton.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.button_pop);
            v.startAnimation(anim);
            v.postDelayed(() -> {
                boolean show = !isAdapterVisible;
                isAdapterVisible = show;
                adapterContainer.post(() -> {
                    float panelW = dpToPx(280);
                    int duration = 200;
                    if (show) {
                        boolean isEmpty = adapter.getItemCount() == 0;
                        adapterRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                        emptyAdapterText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                        adapterContainer.setVisibility(View.VISIBLE);
                        adapterContainer.setTranslationX(panelW);
                        adapterContainer.animate().translationX(0f).setDuration(duration).start();
                        bottomButtons.animate().translationX(-(panelW - dpToPx(200))).setDuration(duration).start();
                    } else {
                        adapterContainer.animate().translationX(panelW).setDuration(duration).withEndAction(() -> adapterContainer.setVisibility(View.GONE)).start();
                        bottomButtons.animate().translationX(0f).setDuration(duration).start();
                    }
                });
            }, 150);
        });

        resetButton.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.button_pop);
            v.startAnimation(anim);
            v.postDelayed(() -> {
                isResetting = true;
                resetAll(grid);
                adapter.submitList(null);
                adapter.submitList(getEnabledMods());
                isResetting = false;
                float panelW = dpToPx(280);
                int duration = 200;
                isAdapterVisible = false;
                adapterContainer.animate().translationX(panelW).setDuration(duration).withEndAction(() -> adapterContainer.setVisibility(View.GONE)).start();
                bottomButtons.animate().translationX(0f).setDuration(duration).start();
            }, 150);
        });

        doneButton.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.button_pop);
            v.startAnimation(anim);
            v.postDelayed(() -> {
                InbuiltModManager manager = InbuiltModManager.getInstance(getContext());
                for (Map.Entry<String, Integer> e : modSizes.entrySet()) {
                    String id = e.getKey();
                    manager.setOverlayButtonSize(id, e.getValue());
                    View btn = modButtons.get(id);
                    if (btn != null) {
                        int[] gridLoc = new int[2];
                        grid.getLocationOnScreen(gridLoc);
                        InbuiltModSizeStore.getInstance().setPositionX(id, btn.getX() + gridLoc[0]);
                        InbuiltModSizeStore.getInstance().setPositionY(id, btn.getY() + gridLoc[1]);
                    }
                }
                for (Map.Entry<String, Integer> e : modOpacity.entrySet()) {
                    manager.setOverlayButtonOpacity(e.getKey(), e.getValue());
                }
                if (modZoomLevels.containsKey(ModIds.ZOOM)) manager.setZoomLevel(modZoomLevels.get(ModIds.ZOOM));
                if (modZoomKeybinds.containsKey(ModIds.ZOOM)) manager.setZoomKeybind(modZoomKeybinds.get(ModIds.ZOOM));
                InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
                if (overlayManager != null) overlayManager.showEnabledOverlays();
                dismiss();
            }, 150);
        });
    }

    private List<InbuiltCustomizeAdapter.Item> getEnabledMods() {
        List<InbuiltCustomizeAdapter.Item> list = new ArrayList<>();
        InbuiltModManager manager = InbuiltModManager.getInstance(getContext());
        if (manager.isModAdded(ModIds.MOD_MENU)) list.add(new InbuiltCustomizeAdapter.Item(ModIds.MOD_MENU, R.drawable.ic_modmenu));
        if (manager.isModAdded(ModIds.AUTO_SPRINT)) list.add(new InbuiltCustomizeAdapter.Item(ModIds.AUTO_SPRINT, R.drawable.as_unpress));
        if (manager.isModAdded(ModIds.QUICK_DROP)) list.add(new InbuiltCustomizeAdapter.Item(ModIds.QUICK_DROP, R.drawable.q_unpress));
        if (manager.isModAdded(ModIds.TOGGLE_HUD)) list.add(new InbuiltCustomizeAdapter.Item(ModIds.TOGGLE_HUD, R.drawable.f1_unpress));
        if (manager.isModAdded(ModIds.CAMERA_PERSPECTIVE)) list.add(new InbuiltCustomizeAdapter.Item(ModIds.CAMERA_PERSPECTIVE, R.drawable.f5_unpress));
        if (manager.isModAdded(ModIds.ZOOM)) {
            list.add(new InbuiltCustomizeAdapter.Item(ModIds.ZOOM, R.drawable.zoom_unpress));
            int savedZoom = manager.getZoomLevel();
            int savedKeybind = manager.getZoomKeybind();
            modZoomLevels.put(ModIds.ZOOM, savedZoom > 0 ? savedZoom : 50);
            modZoomKeybinds.put(ModIds.ZOOM, savedKeybind > 0 ? savedKeybind : KeyEvent.KEYCODE_C);
        }
        return list;
    }

    @Override public int getSizeDp(String id) { return clampSize(modSizes.getOrDefault(id, DEFAULT_SIZE_DP)); }
    @Override public int getOpacity(String id) { return clampOpacity(modOpacity.getOrDefault(id, DEFAULT_OPACITY)); }

    @Override
    public void onSizeChanged(String id, int sizeDp) {
        if (isResetting) return;
        int clamped = clampSize(sizeDp);
        modSizes.put(id, clamped);
        View btn = modButtons.get(id);
        if (btn != null) {
            btn.setMinimumWidth(0);
            btn.setMinimumHeight(0);
            btn.setPadding(0, 0, 0, 0);
            btn.setPaddingRelative(0, 0, 0, 0);
            int px = dpToPx(clamped);
            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) btn.getLayoutParams();
            flp.width = px;
            flp.height = px;
            flp.leftMargin = 0;
            flp.topMargin = 0;
            flp.rightMargin = 0;
            flp.bottomMargin = 0;
            btn.setLayoutParams(flp);
            btn.requestLayout();
            btn.invalidate();
            if (btn instanceof ImageButton) ((ImageButton) btn).setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
    }

    @Override
    public void onOpacityChanged(String id, int opacity) {
        if (isResetting) return;
        int clamped = clampOpacity(opacity);
        modOpacity.put(id, clamped);
        View btn = modButtons.get(id);
        if (btn != null) btn.setAlpha(clamped / 100f);
    }

    @Override public int getZoomLevel(String id) { return modZoomLevels.getOrDefault(id, 50); }
    @Override public void onZoomChanged(String id, int zoomLevel) { modZoomLevels.put(id, zoomLevel); }
    @Override public void onItemClicked(String id) { View btn = modButtons.get(id); if (btn != null) btn.performClick(); }

    @Override
    public boolean getZoomHoldMode(String id) {
        return InbuiltModManager.getInstance(getContext()).getZoomHoldMode();
    }

    @Override
    public void onZoomHoldModeChanged(String id, boolean holdMode) {
        InbuiltModManager.getInstance(getContext()).setZoomHoldMode(holdMode);
    }

    @Override
    public String getKeyName(String id) {
        int keybind = modZoomKeybinds.getOrDefault(id, KeyEvent.KEYCODE_C);
        if (keybind == KeyEvent.KEYCODE_C) return "C";
        String label = KeyEvent.keyCodeToString(keybind);
        return label.startsWith("KEYCODE_") ? label.substring(8) : label;
    }

    @Override
    public void showKeybindDialog(String modId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.zoom_keybind_label);
        builder.setMessage(R.string.zoom_keybind_press);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.dialog_negative_cancel, null);
        AlertDialog kDialog = builder.create();
        GradientDrawable strokeBg = new GradientDrawable();
        strokeBg.setColor(getContext().getResources().getColor(R.color.black, null));
        strokeBg.setStroke(dpToPx(1), getContext().getResources().getColor(R.color.white, null));
        strokeBg.setCornerRadius(dpToPx(16));
        kDialog.getWindow().setBackgroundDrawable(strokeBg);
        kDialog.setOnKeyListener((d, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (!isKeyboardKey(keyCode)) return true;
                if (keyCode == KeyEvent.KEYCODE_BACK) { kDialog.dismiss(); return true; }
                modZoomKeybinds.put(modId, keyCode);
                adapter.notifyDataSetChanged();
                kDialog.dismiss();
                return true;
            }
            return false;
        });
        kDialog.show();
        kDialog.getWindow().getDecorView().post(() -> findAndColorTextViews(kDialog.getWindow().getDecorView(), getContext().getResources().getColor(R.color.white, null)));
    }

    @Override
    public void show() {
        super.show();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        if (overlayManager != null) overlayManager.hideForCustomize();
    }

    @Override
    public void dismiss() {
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        if (overlayManager != null) overlayManager.showAfterCustomize();
        super.dismiss();
    }

    private void findAndColorTextViews(View view, int color) {
        if (view instanceof TextView) ((TextView) view).setTextColor(color);
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) findAndColorTextViews(vg.getChildAt(i), color);
        }
    }

    private boolean isKeyboardKey(int keyCode) {
        return (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) ||
               (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) ||
               keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER;
    }

    private void addModButton(FrameLayout grid, int iconResId, String id) {
        ImageButton btn = new ImageButton(getContext());
        btn.setImageResource(iconResId);
        Bitmap themedBitmap = ThemeManager.getInstance().getOverlayButtonBitmap(id);
        if (themedBitmap != null) btn.setImageBitmap(themedBitmap);
        btn.setBackgroundResource(R.drawable.bg_overlay_button);
        btn.setPadding(0, 0, 0, 0);
        btn.setPaddingRelative(0, 0, 0, 0);
        btn.setMinimumWidth(0);
        btn.setMinimumHeight(0);

        InbuiltModManager manager = InbuiltModManager.getInstance(getContext());
        int savedSizeDp = clampSize(manager.getOverlayButtonSize(id) <= 0 ? DEFAULT_SIZE_DP : manager.getOverlayButtonSize(id));
        int savedOpacity = clampOpacity(manager.getOverlayButtonOpacity(id) <= 0 ? DEFAULT_OPACITY : manager.getOverlayButtonOpacity(id));

        int sizePx = dpToPx(savedSizeDp);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
        btn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        btn.setLayoutParams(lp);

        modSizes.put(id, savedSizeDp);
        modOpacity.put(id, savedOpacity);
        btn.setAlpha(savedOpacity / 100f);
        btn.setX(0f);
        btn.setY(0f);
        modButtons.put(id, btn);

        btn.setOnClickListener(v -> selectButton(v, id));

        btn.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            boolean moved;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        view.bringToFront();
                        dX = event.getRawX() - view.getX();
                        dY = event.getRawY() - view.getY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() - dX;
                        float newY = event.getRawY() - dY;
                        newX = Math.max(0f, Math.min(newX, grid.getWidth() - view.getWidth()));
                        newY = Math.max(0f, Math.min(newY, grid.getHeight() - view.getHeight()));
                        view.setX(newX);
                        view.setY(newY);
                        if (!moved) {
                            moved = true;
                            selectButton(view, id);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            view.performClick();
                        } else {
                            int[] gridLocation = new int[2];
                            grid.getLocationOnScreen(gridLocation);
                            InbuiltModSizeStore.getInstance().setPositionX(id, view.getX() + gridLocation[0]);
                            InbuiltModSizeStore.getInstance().setPositionY(id, view.getY() + gridLocation[1]);
                        }
                        return true;
                }
                return false;
            }
        });

        grid.addView(btn);
    }

    private void resetAll(FrameLayout grid) {
        int defaultSizePx = dpToPx(clampSize(DEFAULT_SIZE_DP));
        for (int i = 0; i < grid.getChildCount(); i++) {
            View c = grid.getChildAt(i);
            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) c.getLayoutParams();
            flp.width = defaultSizePx;
            flp.height = defaultSizePx;
            flp.leftMargin = flp.topMargin = flp.rightMargin = flp.bottomMargin = 0;
            c.setLayoutParams(flp);
            c.setBackgroundResource(R.drawable.bg_overlay_button);
            c.setMinimumWidth(0);
            c.setMinimumHeight(0);
            ((ImageButton) c).setScaleType(ImageView.ScaleType.FIT_CENTER);
            c.setX(0f);
            c.setY(0f);
            c.setAlpha(DEFAULT_OPACITY / 100f);
        }
        for (String key : modSizes.keySet()) modSizes.put(key, clampSize(DEFAULT_SIZE_DP));
        for (String key : modOpacity.keySet()) modOpacity.put(key, DEFAULT_OPACITY);
        lastSelectedButton = null;
        lastSelectedId = null;
        isLocked = false;
        lockButton.setText("Lock");
        lockButton.setTextColor(Color.WHITE);
        lockButton.setBackground(makeBlackBg());
        lockButton.setBackgroundTintList(null);
        isAdapterVisible = false;
        adapterContainer.setVisibility(View.GONE);
        modZoomLevels.clear();
        modZoomLevels.put(ModIds.ZOOM, 50);
        modZoomKeybinds.clear();
        modZoomKeybinds.put(ModIds.ZOOM, KeyEvent.KEYCODE_C);
    }

    private int clampSize(int s) { return Math.max(MIN_SIZE_DP, Math.min(s, MAX_SIZE_DP)); }
    private int clampOpacity(int o) { return Math.max(MIN_OPACITY, Math.min(o, MAX_OPACITY)); }
    private int dpToPx(int dp) { return Math.round(dp * getContext().getResources().getDisplayMetrics().density); }
}
