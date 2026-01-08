package com.origin.launcher;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.origin.launcher.Adapter.InbuiltCustomizeAdapter;
import com.origin.launcher.Adapter.InbuiltCustomizeAdapter.Item;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModSizeStore;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InbuiltModsCustomizeActivity extends BaseThemedActivity implements InbuiltCustomizeAdapter.Callback {

    private View lastSelectedButton;
    private MaterialSwitch lockSwitch;

    private final Map<String, Integer> modSizes = new HashMap<>();
    private final Map<String, Integer> modOpacity = new HashMap<>();
    private final Map<String, View> modButtons = new HashMap<>();
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
    private FrameLayout adapterContainer;
    private TextView emptyAdapterText;

    private int clampSize(int s) {
        return Math.max(MIN_SIZE_DP, Math.min(s, MAX_SIZE_DP));
    }

    private int clampOpacity(int o) {
        return Math.max(MIN_OPACITY, Math.min(o, MAX_OPACITY));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbuilt_mods_customize);

        Button resetButton = findViewById(R.id.reset_button);
        Button doneButton = findViewById(R.id.done_button);
        Button customizeButton = findViewById(R.id.opacity_button);

        lockSwitch = findViewById(R.id.lock_button);
        FrameLayout grid = findViewById(R.id.inbuilt_buttons_grid);

        customizeButton.setText("Customize");

        GradientDrawable blackBg = new GradientDrawable();
        blackBg.setShape(GradientDrawable.RECTANGLE);
        blackBg.setColor(Color.BLACK);
        blackBg.setCornerRadius(dpToPx(12));

        resetButton.setBackground(blackBg);
        customizeButton.setBackground(blackBg);
        doneButton.setBackground(blackBg);

        int padding8dp = dpToPx(8);
        int padding16dp = dpToPx(16);
        int padding24dp = dpToPx(24);

        resetButton.setPadding(padding24dp, padding8dp, padding24dp, padding8dp);
        customizeButton.setPadding(padding16dp, padding8dp, padding16dp, padding8dp);
        doneButton.setPadding(padding24dp, padding8dp, padding24dp, padding8dp);

        adapter = new InbuiltCustomizeAdapter(
                this,
                MIN_SIZE_DP, MAX_SIZE_DP,
                MIN_OPACITY, MAX_OPACITY,
                SEEKBAR_MAX
        );

        adapterContainer = new FrameLayout(this);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                dpToPx(240),
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        containerParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        containerParams.rightMargin = dpToPx(16);
        containerParams.topMargin = dpToPx(16);
        containerParams.bottomMargin = dpToPx(96);
        adapterContainer.setLayoutParams(containerParams);
        adapterContainer.setVisibility(View.GONE);

        adapterRecyclerView = new RecyclerView(this);
        FrameLayout.LayoutParams recyclerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        adapterRecyclerView.setLayoutParams(recyclerParams);
        adapterRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapterRecyclerView.setAdapter(adapter);

        emptyAdapterText = new TextView(this);
        emptyAdapterText.setText("No mods enabled

Add mods from the
main menu first");
        emptyAdapterText.setTextSize(16);
        emptyAdapterText.setTextColor(Color.WHITE);
        emptyAdapterText.setGravity(Gravity.CENTER);
        emptyAdapterText.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24));
        FrameLayout.LayoutParams emptyParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        emptyAdapterText.setLayoutParams(emptyParams);
        emptyAdapterText.setVisibility(View.GONE);

        adapterContainer.addView(adapterRecyclerView);
        adapterContainer.addView(emptyAdapterText);

        View bg = findViewById(R.id.customize_background);
        if (bg instanceof ViewGroup) {
            ((ViewGroup) bg).addView(adapterContainer);
        } else {
            View root = findViewById(android.R.id.content);
            if (root instanceof ViewGroup) ((ViewGroup) root).addView(adapterContainer);
        }

        ThemeUtils.applyThemeToSwitch(lockSwitch, this);
        lockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (lastSelectedId != null) {
                InbuiltModSizeStore.getInstance().setLocked(lastSelectedId, isChecked);
            }
        });

        View rootTouch = findViewById(R.id.customize_background);
        rootTouch.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lastSelectedButton = null;
                lastSelectedId = null;
                lockSwitch.setChecked(false);
            }
            return false;
        });

        InbuiltModSizeStore.getInstance().init(getApplicationContext());

        addModButton(grid, R.drawable.ic_sprint, ModIds.AUTO_SPRINT);
        addModButton(grid, R.drawable.ic_quick_drop, ModIds.QUICK_DROP);
        addModButton(grid, R.drawable.ic_hud, ModIds.TOGGLE_HUD);
        addModButton(grid, R.drawable.ic_camera, ModIds.CAMERA_PERSPECTIVE);

        InbuiltModSizeStore sizeStore = InbuiltModSizeStore.getInstance();
        for (Map.Entry<String, View> e : modButtons.entrySet()) {
            String id = e.getKey();
            View btn = e.getValue();
            float sx = sizeStore.getPositionX(id);
            float sy = sizeStore.getPositionY(id);
            if (sx >= 0f && sy >= 0f) {
                btn.setX(sx);
                btn.setY(sy);
            }
        }

        for (Map.Entry<String, Integer> e : modSizes.entrySet()) {
            int s = e.getValue();
            s = clampSize(s <= 0 ? DEFAULT_SIZE_DP : s);
            e.setValue(s);
        }

        for (Map.Entry<String, Integer> e : modOpacity.entrySet()) {
            int o = e.getValue();
            o = clampOpacity(o <= 0 ? DEFAULT_OPACITY : o);
            e.setValue(o);
        }

        adapter.submitList(getEnabledMods());

        customizeButton.setOnClickListener(v -> {
            isAdapterVisible = !isAdapterVisible;
            adapterContainer.setVisibility(isAdapterVisible ? View.VISIBLE : View.GONE);
            
            boolean isEmpty = adapter.getItemCount() == 0;
            if (isAdapterVisible) {
                adapterRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                emptyAdapterText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });

        resetButton.setOnClickListener(v -> {
            resetAll(grid);
            adapter.notifyDataSetChanged();
        });

        doneButton.setOnClickListener(v -> {
            Intent result = new Intent();
            InbuiltModManager manager = InbuiltModManager.getInstance(this);

            for (Map.Entry<String, Integer> e : modSizes.entrySet()) {
                String id = e.getKey();
                int sizeDp = e.getValue();
                manager.setOverlayButtonSize(id, sizeDp);
                result.putExtra("size_" + id, sizeDp);

                View btn = modButtons.get(id);
                if (btn != null) {
                    result.putExtra("posx_" + id, btn.getX());
                    result.putExtra("posy_" + id, btn.getY());
                }
            }

            for (Map.Entry<String, Integer> e : modOpacity.entrySet()) {
                String id = e.getKey();
                int opacity = e.getValue();
                result.putExtra("opacity_" + id, opacity);
            }

            setResult(RESULT_OK, result);
            finish();
        });
    }

    @NonNull
    private List<InbuiltCustomizeAdapter.Item> getEnabledMods() {
        List<InbuiltCustomizeAdapter.Item> list = new ArrayList<>();
        InbuiltModManager manager = InbuiltModManager.getInstance(this);
        
        if (manager.isModAdded(ModIds.AUTO_SPRINT))
            list.add(new InbuiltCustomizeAdapter.Item(ModIds.AUTO_SPRINT, R.drawable.ic_sprint));
        if (manager.isModAdded(ModIds.QUICK_DROP))
            list.add(new InbuiltCustomizeAdapter.Item(ModIds.QUICK_DROP, R.drawable.ic_quick_drop));
        if (manager.isModAdded(ModIds.TOGGLE_HUD))
            list.add(new InbuiltCustomizeAdapter.Item(ModIds.TOGGLE_HUD, R.drawable.ic_hud));
        if (manager.isModAdded(ModIds.CAMERA_PERSPECTIVE))
            list.add(new InbuiltCustomizeAdapter.Item(ModIds.CAMERA_PERSPECTIVE, R.drawable.ic_camera));
        
        return list;
    }

    @Override
    public int getSizeDp(String id) {
        return clampSize(modSizes.getOrDefault(id, DEFAULT_SIZE_DP));
    }

    @Override
    public int getOpacity(String id) {
        return clampOpacity(modOpacity.getOrDefault(id, DEFAULT_OPACITY));
    }

    @Override
    public void onSizeChanged(String id, int sizeDp) {
        int clamped = clampSize(sizeDp);
        modSizes.put(id, clamped);

        View btn = modButtons.get(id);
        if (btn != null) {
            int px = dpToPx(clamped);
            ViewGroup.LayoutParams lp = btn.getLayoutParams();
            lp.width = px;
            lp.height = px;
            btn.setLayoutParams(lp);
        }
    }

    @Override
    public void onOpacityChanged(String id, int opacity) {
        int clamped = clampOpacity(opacity);
        modOpacity.put(id, clamped);

        View btn = modButtons.get(id);
        if (btn != null) {
            btn.setAlpha(clamped / 100f);
        }
    }

    @Override
    public void onItemClicked(String id) {
        View btn = modButtons.get(id);
        if (btn != null) btn.performClick();
    }

    private void addModButton(FrameLayout grid, int iconResId, String id) {
        ImageButton btn = new ImageButton(this);
        btn.setImageResource(iconResId);
        btn.setBackgroundResource(R.drawable.bg_overlay_button);
        btn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        InbuiltModManager manager = InbuiltModManager.getInstance(this);

        int savedSizeDp = manager.getOverlayButtonSize(id);
        if (savedSizeDp <= 0) savedSizeDp = DEFAULT_SIZE_DP;
        savedSizeDp = clampSize(savedSizeDp);
        int sizePx = dpToPx(savedSizeDp);

        int savedOpacity = manager.getOverlayButtonOpacity(id);
        if (savedOpacity <= 0) savedOpacity = DEFAULT_OPACITY;
        savedOpacity = clampOpacity(savedOpacity);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
        lp.leftMargin = dpToPx(8);
        lp.topMargin = dpToPx(8);
        btn.setLayoutParams(lp);

        modSizes.put(id, savedSizeDp);
        modOpacity.put(id, savedOpacity);
        btn.setAlpha(savedOpacity / 100f);

        btn.setX(0f);
        btn.setY(0f);

        modButtons.put(id, btn);

        btn.setOnClickListener(v -> {
            lastSelectedButton = v;
            lastSelectedId = id;
            lockSwitch.setChecked(InbuiltModSizeStore.getInstance().isLocked(id));
        });

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

                        View bg = findViewById(R.id.customize_background);
                        float left = 0f;
                        float top = 0f;
                        float right = bg.getWidth() - view.getWidth();
                        float bottom = bg.getHeight() - view.getHeight();

                        if (newX < left) newX = left;
                        if (newX > right) newX = right;
                        if (newY < top) newY = top;
                        if (newY > bottom) newY = bottom;

                        view.setX(newX);
                        view.setY(newY);
                        moved = true;
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            view.performClick();
                        } else {
                            InbuiltModSizeStore store = InbuiltModSizeStore.getInstance();
                            store.setPositionX(id, view.getX());
                            store.setPositionY(id, view.getY());
                        }
                        return true;
                }
                return false;
            }
        });

        grid.addView(btn);
    }

    private void resetAll(FrameLayout grid) {
        int defaultSizeDp = clampSize(DEFAULT_SIZE_DP);
        int defaultSizePx = dpToPx(defaultSizeDp);
        int defaultOpacity = DEFAULT_OPACITY;

        for (int i = 0; i < grid.getChildCount(); i++) {
            View c = grid.getChildAt(i);
            ViewGroup.LayoutParams lp = c.getLayoutParams();
            lp.width = defaultSizePx;
            lp.height = defaultSizePx;
            c.setLayoutParams(lp);
            c.setX(0f);
            c.setY(0f);
            c.setAlpha(defaultOpacity / 100f);
        }

        for (String key : modSizes.keySet()) modSizes.put(key, defaultSizeDp);
        for (String key : modOpacity.keySet()) modOpacity.put(key, defaultOpacity);

        lastSelectedButton = null;
        lastSelectedId = null;
        lockSwitch.setChecked(false);

        isAdapterVisible = false;
        adapterContainer.setVisibility(View.GONE);
    }
}