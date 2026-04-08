package com.origin.launcher.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.Adapter.ModMenuAdapter;
import com.origin.launcher.manager.ThemeManager;
import com.origin.launcher.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModMenuDialog {

    private final Activity activity;
    private Dialog dialog;
    private RecyclerView recyclerView;
    private InbuiltModManager modManager;
    private List<ModMenuAdapter.ModEntry> utilityMods;
    private List<ModMenuAdapter.ModEntry> statsMods;
    private List<ModMenuAdapter.ModEntry> qolMods;

    public ModMenuDialog(Activity activity) {
        this.activity = activity;
    }

    private void animatePop(View view) {
        Animation anim = AnimationUtils.loadAnimation(activity, R.anim.button_pop);
        view.startAnimation(anim);
    }

    private void dismissWithAnimation() {
        View animTarget = dialog.getWindow().getDecorView();
        ScaleAnimation scale = new ScaleAnimation(
            1f, 0.85f, 1f, 0.85f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        AlphaAnimation alpha = new AlphaAnimation(1f, 0f);
        android.view.animation.AnimationSet set = new android.view.animation.AnimationSet(true);
        set.addAnimation(scale);
        set.addAnimation(alpha);
        set.setDuration(180);
        set.setInterpolator(new android.view.animation.AccelerateInterpolator());
        set.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {}
            @Override public void onAnimationRepeat(Animation a) {}
            @Override public void onAnimationEnd(Animation a) { dialog.dismiss(); }
        });
        animTarget.startAnimation(set);
    }

    public void show() {
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_mod_menu);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.80),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );

        ThemeManager themeManager = ThemeManager.getInstance();
        int surfaceColor = themeManager.getColor("surface");
        int outlineColor = themeManager.getColor("outline");
        int primaryColor = themeManager.getColor("primary");
        int onSurfaceColor = themeManager.getColor("onSurface");
        int onSurfaceVariantColor = themeManager.getColor("onSurfaceVariant");

        View contentRoot = dialog.getWindow().getDecorView().findViewById(android.R.id.content);

        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setShape(GradientDrawable.RECTANGLE);
        dialogBg.setColor(surfaceColor);
        dialogBg.setStroke((int) (0 * activity.getResources().getDisplayMetrics().density), outlineColor);
        dialogBg.setCornerRadius(20 * activity.getResources().getDisplayMetrics().density);

        View dialogRoot = (View) contentRoot.getParent();
        if (dialogRoot != null) {
            dialogRoot.setBackground(dialogBg);
        }

        View divider = dialog.findViewById(R.id.mod_menu_divider);
        if (divider != null) {
            divider.setBackgroundColor(outlineColor);
        }

        TextView titleText = dialog.findViewById(R.id.mod_menu_title);
        if (titleText != null) {
            titleText.setTextColor(primaryColor);
        }

        ImageView btnBack = dialog.findViewById(R.id.btn_back);
        ImageView btnWrench = dialog.findViewById(R.id.btn_wrench);

        if (btnBack != null) btnBack.setColorFilter(onSurfaceColor);
        if (btnWrench != null) btnWrench.setColorFilter(onSurfaceVariantColor);

        btnBack.setOnClickListener(v -> {
            animatePop(btnBack);
            btnBack.postDelayed(this::dismissWithAnimation, 150);
        });

        btnWrench.setOnClickListener(v -> {
            animatePop(btnWrench);
            btnWrench.postDelayed(() -> {
                dismissWithAnimation();
                new InbuiltModsCustomizeDialog(activity, false).show();
            }, 150);
        });

        int modMenuOpacity = InbuiltModManager.getInstance(activity).getModMenuOpacity();
        dialog.getWindow().getDecorView().setAlpha(modMenuOpacity / 100f);

        modManager = InbuiltModManager.getInstance(activity);

        utilityMods = new ArrayList<>();
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.QUICK_DROP, activity.getString(R.string.inbuilt_mod_quick_drop)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.HOTBAR_ONE, activity.getString(R.string.inbuilt_mod_hotbarone)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.HOTBAR_TWO,   activity.getString(R.string.inbuilt_mod_hotbartwo)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.HOTBAR_THREE, activity.getString(R.string.inbuilt_mod_hotbarthree)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.HOTBAR_FOUR,  activity.getString(R.string.inbuilt_mod_hotbarfour)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.HOTBAR_FIVE,  activity.getString(R.string.inbuilt_mod_hotbarfive)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.HOTBAR_SIX,   activity.getString(R.string.inbuilt_mod_hotbarsix)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.HOTBAR_SEVEN, activity.getString(R.string.inbuilt_mod_hotbarseven)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.HOTBAR_EIGHT, activity.getString(R.string.inbuilt_mod_hotbareight)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.HOTBAR_NINE,  activity.getString(R.string.inbuilt_mod_hotbarnine)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.CAMERA_PERSPECTIVE, activity.getString(R.string.inbuilt_mod_camera)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.TOGGLE_HUD, activity.getString(R.string.inbuilt_mod_hud)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.AUTO_SPRINT, activity.getString(R.string.inbuilt_mod_autosprint)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.ZOOM, activity.getString(R.string.inbuilt_mod_zoom)));
        // Resource pack / visual utility mods
        utilityMods.add(new ModMenuAdapter.ModEntry("custom_cross_hair", "Custom CrossHair"));
        utilityMods.add(new ModMenuAdapter.ModEntry("white_block_outline", "White Block Outline"));
        utilityMods.add(new ModMenuAdapter.ModEntry("java_clouds", "Java Fancy Clouds"));
        utilityMods.add(new ModMenuAdapter.ModEntry("java_cubemap", "Java Cubemap"));
        utilityMods.add(new ModMenuAdapter.ModEntry("classic_skins", "Classic Vanilla Skins"));
        utilityMods.add(new ModMenuAdapter.ModEntry("xelo_title", "Xelo Title"));
        utilityMods.add(new ModMenuAdapter.ModEntry("double_tppview", "2x TPP View"));

        statsMods = new ArrayList<>();
        statsMods.add(new ModMenuAdapter.ModEntry(ModIds.FPS_DISPLAY, activity.getString(R.string.inbuilt_mod_fps_display)));
        statsMods.add(new ModMenuAdapter.ModEntry(ModIds.CPS_DISPLAY, activity.getString(R.string.inbuilt_mod_cps_display)));

        qolMods = new ArrayList<>();
        qolMods.add(new ModMenuAdapter.ModEntry(ModIds.THIRD_PERSON_NAMETAG, activity.getString(R.string.inbuilt_mod_nametag)));
        // Performance / visual QoL mods
        qolMods.add(new ModMenuAdapter.ModEntry("Nohurtcam", "No Hurt Cam"));
        qolMods.add(new ModMenuAdapter.ModEntry("Nofog", "No Fog"));
        qolMods.add(new ModMenuAdapter.ModEntry("better_brightness", "Better Brightness"));
        qolMods.add(new ModMenuAdapter.ModEntry("particles_disabler", "Particles Disabler"));
        qolMods.add(new ModMenuAdapter.ModEntry("no_flipbook_animations", "No Flipbook Animations"));
        qolMods.add(new ModMenuAdapter.ModEntry("no_shadows", "No Shadows"));
        qolMods.add(new ModMenuAdapter.ModEntry("no_pumpkin_overlay", "No Pumpkin Overlay"));
        qolMods.add(new ModMenuAdapter.ModEntry("no_spyglass_overlay", "No Spyglass Overlay"));
        qolMods.add(new ModMenuAdapter.ModEntry("no_eating_animation", "No Eating Animation"));
        qolMods.add(new ModMenuAdapter.ModEntry("no_bow_animation", "No Bow Animation"));
        qolMods.add(new ModMenuAdapter.ModEntry("portal_optimizer", "Portal Optimizer"));
        qolMods.add(new ModMenuAdapter.ModEntry("psm", "Particle Single Mapping"));

        recyclerView = dialog.findViewById(R.id.mod_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(new ModMenuAdapter(utilityMods, modManager));

        LinearLayout tabUtility = dialog.findViewById(R.id.tab_utility);
        LinearLayout tabQol = dialog.findViewById(R.id.tab_qol);
        LinearLayout tabStats = dialog.findViewById(R.id.tab_stats);

        applyTabColors(tabUtility, primaryColor, true);
        applyTabColors(tabQol, onSurfaceColor, false);
        applyTabColors(tabStats, onSurfaceColor, false);

        LinearLayout[] tabs = {tabUtility, tabQol, tabStats};
        List<List<ModMenuAdapter.ModEntry>> tabData = Arrays.asList(utilityMods, qolMods, statsMods);

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> {
                animatePop(tabs[index]);
                for (int j = 0; j < tabs.length; j++) {
                    tabs[j].setSelected(false);
                    applyTabColors(tabs[j], onSurfaceColor, false);
                }
                tabs[index].setSelected(true);
                applyTabColors(tabs[index], primaryColor, true);
                recyclerView.setAdapter(new ModMenuAdapter(tabData.get(index), modManager));
            });
        }

        tabUtility.setSelected(true);

        dialog.show();

        View animTarget = dialog.getWindow().getDecorView();
        ScaleAnimation scale = new ScaleAnimation(
            0.85f, 1f, 0.85f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
        android.view.animation.AnimationSet set = new android.view.animation.AnimationSet(true);
        set.addAnimation(scale);
        set.addAnimation(alpha);
        set.setDuration(220);
        set.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animTarget.startAnimation(set);
    }

    private void applyTabColors(LinearLayout tab, int textColor, boolean selected) {
        if (tab == null) return;
        for (int i = 0; i < tab.getChildCount(); i++) {
            View child = tab.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(textColor);
            } else if (child instanceof ImageView) {
                ((ImageView) child).clearColorFilter();
            }
        }
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public void hide() {
        if (isShowing()) dismissWithAnimation();
    }
}