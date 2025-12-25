package com.origin.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModSizeStore;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.R;

import java.util.HashMap;
import java.util.Map;

public class InbuiltModsCustomizeActivity extends BaseThemedActivity {

    private enum SliderMode { SIZE, OPACITY }

    private View lastSelectedButton;
    private View sliderContainer;
    private SeekBar sizeSeekBar;
    private TextView sliderLabel;
    private MaterialSwitch lockSwitch;
    private final Map<String, Integer> modSizes = new HashMap<>();
    private final Map<String, Integer> modOpacity = new HashMap<>();
    private final Map<String, View> modButtons = new HashMap<>();
    private String lastSelectedId = null;
    private SliderMode currentMode = SliderMode.SIZE;

    private static final int MIN_SIZE_DP = 32;
    private static final int MAX_SIZE_DP = 96;
    private static final int DEFAULT_SIZE_DP = 40;

    private static final int MIN_OPACITY = 20;
    private static final int MAX_OPACITY = 100;
    private static final int DEFAULT_OPACITY = 100;

    private int sizeToProgress(int sizeDp) {
        float t = (sizeDp - MIN_SIZE_DP) / (float) (MAX_SIZE_DP - MIN_SIZE_DP);
        return Math.round(t * sizeSeekBar.getMax());
    }

    private int progressToSize(int progress) {
        float t = progress / (float) sizeSeekBar.getMax();
        return MIN_SIZE_DP + Math.round(t * (MAX_SIZE_DP - MIN_SIZE_DP));
    }

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
        Button sizeButton = findViewById(R.id.size_button);
        Button opacityButton = findViewById(R.id.opacity_button);
        lockSwitch = findViewById(R.id.lock_button);
        FrameLayout grid = findViewById(R.id.inbuilt_buttons_grid);
        sliderContainer = findViewById(R.id.slider_container);
        sizeSeekBar = findViewById(R.id.size_seekbar);
        sliderLabel = findViewById(R.id.slider_label);

        ThemeUtils.applyThemeToSwitch(lockSwitch, this);
        lockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (lastSelectedId != null) {
                InbuiltModSizeStore.getInstance().setLocked(lastSelectedId, isChecked);
            }
        });

        sizeSeekBar.setMax(100);

        View root = findViewById(R.id.customize_background);
        root.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lastSelectedButton = null;
                lastSelectedId = null;
                sliderContainer.setVisibility(View.GONE);
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

        int initialSize = clampSize(DEFAULT_SIZE_DP);
        sizeSeekBar.setProgress(sizeToProgress(initialSize));
        sliderContainer.setVisibility(View.GONE);
        lastSelectedButton = null;
        lastSelectedId = null;

        resetButton.setOnClickListener(v -> resetAll(grid));

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
                    float x = btn.getX();
                    float y = btn.getY();
                    result.putExtra("posx_" + id, x);
                    result.putExtra("posy_" + id, y);
                }
                boolean locked = InbuiltModSizeStore.getInstance().isLocked(id);
            }
            for (Map.Entry<String, Integer> e : modOpacity.entrySet()) {
                String id = e.getKey();
                int opacity = e.getValue();
                result.putExtra("opacity_" + id, opacity);
            }
            setResult(RESULT_OK, result);
            finish();
        });

        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (lastSelectedButton == null || lastSelectedId == null) return;

                if (currentMode == SliderMode.SIZE) {
                    int sizeDp = clampSize(progressToSize(progress));
                    int sizePx = dpToPx(sizeDp);
                    ViewGroup.LayoutParams lp = lastSelectedButton.getLayoutParams();
                    lp.width = sizePx;
                    lp.height = sizePx;
                    lastSelectedButton.setLayoutParams(lp);
                    modSizes.put(lastSelectedId, sizeDp);
                } else if (currentMode == SliderMode.OPACITY) {
                    int opacity = clampOpacity(MIN_OPACITY + progress);
                    lastSelectedButton.setAlpha(opacity / 100f);
                    modOpacity.put(lastSelectedId, opacity);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        sizeButton.setOnClickListener(v -> {
            currentMode = SliderMode.SIZE;
            sliderLabel.setText("Size");
            if (lastSelectedId != null) {
                int sizeDp = modSizes.getOrDefault(lastSelectedId, DEFAULT_SIZE_DP);
                sizeSeekBar.setMax(100);
                sizeSeekBar.setProgress(sizeToProgress(sizeDp));
                sliderContainer.setVisibility(View.VISIBLE);
            }
        });

        opacityButton.setOnClickListener(v -> {
            currentMode = SliderMode.OPACITY;
            sliderLabel.setText("Opacity");
            if (lastSelectedId != null) {
                int opacity = modOpacity.getOrDefault(lastSelectedId, DEFAULT_OPACITY);
                int range = MAX_OPACITY - MIN_OPACITY;
                sizeSeekBar.setMax(range);
                sizeSeekBar.setProgress(opacity - MIN_OPACITY);
                sliderContainer.setVisibility(View.VISIBLE);
            }
        });
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

        int savedOpacity = InbuiltModManager.getInstance(this).getOverlayButtonOpacity(id);
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
            if (lockSwitch != null) {
                lockSwitch.setChecked(InbuiltModSizeStore.getInstance().isLocked(id));
            }

            int sizeDp = modSizes.getOrDefault(id, DEFAULT_SIZE_DP);
            sizeDp = clampSize(sizeDp);
            int sizePx2 = dpToPx(sizeDp);
            ViewGroup.LayoutParams lp2 = v.getLayoutParams();
            lp2.width = sizePx2;
            lp2.height = sizePx2;
            v.setLayoutParams(lp2);
            modSizes.put(id, sizeDp);

            if (currentMode == SliderMode.SIZE) {
                sliderLabel.setText("Size");
                sizeSeekBar.setMax(100);
                sizeSeekBar.setProgress(sizeToProgress(sizeDp));
            } else if (currentMode == SliderMode.OPACITY) {
                sliderLabel.setText("Opacity");
                int opacity = modOpacity.getOrDefault(id, DEFAULT_OPACITY);
                opacity = clampOpacity(opacity);
                int range = MAX_OPACITY - MIN_OPACITY;
                sizeSeekBar.setMax(range);
                sizeSeekBar.setProgress(opacity - MIN_OPACITY);
            }
            sliderContainer.setVisibility(View.VISIBLE);
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
        for (String key : modSizes.keySet()) {
            modSizes.put(key, defaultSizeDp);
        }
        for (String key : modOpacity.keySet()) {
            modOpacity.put(key, defaultOpacity);
        }
        lastSelectedButton = null;
        lastSelectedId = null;
        sliderContainer.setVisibility(View.GONE);
        currentMode = SliderMode.SIZE;
        sliderLabel.setText("Size");
        sizeSeekBar.setMax(100);
        sizeSeekBar.setProgress(sizeToProgress(defaultSizeDp));
    }
}