package com.origin.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.Adapter.ModMenuAdapter;
import com.origin.launcher.R;

import java.util.ArrayList;
import java.util.List;

public class ModMenuDialog {

    private final Activity activity;
    private Dialog dialog;

    public ModMenuDialog(Activity activity) {
        this.activity = activity;
    }

    public void show() {
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_mod_menu);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.85),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );

        InbuiltModManager modManager = InbuiltModManager.getInstance(activity);

        List<ModMenuAdapter.ModEntry> mods = new ArrayList<>();
        mods.add(new ModMenuAdapter.ModEntry(ModIds.QUICK_DROP,        activity.getString(R.string.inbuilt_mod_quick_drop)));
        mods.add(new ModMenuAdapter.ModEntry(ModIds.CAMERA_PERSPECTIVE, activity.getString(R.string.inbuilt_mod_camera)));
        mods.add(new ModMenuAdapter.ModEntry(ModIds.TOGGLE_HUD,        activity.getString(R.string.inbuilt_mod_hud)));
        mods.add(new ModMenuAdapter.ModEntry(ModIds.AUTO_SPRINT,       activity.getString(R.string.inbuilt_mod_autosprint)));
        mods.add(new ModMenuAdapter.ModEntry(ModIds.ZOOM,              activity.getString(R.string.inbuilt_mod_zoom)));
        mods.add(new ModMenuAdapter.ModEntry(ModIds.FPS_DISPLAY,       activity.getString(R.string.inbuilt_mod_fps_display)));
        mods.add(new ModMenuAdapter.ModEntry(ModIds.CPS_DISPLAY,       activity.getString(R.string.inbuilt_mod_cps_display)));

        RecyclerView recyclerView = dialog.findViewById(R.id.mod_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(new ModMenuAdapter(mods, modManager));

        dialog.show();
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public void hide() {
        if (isShowing()) dialog.dismiss();
    }
}