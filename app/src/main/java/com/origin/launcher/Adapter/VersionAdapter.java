package com.origin.launcher.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.origin.launcher.R;
import com.origin.launcher.versions.GameVersion;
import java.util.ArrayList;
import java.util.List;

public class VersionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private List<GameVersion> versions = new ArrayList<>();
    private OnVersionSelectedListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;
    
    public interface OnVersionSelectedListener {
        void onVersionSelected(GameVersion version);
    }
    
    public VersionAdapter(List<GameVersion> versions, OnVersionSelectedListener listener) {
        this.versions = new ArrayList<>(versions);
        this.listener = listener;
    }
    
    public void setSelectedPosition(int position) {
        int prevPosition = selectedPosition;
        selectedPosition = position;
        if (prevPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(prevPosition);
        }
        notifyItemChanged(selectedPosition);
    }
    
    public int getSelectedPosition() {
        return selectedPosition;
    }
    
    @Override
    public int getItemViewType(int position) {
        return 0;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_version, parent, false);
        return new VersionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        VersionViewHolder versionHolder = (VersionViewHolder) holder;
        GameVersion version = versions.get(position);
        
        versionHolder.tvVersionName.setText(version.displayName);
        versionHolder.tvPackageName.setText(version.packageName);
        
        if (position == selectedPosition) {
            versionHolder.itemView.setBackgroundColor(0xFF4CAF50);
            versionHolder.tvVersionName.setTextColor(0xFFFFFFFF);
            versionHolder.tvPackageName.setTextColor(0xFFFFFFFF);
        } else {
            versionHolder.itemView.setBackgroundColor(0x00000000);
            versionHolder.tvVersionName.setTextColor(0xFF000000);
            versionHolder.tvPackageName.setTextColor(0xFF666666);
        }
        
        holder.itemView.setOnClickListener(v -> {
            setSelectedPosition(position);
            if (listener != null) {
                listener.onVersionSelected(version);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return versions.size();
    }
    
    public void updateVersions(List<GameVersion> newVersions) {
        this.versions = new ArrayList<>(newVersions);
        notifyDataSetChanged();
    }
    
    static class VersionViewHolder extends RecyclerView.ViewHolder {
        TextView tvVersionName;
        TextView tvPackageName;
        
        VersionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVersionName = itemView.findViewById(R.id.tv_version_name_item);
            tvPackageName = itemView.findViewById(R.id.tv_package_name);
        }
    }
}