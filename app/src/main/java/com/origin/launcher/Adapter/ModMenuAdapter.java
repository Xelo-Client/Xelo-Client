package com.origin.launcher.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.overlay.InbuiltOverlayManager;
import com.origin.launcher.R;

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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mod_toggle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModEntry entry = mods.get(position);
        holder.modName.setText(entry.modName);
        holder.modSwitch.setOnCheckedChangeListener(null);
        holder.modSwitch.setChecked(modManager.isModAdded(entry.modId));
        holder.modSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
            if (overlayManager != null) {
                overlayManager.toggleMod(entry.modId);
            }
            holder.modSwitch.setChecked(modManager.isModAdded(entry.modId));
        });
    }

    @Override
    public int getItemCount() {
        return mods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView modName;
        SwitchMaterial modSwitch;

        ViewHolder(View view) {
            super(view);
            modName = view.findViewById(R.id.mod_name);
            modSwitch = view.findViewById(R.id.mod_switch);
        }
    }
}