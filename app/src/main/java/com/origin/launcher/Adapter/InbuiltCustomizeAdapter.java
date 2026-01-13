package com.origin.launcher.Adapter;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;

import java.util.ArrayList;
import java.util.List;

public class InbuiltCustomizeAdapter extends RecyclerView.Adapter<InbuiltCustomizeAdapter.VH> {

    public static class Item {
        public final String id;
        public final int iconRes;
        public int keybind = KeyEvent.KEYCODE_C;
        public Item(String id, int iconRes) {
            this.id = id;
            this.iconRes = iconRes;
        }
    }

    public interface Callback {
        int getSizeDp(String id);
        int getOpacity(String id);
        void onSizeChanged(String id, int sizeDp);
        void onOpacityChanged(String id, int opacity);
        void onItemClicked(String id);
        int getZoomLevel(String id);
        void onZoomChanged(String id, int zoomLevel);
        String getKeyName(int keybind);
        void showKeybindDialog(String id);
    }

    private final List<Item> items = new ArrayList<>();
    private final Callback callback;
    private final int minSizeDp;
    private final int maxSizeDp;
    private final int minOpacity;
    private final int maxOpacity;
    private final int seekMax;

    public InbuiltCustomizeAdapter(
            Callback callback,
            int minSizeDp,
            int maxSizeDp,
            int minOpacity,
            int maxOpacity,
            int seekMax
    ) {
        this.callback = callback;
        this.minSizeDp = minSizeDp;
        this.maxSizeDp = maxSizeDp;
        this.minOpacity = minOpacity;
        this.maxOpacity = maxOpacity;
        this.seekMax = seekMax;
    }

    public void submitList(List<Item> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inbuilt_customize, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Item item = items.get(position);

        h.title.setText(item.id);
        h.icon.setImageResource(item.iconRes);

        h.sizeSeek.setOnSeekBarChangeListener(null);
        h.opacitySeek.setOnSeekBarChangeListener(null);

        h.sizeSeek.setMax(seekMax);
        h.opacitySeek.setMax(seekMax);

        int sizeDp = callback.getSizeDp(item.id);
        int opacity = callback.getOpacity(item.id);

        h.sizeSeek.setProgress(sizeToProgress(sizeDp));
        h.opacitySeek.setProgress(opacityToProgress(opacity));

        h.icon.setOnClickListener(v -> callback.onItemClicked(item.id));
        h.itemView.setOnClickListener(v -> callback.onItemClicked(item.id));

        h.sizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newSize = progressToSize(progress);
                callback.onSizeChanged(item.id, newSize);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        h.opacitySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newOpacity = progressToOpacity(progress);
                callback.onOpacityChanged(item.id, newOpacity);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        if (item.id.equals(ModIds.ZOOM)) {
            h.zoomContainer.setVisibility(View.VISIBLE);
            int currentZoom = callback.getZoomLevel(item.id);
            h.zoomSeek.setProgress(currentZoom);
            h.zoomText.setText(currentZoom + "%");

            h.zoomSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    h.zoomText.setText(progress + "%");
                    if (fromUser) {
                        callback.onZoomChanged(item.id, progress);
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            h.tvKeybindLabel.setVisibility(View.VISIBLE);
            h.tvKeybind.setVisibility(View.VISIBLE);
            h.btnChangeKeybind.setVisibility(View.VISIBLE);
            
            h.tvKeybind.setText(callback.getKeyName(item.keybind));
            h.btnChangeKeybind.setOnClickListener(v -> callback.showKeybindDialog(item.id));
        } else {
            h.zoomContainer.setVisibility(View.GONE);
            h.tvKeybindLabel.setVisibility(View.GONE);
            h.tvKeybind.setVisibility(View.GONE);
            h.btnChangeKeybind.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private int sizeToProgress(int sizeDp) {
        float t = (sizeDp - minSizeDp) / (float) (maxSizeDp - minSizeDp);
        return Math.round(t * seekMax);
    }

    private int progressToSize(int progress) {
        float t = progress / (float) seekMax;
        return minSizeDp + Math.round(t * (maxSizeDp - minSizeDp));
    }

    private int opacityToProgress(int opacity) {
        float t = (opacity - minOpacity) / (float) (maxOpacity - minOpacity);
        return Math.round(t * seekMax);
    }

    private int progressToOpacity(int progress) {
        float t = progress / (float) seekMax;
        return minOpacity + Math.round(t * (maxOpacity - minOpacity));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final ImageButton icon;
        final SeekBar sizeSeek;
        final SeekBar opacitySeek;
        final LinearLayout zoomContainer;
        final SeekBar zoomSeek;
        final TextView zoomText;
        TextView tvKeybindLabel, tvKeybind;
        Button btnChangeKeybind;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.mod_title);
            icon = itemView.findViewById(R.id.mod_icon);
            sizeSeek = itemView.findViewById(R.id.size_seek);
            opacitySeek = itemView.findViewById(R.id.opacity_seek);
            zoomContainer = itemView.findViewById(R.id.config_zoom_container);
            zoomSeek = itemView.findViewById(R.id.seekbar_zoom_level);
            zoomText = itemView.findViewById(R.id.text_zoom_level);
            tvKeybindLabel = itemView.findViewById(R.id.tv_keybind_label);
            tvKeybind = itemView.findViewById(R.id.tv_keybind);
            btnChangeKeybind = itemView.findViewById(R.id.btn_change_keybind);
        }
    }
}