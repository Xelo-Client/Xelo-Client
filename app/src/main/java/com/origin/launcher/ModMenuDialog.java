package com.origin.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.Adapter.ModMenuAdapter;
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

    public void show() {
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_mod_menu);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.70),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );

        ImageView btnBack = dialog.findViewById(R.id.btn_back);
        ImageView btnWrench = dialog.findViewById(R.id.btn_wrench);

        btnBack.setOnClickListener(v -> dialog.dismiss());
        btnWrench.setOnClickListener(v -> {});

        modManager = InbuiltModManager.getInstance(activity);

        utilityMods = new ArrayList<>();
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.QUICK_DROP, activity.getString(R.string.inbuilt_mod_quick_drop)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.CAMERA_PERSPECTIVE, activity.getString(R.string.inbuilt_mod_camera)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.TOGGLE_HUD, activity.getString(R.string.inbuilt_mod_hud)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.AUTO_SPRINT, activity.getString(R.string.inbuilt_mod_autosprint)));
        utilityMods.add(new ModMenuAdapter.ModEntry(ModIds.ZOOM, activity.getString(R.string.inbuilt_mod_zoom)));

        statsMods = new ArrayList<>();
        statsMods.add(new ModMenuAdapter.ModEntry(ModIds.FPS_DISPLAY, activity.getString(R.string.inbuilt_mod_fps_display)));
        statsMods.add(new ModMenuAdapter.ModEntry(ModIds.CPS_DISPLAY, activity.getString(R.string.inbuilt_mod_cps_display)));

        qolMods = new ArrayList<>();

        recyclerView = dialog.findViewById(R.id.mod_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(new ModMenuAdapter(utilityMods, modManager));

        TextView tabUtility = dialog.findViewById(R.id.tab_utility);
        TextView tabQol = dialog.findViewById(R.id.tab_qol);
        TextView tabStats = dialog.findViewById(R.id.tab_stats);

        View[] tabs = {tabUtility, tabQol, tabStats};
        List<List<ModMenuAdapter.ModEntry>> tabData = Arrays.asList(utilityMods, qolMods, statsMods);

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> {
                for (View t : tabs) {
                    ((TextView) t).setTextColor(activity.getResources().getColor(R.color.onSurface, activity.getTheme()));
                    t.setSelected(false);
                }
                ((TextView) tabs[index]).setTextColor(activity.getResources().getColor(R.color.primary, activity.getTheme()));
                tabs[index].setSelected(true);
                recyclerView.setAdapter(new ModMenuAdapter(tabData.get(index), modManager));
            });
        }

        dialog.show();
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public void hide() {
        if (isShowing()) dialog.dismiss();
    }
}