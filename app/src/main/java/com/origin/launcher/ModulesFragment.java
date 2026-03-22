package com.origin.launcher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.button.MaterialButton;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.origin.launcher.InbuiltModsActivity;

public class ModulesFragment extends BaseThemedFragment {
    
    private File configFile;
    private ScrollView modulesScrollView;
    private LinearLayout modulesContainer;
    private List<ModuleItem> moduleItems;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private static final int SWITCH_DISABLED_COLOR = 0xFF757575;
    
    // Module data class
    private static class ModuleItem {
        private String name;
        private String description;
        private String configKey;
        private boolean enabled;
        
        public ModuleItem(String name, String description, String configKey) {
            this.name = name;
            this.description = description;
            this.configKey = configKey;
            this.enabled = false; // Default to disabled
        }
        
        // Getters and setters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getConfigKey() { return configKey; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        saveCrosshairImage(imageUri);
                    }
                }
            }
        );
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_modules, container, false);
        
        // Initialize back button
        initializeBackButton(view);
        
        // Initialize modules
        initializeModules(view);
        
        return view;
    }
    
    private void initializeBackButton(View view) {
        ImageView backButton = view.findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                try {
                    requireActivity().getSupportFragmentManager().popBackStack();
                } catch (Exception e) {
                    // Handle error gracefully
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                }
            });
        }
    }
    
    private void initializeModules(View view) {
        // Initialize config file path
        configFile = new File("/storage/emulated/0/games/xelo_client/xelo_mods/config.json");
        
        // Get ScrollView and container
        modulesScrollView = view.findViewById(R.id.modulesScrollView);
        modulesContainer = view.findViewById(R.id.modulesContainer);
        
        // Apply theme background to ScrollView and container
        refreshScrollViewBackground();
        
        if (modulesContainer != null) {
            // Initialize module items
            moduleItems = new ArrayList<>();
        
        moduleItems.add(new ModuleItem(
                    "Touch Modules",
                    "Manage Xelo touch mods (AutoSprint, Quick Drop, etc.)",
                    "inbuilt_mods_entry"
            ));
        
            moduleItems.add(new ModuleItem("No hurt cam", "allows you to toggle the in-game hurt cam", "Nohurtcam"));
            moduleItems.add(new ModuleItem("No Fog", "(Doesnt work with fullbright) allows you to toggle the in-game fog", "Nofog"));
            moduleItems.add(new ModuleItem("Better Brightness", "allows you to see in the dark", "better_brightness"));
            moduleItems.add(new ModuleItem("Particles Disabler", "allows you to toggle the in-game particles", "particles_disabler"));
            moduleItems.add(new ModuleItem("Java Fancy Clouds", "Changes the clouds to Java Fancy Clouds", "java_clouds"));
            moduleItems.add(new ModuleItem("Java Cubemap", "improves the in-game cubemap bringing it abit lower", "java_cubemap"));
            moduleItems.add(new ModuleItem("Classic Vanilla skins", "Disables the newly added skins by mojang", "classic_skins"));
            moduleItems.add(new ModuleItem("No flipbook animation", "optimizes your fps by disabling block animation", "no_flipbook_animations"));
            moduleItems.add(new ModuleItem("No Shadows", "optimizes your fps by disabling shadows", "no_shadows"));
            moduleItems.add(new ModuleItem("Xelo Title", "Changes the Start screen title image", "xelo_title"));
            moduleItems.add(new ModuleItem("2x tpp view", "doubles your third person view radius, letting you see more than you're supposed to", "double_tppview"));
            moduleItems.add(new ModuleItem("White Block Outline", "changes the block selection outline to white", "white_block_outline"));
            moduleItems.add(new ModuleItem("No pumpkin overlay", "disables the dark blurry overlay when wearing pumpkin", "no_pumpkin_overlay"));
            moduleItems.add(new ModuleItem("No spyglass overlay", "disables the spyglass overlay when using spyglass", "no_spyglass_overlay"));
moduleItems.add(new ModuleItem("Custom CrossHair", "lets you use your own CrossHair", "custom_cross_hair"));
            
            // Load current config state and populate modules
            loadModuleStates();
            populateModules();
        }
    }
    
    private void populateModules() {
        if (modulesContainer == null) return;
        
        // Clear existing modules
        modulesContainer.removeAllViews();
        
        // Add each module as a card view with spacing
        for (int i = 0; i < moduleItems.size(); i++) {
            ModuleItem module = moduleItems.get(i);
            View moduleView = createModuleView(module);
            modulesContainer.addView(moduleView);
            
            // Add upload button after Custom CrossHair module
            if (module.getConfigKey().equals("custom_cross_hair")) {
                // Add spacing before button
                View spacer = new View(getContext());
                LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    (int) (12 * getResources().getDisplayMetrics().density)
                );
                spacer.setLayoutParams(spacerParams);
                modulesContainer.addView(spacer);
                
                // Add upload button
                MaterialButton uploadButton = createUploadButton();
                modulesContainer.addView(uploadButton);
            }
            
            // Add spacing between cards
            if (i < moduleItems.size() - 1) {
                View spacer = new View(getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    (int) (12 * getResources().getDisplayMetrics().density)
                );
                spacer.setLayoutParams(params);
                modulesContainer.addView(spacer);
            }
        }
    }
    
    private MaterialButton createUploadButton() {
        MaterialButton uploadButton = new MaterialButton(requireContext());
        
        // Layout params
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        uploadButton.setLayoutParams(buttonParams);
        
        // Button styling
        uploadButton.setText("Upload crosshair");
        uploadButton.setTextSize(16);
        uploadButton.setAllCaps(false);
        
        // Apply theme colors
        int primaryColor = ThemeManager.getInstance().getColor("primary");
        int onPrimaryColor = ThemeManager.getInstance().getColor("onPrimary");
        uploadButton.setBackgroundColor(primaryColor);
        uploadButton.setTextColor(onPrimaryColor);
        
        // Set corner radius
        uploadButton.setCornerRadius((int) (8 * getResources().getDisplayMetrics().density));
        
        // Add padding
        int paddingVertical = (int) (12 * getResources().getDisplayMetrics().density);
        uploadButton.setPadding(0, paddingVertical, 0, paddingVertical);
        
        // Set click listener
        uploadButton.setOnClickListener(v -> openImagePicker());
        
        return uploadButton;
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }
    
    private void saveCrosshairImage(Uri imageUri) {
        try {
            // Create target directory
            File targetDir = new File("/storage/emulated/0/games/xelo_client/custom_cross_hair");
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created) {
                    Toast.makeText(requireContext(), "Failed to create crosshair directory", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // Create target file
            File targetFile = new File(targetDir, "cross_hair.png");
            
            // Copy image from URI to target file
            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
                 FileOutputStream outputStream = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            Toast.makeText(requireContext(), "Crosshair uploaded successfully!", Toast.LENGTH_SHORT).show();
            
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to upload crosshair: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
private View createModuleView(ModuleItem module) {
    // Create card layout (EXACTLY matching ThemesFragment pattern)
    MaterialCardView moduleCard = new MaterialCardView(requireContext());
    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    moduleCard.setLayoutParams(cardParams);
    moduleCard.setRadius(12 * getResources().getDisplayMetrics().density);
    moduleCard.setCardElevation(0); // Remove elevation for flat design
    moduleCard.setClickable(true);
    moduleCard.setFocusable(true);

    // Apply theme colors to card (exactly like ThemesFragment)
    ThemeUtils.applyThemeToCard(moduleCard, requireContext());
    moduleCard.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));

    // Main container (EXACTLY matching ThemesFragment)
    LinearLayout mainLayout = new LinearLayout(requireContext());
    mainLayout.setOrientation(LinearLayout.HORIZONTAL);
    mainLayout.setPadding(
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density)
    );
    mainLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

    // Text container (EXACTLY matching ThemesFragment - text comes first)
    LinearLayout textLayout = new LinearLayout(requireContext());
    textLayout.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f // Weight 1 to take remaining space
    );
    textLayout.setLayoutParams(textParams);

    // Module name (EXACTLY matching ThemesFragment title)
    TextView nameText = new TextView(requireContext());
    nameText.setText(module.getName());
    nameText.setTextSize(18);
    nameText.setTypeface(null, Typeface.BOLD);
    nameText.setTextColor(ThemeManager.getInstance().getColor("onSurface"));
    textLayout.addView(nameText);

    // Module description (EXACTLY matching ThemesFragment description)
    TextView descText = new TextView(requireContext());
    descText.setText(module.getDescription());
    descText.setTextSize(14);
    descText.setTextColor(ThemeManager.getInstance().getColor("onSurfaceVariant"));
    LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    descParams.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
    descText.setLayoutParams(descParams);
    textLayout.addView(descText);

    mainLayout.addView(textLayout);

    // Right container for switch/button (EXACTLY matching ThemesFragment)
    LinearLayout rightContainer = new LinearLayout(requireContext());
    rightContainer.setOrientation(LinearLayout.HORIZONTAL);
    rightContainer.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    rightContainer.setLayoutParams(rightParams);

    // Check if this is the "Touch Modules" entry
    if (module.getConfigKey().equals("inbuilt_mods_entry")) {
        // Create arrow button instead of switch
        ImageView arrowButton = new ImageView(requireContext());
        arrowButton.setImageResource(android.R.drawable.ic_menu_more);
        arrowButton.setColorFilter(ThemeManager.getInstance().getColor("onSurfaceVariant"));
        
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(
                (int) (24 * getResources().getDisplayMetrics().density),
                (int) (24 * getResources().getDisplayMetrics().density)
        );
        arrowButton.setLayoutParams(arrowParams);
        arrowButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        
        rightContainer.addView(arrowButton);
        
        // Make the entire card clickable to launch InbuiltModsActivity
        moduleCard.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), InbuiltModsActivity.class);
            startActivity(intent);
        });
        
    } else {
        // Create switch (EXACTLY matching ThemesFragment pattern)
        int primaryColor = ThemeManager.getInstance().getColor("primary");

        MaterialSwitch moduleSwitch = new MaterialSwitch(requireContext());
        moduleSwitch.setChecked(module.isEnabled());

        // Apply theme colors to the switch
        moduleSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(
                module.isEnabled() ? primaryColor : SWITCH_DISABLED_COLOR
        ));
        moduleSwitch.setTrackTintList(android.content.res.ColorStateList.valueOf(
                module.isEnabled() ? adjustAlpha(primaryColor, 0.5f) : adjustAlpha(SWITCH_DISABLED_COLOR, 0.5f)
        ));

        moduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            module.setEnabled(isChecked);
            moduleSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(
                    isChecked ? primaryColor : SWITCH_DISABLED_COLOR
            ));
            moduleSwitch.setTrackTintList(android.content.res.ColorStateList.valueOf(
                    isChecked ? adjustAlpha(primaryColor, 0.5f) : adjustAlpha(SWITCH_DISABLED_COLOR, 0.5f)
            ));
            onModuleToggle(module, isChecked);
        });

        rightContainer.addView(moduleSwitch);
        
        // Make card toggle the switch when clicked
        moduleCard.setOnClickListener(v -> moduleSwitch.setChecked(!moduleSwitch.isChecked()));
    }

    mainLayout.addView(rightContainer);
    moduleCard.addView(mainLayout);

    return moduleCard;
}

// Helper method to adjust alpha
private int adjustAlpha(int color, float factor) {
    int alpha = Math.round(android.graphics.Color.alpha(color) * factor);
    int red = android.graphics.Color.red(color);
    int green = android.graphics.Color.green(color);
    int blue = android.graphics.Color.blue(color);
    return android.graphics.Color.argb(alpha, red, green, blue);
}
    
    private void onModuleToggle(ModuleItem module, boolean isEnabled) {
        updateConfigFile(module.getConfigKey(), isEnabled);
        Toast.makeText(requireContext(), 
            module.getName() + " " + (isEnabled ? "enabled" : "disabled"), 
            Toast.LENGTH_SHORT).show();
    }
    
    private void loadModuleStates() {
        try {
            if (!configFile.exists()) {
                // Create directory if it doesn't exist
                File parentDir = configFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                // Create default config
                createDefaultConfig();
                return;
            }
            
            // Read existing config
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }
            
            JSONObject config = new JSONObject(content.toString());
            
            // Update module states
            for (ModuleItem module : moduleItems) {
                if (config.has(module.getConfigKey())) {
                    module.setEnabled(config.getBoolean(module.getConfigKey()));
                }
            }
            
        } catch (IOException | JSONException e) {
            Toast.makeText(requireContext(), "Failed to load module config: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void createDefaultConfig() {
        try {
            // Create parent directory if it doesn't exist
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    Toast.makeText(requireContext(), "Failed to create config directory", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            JSONObject defaultConfig = new JSONObject();
            defaultConfig.put("Nohurtcam", false);
            defaultConfig.put("Nofog", false);
            defaultConfig.put("better_brightness", false);
            defaultConfig.put("particles_disabler", false);
            defaultConfig.put("java_clouds", false);
            defaultConfig.put("java_cubemap", false);
            defaultConfig.put("classic_skins", false);
            defaultConfig.put("white_block_outline", false);
            defaultConfig.put("no_flipbook_animations", false);
            defaultConfig.put("no_shadows", false);
            defaultConfig.put("no_spyglass_overlay", false);
            defaultConfig.put("no_pumpkin_overlay", false);
            defaultConfig.put("double_tppview", false);
            defaultConfig.put("xelo_title", true);
defaultConfig.put("custom_cross_hair", false);
            
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(defaultConfig.toString(2)); // Pretty print with indent
            }
            
        } catch (IOException | JSONException e) {
            Toast.makeText(requireContext(), "Failed to create default config: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    private void updateConfigFile(String key, boolean value) {
        try {
            JSONObject config;
            
            if (configFile.exists()) {
                // Read existing config
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line);
                    }
                }
                config = new JSONObject(content.toString());
            } else {
                // Create new config and ensure directory exists
                config = new JSONObject();
                File parentDir = configFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    boolean created = parentDir.mkdirs();
                    if (!created) {
                        Toast.makeText(requireContext(), "Failed to create config directory", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            
            // Update the specific key
            config.put(key, value);
            
            // Write back to file
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(config.toString(2)); // Pretty print with indent
            }
            
        } catch (IOException | JSONException e) {
            Toast.makeText(requireContext(), "Failed to update config: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    @Override
    protected void onApplyTheme() {
        // Apply theme to the root view background
        View rootView = getView();
        if (rootView != null) {
            rootView.setBackgroundColor(ThemeManager.getInstance().getColor("background"));
        }
        
        // Apply theme to back button
        ImageView backButton = rootView != null ? rootView.findViewById(R.id.back_button) : null;
        if (backButton != null) {
            backButton.setColorFilter(ThemeManager.getInstance().getColor("onBackground"));
        }
        
        // Apply theme to ScrollView and modules container background
        refreshScrollViewBackground();
        
        // Refresh all module cards
        if (modulesContainer != null) {
            populateModules();
        }
    }
    
    /**
     * Refresh ScrollView and container background colors
     */
    private void refreshScrollViewBackground() {
        try {
            if (modulesScrollView != null) {
                // Make ScrollView background completely transparent
                modulesScrollView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                modulesScrollView.setBackground(null); // Remove any drawable background
            }
            if (modulesContainer != null) {
                // Make container background completely transparent
                modulesContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                modulesContainer.setBackground(null); // Remove any drawable background
            }
        } catch (Exception e) {
            // Fallback to transparent background
            if (modulesScrollView != null) {
                modulesScrollView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                try {
                    modulesScrollView.setBackground(null);
                } catch (Exception ignored) {}
            }
            if (modulesContainer != null) {
                modulesContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                try {
                    modulesContainer.setBackground(null);
                } catch (Exception ignored) {}
            }
        }
    }
}
