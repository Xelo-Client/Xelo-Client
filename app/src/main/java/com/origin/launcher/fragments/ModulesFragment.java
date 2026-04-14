package com.origin.launcher.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.origin.launcher.R;
import com.origin.launcher.activity.InbuiltModsActivity;
import com.origin.launcher.manager.ThemeManager;
import com.origin.launcher.utils.ThemeUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ModulesFragment extends BaseThemedFragment {

    private File configFile;
    private ScrollView modulesScrollView;
    private LinearLayout modulesContainer;
    private List<ModuleItem> moduleItems;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private AlertDialog storageWarningDialog;
    
    private MaterialButton uploadCrosshairButton;
    private MaterialButton viewCrosshairButton;
    private LinearLayout crosshairButtonRow;
    private View crosshairSpacer;

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

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void openStoragePermissionSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
            } else {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
            }
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivity(intent);
        }
    }

    private void showStorageWarningDialog() {
        if (storageWarningDialog != null && storageWarningDialog.isShowing()) return;
        if (getContext() == null) return;

        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        dialogLayout.setPadding(padding, padding, padding, padding);

        TextView titleText = new TextView(requireContext());
        titleText.setText("Storage Permission Required");
        titleText.setTextSize(18);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setTextColor(ThemeManager.getInstance().getColor("onSurface"));
        dialogLayout.addView(titleText);

        TextView messageText = new TextView(requireContext());
        messageText.setText("Xelo Modules need storage access to save and load your mod settings. Without this permission, your changes will not be saved.\n\nPlease grant storage permission to continue.");
        messageText.setTextSize(14);
        messageText.setTextColor(ThemeManager.getInstance().getColor("onSurfaceVariant"));
        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        msgParams.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        messageText.setLayoutParams(msgParams);
        dialogLayout.addView(messageText);

        LinearLayout buttonRow = new LinearLayout(requireContext());
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = (int) (24 * getResources().getDisplayMetrics().density);
        buttonRow.setLayoutParams(rowParams);

        MaterialButton cancelButton = new MaterialButton(requireContext());
        cancelButton.setText("Cancel");
        cancelButton.setAllCaps(false);
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setShape(GradientDrawable.RECTANGLE);
        cancelBg.setColor(Color.parseColor("#F44336"));
        cancelBg.setCornerRadius(8 * getResources().getDisplayMetrics().density);
        cancelButton.setBackground(cancelBg);
        cancelButton.setBackgroundTintList(null);
        cancelButton.setTextColor(Color.WHITE);
        cancelButton.setStateListAnimator(null);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        cancelParams.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
        cancelButton.setLayoutParams(cancelParams);

        MaterialButton okButton = new MaterialButton(requireContext());
        okButton.setText("OK");
        okButton.setAllCaps(false);
        okButton.setStateListAnimator(null);
        ThemeUtils.applyThemeToButton(okButton, requireContext());
        LinearLayout.LayoutParams okParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        okButton.setLayoutParams(okParams);

        buttonRow.addView(cancelButton);
        buttonRow.addView(okButton);
        dialogLayout.addView(buttonRow);

        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setShape(GradientDrawable.RECTANGLE);
        dialogBg.setColor(ThemeManager.getInstance().getColor("surface"));
        dialogBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
        dialogBg.setStroke(
                (int) (1 * getResources().getDisplayMetrics().density),
                ThemeManager.getInstance().getColor("outline")
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogLayout);
        builder.setCancelable(false);

        storageWarningDialog = builder.create();
        if (storageWarningDialog.getWindow() != null) {
            storageWarningDialog.getWindow().setBackgroundDrawable(dialogBg);
        }

        cancelButton.setOnClickListener(v -> {
            Animation popDown = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_down);
            dialogLayout.startAnimation(popDown);
            popDown.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    if (storageWarningDialog != null) {
                        storageWarningDialog.dismiss();
                        storageWarningDialog = null;
                    }
                }
            });
        });

        okButton.setOnClickListener(v -> {
            if (storageWarningDialog != null) {
                storageWarningDialog.dismiss();
                storageWarningDialog = null;
            }
            openStoragePermissionSettings();
        });

        storageWarningDialog.show();

        if (storageWarningDialog.getWindow() != null) {
            Animation popUp = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_up);
            dialogLayout.startAnimation(popUp);
        }
    }

    private void showToggleWarningDialog(ModuleItem module, boolean isChecked, MaterialSwitch moduleSwitch) {
        if (getContext() == null) return;

        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        dialogLayout.setPadding(padding, padding, padding, padding);

        TextView titleText = new TextView(requireContext());
        titleText.setText("Storage Permission Required");
        titleText.setTextSize(18);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setTextColor(ThemeManager.getInstance().getColor("onSurface"));
        dialogLayout.addView(titleText);

        TextView messageText = new TextView(requireContext());
        messageText.setText("Saving module settings requires storage access. Please grant storage permission so your changes can be saved.");
        messageText.setTextSize(14);
        messageText.setTextColor(ThemeManager.getInstance().getColor("onSurfaceVariant"));
        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        msgParams.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        messageText.setLayoutParams(msgParams);
        dialogLayout.addView(messageText);

        LinearLayout buttonRow = new LinearLayout(requireContext());
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = (int) (24 * getResources().getDisplayMetrics().density);
        buttonRow.setLayoutParams(rowParams);

        MaterialButton cancelButton = new MaterialButton(requireContext());
        cancelButton.setText("Cancel");
        cancelButton.setAllCaps(false);
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setShape(GradientDrawable.RECTANGLE);
        cancelBg.setColor(Color.parseColor("#F44336"));
        cancelBg.setCornerRadius(8 * getResources().getDisplayMetrics().density);
        cancelButton.setBackground(cancelBg);
        cancelButton.setBackgroundTintList(null);
        cancelButton.setTextColor(Color.WHITE);
        cancelButton.setStateListAnimator(null);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        cancelParams.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
        cancelButton.setLayoutParams(cancelParams);

        MaterialButton okButton = new MaterialButton(requireContext());
        okButton.setText("OK");
        okButton.setAllCaps(false);
        okButton.setStateListAnimator(null);
        ThemeUtils.applyThemeToButton(okButton, requireContext());
        LinearLayout.LayoutParams okParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        okButton.setLayoutParams(okParams);

        buttonRow.addView(cancelButton);
        buttonRow.addView(okButton);
        dialogLayout.addView(buttonRow);

        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setShape(GradientDrawable.RECTANGLE);
        dialogBg.setColor(ThemeManager.getInstance().getColor("surface"));
        dialogBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
        dialogBg.setStroke(
                (int) (1 * getResources().getDisplayMetrics().density),
                ThemeManager.getInstance().getColor("outline")
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogLayout);
        builder.setCancelable(false);

        AlertDialog toggleDialog = builder.create();
        if (toggleDialog.getWindow() != null) {
            toggleDialog.getWindow().setBackgroundDrawable(dialogBg);
        }

        cancelButton.setOnClickListener(v -> {
            Animation popDown = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_down);
            dialogLayout.startAnimation(popDown);
            popDown.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    toggleDialog.dismiss();
                    try {
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } catch (Exception e) {
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    }
                }
            });
        });

        okButton.setOnClickListener(v -> {
            toggleDialog.dismiss();
            openStoragePermissionSettings();
        });

        toggleDialog.show();

        if (toggleDialog.getWindow() != null) {
            Animation popUp = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_up);
            dialogLayout.startAnimation(popUp);
        }
    }

    private void rebindSwitch(ModuleItem module, MaterialSwitch moduleSwitch) {
        int primaryColor = ThemeManager.getInstance().getColor("primary");
        moduleSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(
                module.isEnabled() ? primaryColor : SWITCH_DISABLED_COLOR
        ));
        moduleSwitch.setTrackTintList(android.content.res.ColorStateList.valueOf(
                module.isEnabled() ? adjustAlpha(primaryColor, 0.5f) : adjustAlpha(SWITCH_DISABLED_COLOR, 0.5f)
        ));
        moduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!hasStoragePermission()) {
                showToggleWarningDialog(module, isChecked, moduleSwitch);
                return;
            }
            module.setEnabled(isChecked);
            moduleSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(
                    isChecked ? primaryColor : SWITCH_DISABLED_COLOR
            ));
            moduleSwitch.setTrackTintList(android.content.res.ColorStateList.valueOf(
                    isChecked ? adjustAlpha(primaryColor, 0.5f) : adjustAlpha(SWITCH_DISABLED_COLOR, 0.5f)
            ));
            onModuleToggle(module, isChecked);
        });
    }

    @Override
    public void onDestroyView() {
        if (storageWarningDialog != null) {
            storageWarningDialog.dismiss();
            storageWarningDialog = null;
        }
        super.onDestroyView();
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!hasStoragePermission()) {
            showStorageWarningDialog();
        }
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
            //moduleItems.add(new ModuleItem("No Fog", "(Doesnt work with fullbright) allows you to toggle the in-game fog", "Nofog"));
            moduleItems.add(new ModuleItem("Better Brightness", "allows you to see in the dark", "better_brightness"));
            moduleItems.add(new ModuleItem("Particles Disabler", "allows you to toggle the in-game particles", "particles_disabler"));
            moduleItems.add(new ModuleItem("Java Fancy Clouds", "Changes the clouds to Java Fancy Clouds", "java_clouds"));
            //moduleItems.add(new ModuleItem("Java Cubemap", "improves the in-game cubemap bringing it abit lower", "java_cubemap"));
            moduleItems.add(new ModuleItem("Classic Vanilla skins", "Disables the newly added skins by mojang", "classic_skins"));
            moduleItems.add(new ModuleItem("No flipbook animation", "optimizes your fps by disabling block animation", "no_flipbook_animations"));
            moduleItems.add(new ModuleItem("No Shadows", "optimizes your fps by disabling shadows", "no_shadows"));
            moduleItems.add(new ModuleItem("Xelo Title", "Changes the Start screen title image", "xelo_title"));
            moduleItems.add(new ModuleItem("2x tpp view", "doubles your third person view radius, letting you see more than you're supposed to", "double_tppview"));
            moduleItems.add(new ModuleItem("White Block Outline", "changes the block selection outline to white", "white_block_outline"));
            moduleItems.add(new ModuleItem("No pumpkin overlay", "disables the dark blurry overlay when wearing pumpkin", "no_pumpkin_overlay"));
            moduleItems.add(new ModuleItem("No spyglass overlay", "disables the spyglass overlay when using spyglass", "no_spyglass_overlay"));
            moduleItems.add(new ModuleItem("No Eating animation", "disables the eating animation of food items", "no_eating_animation"));
            moduleItems.add(new ModuleItem("No Bow animation", "disables the bow animation", "no_bow_animation"));
            moduleItems.add(new ModuleItem("Portal optimizer", "optimizes the portal by tweaking", "portal_optimizer"));
            //moduleItems.add(new ModuleItem("Particle single mapping", "optimizes the particles material by enabling culling and disabling particle fog", "psm"));
            moduleItems.add(new ModuleItem("Custom CrossHair", "lets you use your own CrossHair", "custom_cross_hair"));

            // Load current config state and populate modules
            loadModuleStates();
            populateModules();
        }
    }

    private void populateModules() {
        if (modulesContainer == null || moduleItems == null) return;

        // Clear existing modules
        modulesContainer.removeAllViews();

        // Add each module as a card view with spacing
        for (int i = 0; i < moduleItems.size(); i++) {
            ModuleItem module = moduleItems.get(i);
            View moduleView = createModuleView(module);
            modulesContainer.addView(moduleView);

            // Add upload button after Custom CrossHair module
            if (module.getConfigKey().equals("custom_cross_hair")) {
                // Add spacing before button row
                crosshairSpacer = new View(getContext());
                LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (12 * getResources().getDisplayMetrics().density)
                );
                crosshairSpacer.setLayoutParams(spacerParams);
                modulesContainer.addView(crosshairSpacer);

                // Create horizontal button row
                crosshairButtonRow = new LinearLayout(requireContext());
                crosshairButtonRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                crosshairButtonRow.setLayoutParams(rowParams);

                // Upload button (weight 1)
                uploadCrosshairButton = createUploadButton();
                LinearLayout.LayoutParams uploadParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                );
                uploadParams.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
                uploadCrosshairButton.setLayoutParams(uploadParams);

                // View crosshair button (weight 1)
                viewCrosshairButton = createViewCrosshairButton();
                LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                );
                viewCrosshairButton.setLayoutParams(viewParams);

                crosshairButtonRow.addView(uploadCrosshairButton);
                crosshairButtonRow.addView(viewCrosshairButton);
                modulesContainer.addView(crosshairButtonRow);

                int visibility = module.isEnabled() ? View.VISIBLE : View.GONE;
                crosshairSpacer.setVisibility(visibility);
                crosshairButtonRow.setVisibility(visibility);
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

    private MaterialButton createViewCrosshairButton() {
        MaterialButton viewButton = new MaterialButton(requireContext());

        viewButton.setText("View Crosshair");
        viewButton.setTextSize(16);
        viewButton.setAllCaps(false);
        viewButton.setStateListAnimator(null);

        // Outlined secondary style to distinguish from upload button
        int primaryColor = ThemeManager.getInstance().getColor("primary");
        int surfaceColor = ThemeManager.getInstance().getColor("surface");
        viewButton.setBackgroundColor(surfaceColor);
        viewButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(surfaceColor));
        viewButton.setTextColor(primaryColor);
        viewButton.setStrokeColor(android.content.res.ColorStateList.valueOf(primaryColor));
        viewButton.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
        viewButton.setCornerRadius((int) (8 * getResources().getDisplayMetrics().density));

        int paddingVertical = (int) (12 * getResources().getDisplayMetrics().density);
        viewButton.setPadding(0, paddingVertical, 0, paddingVertical);

        viewButton.setOnClickListener(v -> showCrosshairPreviewDialog());

        return viewButton;
    }

    private void showCrosshairPreviewDialog() {
        if (getContext() == null) return;

        File crosshairFile = new File("/storage/emulated/0/games/xelo_client/custom_cross_hair/cross_hair.png");

        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        dialogLayout.setPadding(padding, padding, padding, padding);
        dialogLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // Title
        TextView titleText = new TextView(requireContext());
        titleText.setText("Crosshair Preview");
        titleText.setTextSize(18);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setTextColor(ThemeManager.getInstance().getColor("onSurface"));
        dialogLayout.addView(titleText);

        if (crosshairFile.exists()) {
            // Show the crosshair image
            ImageView crosshairImage = new ImageView(requireContext());
            int imageSize = (int) (160 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imageSize, imageSize);
            imageParams.topMargin = (int) (20 * getResources().getDisplayMetrics().density);
            crosshairImage.setLayoutParams(imageParams);
            crosshairImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            crosshairImage.setImageURI(Uri.fromFile(crosshairFile));
            dialogLayout.addView(crosshairImage);
        } else {
            // No file uploaded yet — show placeholder icon + message
            ImageView placeholderIcon = new ImageView(requireContext());
            int iconSize = (int) (64 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconParams.topMargin = (int) (20 * getResources().getDisplayMetrics().density);
            placeholderIcon.setLayoutParams(iconParams);
            placeholderIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            placeholderIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            placeholderIcon.setColorFilter(ThemeManager.getInstance().getColor("onSurfaceVariant"));
            dialogLayout.addView(placeholderIcon);

            TextView noFileText = new TextView(requireContext());
            noFileText.setText("No crosshair uploaded yet.\nUse \"Upload Crosshair\" to add one.");
            noFileText.setTextSize(13);
            noFileText.setTextColor(ThemeManager.getInstance().getColor("onSurfaceVariant"));
            noFileText.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams noFileParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            noFileParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
            noFileText.setLayoutParams(noFileParams);
            dialogLayout.addView(noFileText);
        }

        // Close button
        MaterialButton closeButton = new MaterialButton(requireContext());
        closeButton.setText("Close");
        closeButton.setAllCaps(false);
        closeButton.setStateListAnimator(null);
        ThemeUtils.applyThemeToButton(closeButton, requireContext());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        closeParams.topMargin = (int) (20 * getResources().getDisplayMetrics().density);
        closeButton.setLayoutParams(closeParams);
        dialogLayout.addView(closeButton);

        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setShape(GradientDrawable.RECTANGLE);
        dialogBg.setColor(ThemeManager.getInstance().getColor("surface"));
        dialogBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
        dialogBg.setStroke(
            (int) (1 * getResources().getDisplayMetrics().density),
            ThemeManager.getInstance().getColor("outline")
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogLayout);
        builder.setCancelable(true);

        AlertDialog previewDialog = builder.create();
        if (previewDialog.getWindow() != null) {
            previewDialog.getWindow().setBackgroundDrawable(dialogBg);
        }

        closeButton.setOnClickListener(v -> {
            Animation popDown = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_down);
            dialogLayout.startAnimation(popDown);
            popDown.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    previewDialog.dismiss();
                }
            });
        });

        previewDialog.show();

        if (previewDialog.getWindow() != null) {
            Animation popUp = AnimationUtils.loadAnimation(requireContext(), R.anim.pop_up);
            dialogLayout.startAnimation(popUp);
        }
    }

    private MaterialButton createUploadButton() {
        MaterialButton uploadButton = new MaterialButton(requireContext());

        // Layout params — width/weight are set by the caller (populateModules)
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
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
                if (!hasStoragePermission()) {
                    showToggleWarningDialog(module, isChecked, moduleSwitch);
                    return;
                }
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
    
        // --- NEW CODE: Toggle visibility when switch is pressed ---
        if (module.getConfigKey().equals("custom_cross_hair") && crosshairButtonRow != null && crosshairSpacer != null) {
            int visibility = isEnabled ? View.VISIBLE : View.GONE;
            crosshairButtonRow.setVisibility(visibility);
            crosshairSpacer.setVisibility(visibility);
        }
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
            //defaultConfig.put("Nofog", false);
            defaultConfig.put("better_brightness", false);
            defaultConfig.put("particles_disabler", false);
            defaultConfig.put("java_clouds", false);
            //defaultConfig.put("java_cubemap", false);
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
        if (modulesContainer != null && moduleItems != null) {
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
