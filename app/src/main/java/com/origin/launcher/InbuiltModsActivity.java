package com.origin.launcher;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModSizeStore;
import com.origin.launcher.Launcher.inbuilt.model.InbuiltMod;
import com.origin.launcher.Adapter.InbuiltModsAdapter;
import com.origin.launcher.animation.DynamicAnim;
import android.content.Intent;

import java.util.List;

public class InbuiltModsActivity extends BaseThemedActivity {

    private RecyclerView recyclerView;
    private InbuiltModsAdapter adapter;
    private InbuiltModManager modManager;
    private TextView emptyText;
    private static final int REQ_CUSTOMIZE_INBUILT = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbuilt_mods);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            DynamicAnim.applyPressScaleRecursively(root);
        }

        modManager = InbuiltModManager.getInstance(this);
        InbuiltModSizeStore.getInstance().init(getApplicationContext());
        setupViews();
        loadMods();
    }

    private void setupViews() {
        ImageButton closeButton = findViewById(R.id.close_inbuilt_button);
        closeButton.setOnClickListener(v -> finish());
        DynamicAnim.applyPressScale(closeButton);
        ImageButton customiseButton = findViewById(R.id.customise_inbuilt_mod_button);
        customiseButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, InbuiltModsCustomizeActivity.class);
            startActivityForResult(intent, REQ_CUSTOMIZE_INBUILT);
        });
        DynamicAnim.applyPressScale(customiseButton);

        recyclerView = findViewById(R.id.inbuilt_mods_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        emptyText = findViewById(R.id.empty_inbuilt_text);

        adapter = new InbuiltModsAdapter(modManager);
        adapter.setOnToggleClickListener((mod, enable) -> {
            if (enable) {
                modManager.addMod(mod.getId());
                Toast.makeText(this,
                        getString(R.string.inbuilt_mod_added, mod.getName()),
                        Toast.LENGTH_SHORT).show();
            } else {
                modManager.removeMod(mod.getId());
                Toast.makeText(this,
                        getString(R.string.inbuilt_mod_removed, mod.getName()),
                        Toast.LENGTH_SHORT).show();
            }
            loadMods();
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadMods() {
        List<InbuiltMod> mods = modManager.getAllMods(this);
        adapter.updateMods(mods);
        emptyText.setVisibility(mods.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(mods.isEmpty() ? View.GONE : View.VISIBLE);
        recyclerView.post(() -> DynamicAnim.staggerRecyclerChildren(recyclerView));
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_CUSTOMIZE_INBUILT || resultCode != RESULT_OK || data == null) {
            return;
        }

        List<InbuiltMod> mods = modManager.getAllMods(this);
        InbuiltModSizeStore sizeStore = InbuiltModSizeStore.getInstance();

        for (InbuiltMod mod : mods) {
            String id = mod.getId();

            int opacity = data.getIntExtra("opacity_" + id, -1);
            if (opacity > 0) {
                InbuiltModManager.getInstance(this).setOverlayButtonOpacity(id, opacity);
            }

            float x = data.getFloatExtra("posx_" + id, -1f);
            float y = data.getFloatExtra("posy_" + id, -1f);
            if (x >= 0f && y >= 0f) {
                sizeStore.setPositionX(id, x);
                sizeStore.setPositionY(id, y);
            }
        }
    }
}