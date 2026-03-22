package com.origin.launcher.Adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.overlay.InbuiltOverlayManager;
import com.origin.launcher.R;
import com.origin.launcher.ThemeManager;

import java.util.List;

public class ModMenuAdapter extends RecyclerView.Adapter<ModMenuAdapter.ViewHolder> {

    public static class ModEntry {
        public final String modId;
        public final String modName;

        public ModEntry(String modId, String modName) {
            this.modId = modId;
            this.modName = modName;
        }
    }

    private final List<ModEntry> mods;
    private final InbuiltModManager modManager;

    public ModMenuAdapter(List<ModEntry> mods, InbuiltModManager modManager) {
        this.mods = mods;
        this.modManager = modManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        android.content.Context themedContext = new androidx.appcompat.view.ContextThemeWrapper(
            parent.getContext(),
            com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar
        );
        View view = LayoutInflater.from(themedContext)
                .inflate(R.layout.item_mod_toggle_card, parent, false);

        int horizontalMargin = (int) (2 * parent.getResources().getDisplayMetrics().density);
        int verticalMargin = (int) (1 * parent.getResources().getDisplayMetrics().density);

        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
        view.setLayoutParams(params);

        MaterialSwitch sw = view.findViewById(R.id.mod_switch);
        sw.setTextOn("");
        sw.setTextOff("");
        sw.setShowText(false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModEntry entry = mods.get(position);
        holder.modName.setText(entry.modName);

        boolean isChecked = modManager.isModAdded(entry.modId);
        holder.modSwitch.setOnCheckedChangeListener(null);
        holder.modSwitch.setChecked(isChecked);
        applySwitchTheme(holder.modSwitch, isChecked);

        holder.modSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) {
                modManager.addMod(entry.modId);
                modManager.applyAllPatches();
            } else {
                modManager.removeAllPatches();
                modManager.removeMod(entry.modId);
            }
            applySwitchTheme(holder.modSwitch, checked);
            InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
            if (overlayManager != null) {
                overlayManager.showEnabledOverlays();
            }
            holder.modSwitch.setChecked(modManager.isModAdded(entry.modId));
        });
    }

    private void applySwitchTheme(MaterialSwitch sw, boolean isChecked) {
        int primaryColor = ThemeManager.getInstance().getColor("primary");
        int uncheckedColor = Color.parseColor("#888888");
        sw.setThumbTintList(ColorStateList.valueOf(isChecked ? primaryColor : uncheckedColor));
        sw.setTrackTintList(ColorStateList.valueOf(isChecked ? adjustAlpha(primaryColor, 0.5f) : adjustAlpha(uncheckedColor, 0.5f)));
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    @Override
    public int getItemCount() {
        return mods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView modName;
        MaterialSwitch modSwitch;

        ViewHolder(View view) {
            super(view);
            modName = view.findViewById(R.id.mod_name);
            modSwitch = view.findViewById(R.id.mod_switch);
        }
    }
}
