package com.origin.launcher.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.JSONObject;

public class ThemesFragment extends BaseThemedFragment {
    private static final String TAG = "ThemesFragment";
    private static final String PREF_SELECTED_THEME = "selected_theme";
    private static final String DEFAULT_THEME = "default";
    
    private ImageView backButton;
    private LinearLayout themesContainer;
    private LinearLayout noThemesContainer;
    private TextView noThemesText;
    private MaterialButton importThemeFab;
    private List<ThemeItem> themesList;
    private String selectedTheme;
    private File themesDirectory;
    
    private ActivityResultLauncher<Intent> filePickerLauncher;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize file picker launcher
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importThemeFile(uri);
                    }
                }
            }
        );
        
        // Initialize themes directory
        File dataDir = new File(requireContext().getExternalFilesDir(null), "themes");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        themesDirectory = dataDir;
        
        // Initialize ThemeManager and get current theme
        ThemeManager themeManager = ThemeManager.getInstance(requireContext());
        selectedTheme = themeManager.getCurrentThemeName();
        if (selectedTheme == null || selectedTheme.isEmpty()) {
            selectedTheme = DEFAULT_THEME;
            // Load default theme to ensure it's properly initialized
            themeManager.loadTheme(DEFAULT_THEME);
        }
        
        Log.d(TAG, "Current selected theme: " + selectedTheme);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_themes, container, false);
        
        backButton = view.findViewById(R.id.back_button);
        themesContainer = view.findViewById(R.id.themes_container);
        noThemesContainer = view.findViewById(R.id.no_themes_container);
        noThemesText = view.findViewById(R.id.no_themes_text);
        importThemeFab = view.findViewById(R.id.new_theme_button);
        
        // Set up back button
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        
        // Set up FAB
        importThemeFab.setOnClickListener(v -> openFilePicker());
        
        loadThemes();
        
        return view;
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        // Add extra filter for .xtheme files
        String[] mimeTypes = {"application/octet-stream", "*/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "Select Theme File"));
        } catch (Exception e) {
            Log.e(TAG, "Error opening file picker", e);
            Toast.makeText(getContext(), "Unable to open file picker", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void importThemeFile(Uri uri) {
        try {
            String fileName = getFileName(uri);
            if (fileName == null || !fileName.endsWith(".xtheme")) {
                Toast.makeText(getContext(), "Please select a valid .xtheme file", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Extract and validate the .xtheme file (ZIP archive)
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (!extractAndValidateXTheme(inputStream, fileName)) {
                Toast.makeText(getContext(), "Invalid .xtheme file format", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Toast.makeText(getContext(), "Theme imported: " + fileName, Toast.LENGTH_SHORT).show();
            loadThemes(); // Refresh the list
            
        } catch (Exception e) {
            Log.e(TAG, "Error importing theme file", e);
            Toast.makeText(getContext(), "Error importing theme: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private boolean extractAndValidateXTheme(InputStream inputStream, String fileName) {
        try {
            String themeKey = fileName.replace(".xtheme", "");
            File themeDir = new File(themesDirectory, themeKey);
            
            // Create theme directory
            if (!themeDir.exists()) {
                themeDir.mkdirs();
            }
            
            // Extract ZIP contents
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry entry;
            boolean foundColorsJson = false;
            
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    // Create directory
                    File dir = new File(themeDir, entry.getName());
                    dir.mkdirs();
                } else {
                    // Extract file
                    File file = new File(themeDir, entry.getName());
                    file.getParentFile().mkdirs();
                    
                    FileOutputStream outputStream = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    
                    while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.close();
                    
                    // Check if this is the required files
                    if (entry.getName().equals("colors/colors.json")) {
                        foundColorsJson = true;
                    }
                }
                zipInputStream.closeEntry();
            }
            
            zipInputStream.close();
            inputStream.close();
            
            if (!foundColorsJson) {
                // Clean up extracted files if invalid
                deleteDirectory(themeDir);
                Log.e(TAG, "No colors/colors.json found in .xtheme file");
                return false;
            }
            
            // Verify manifest.json exists
            File manifestFile = new File(themeDir, "manifest.json");
            if (!manifestFile.exists()) {
                Log.w(TAG, "No manifest.json found in .xtheme file, theme may not display properly");
            }
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting .xtheme file", e);
            return false;
        }
    }
    
    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }
    
    private ThemeItem getXThemeMetadata(File colorsJsonFile, String themeKey) {
        try {
            // First try to read manifest.json for metadata
            File themeDir = colorsJsonFile.getParentFile().getParentFile(); // Go up from colors/colors.json to theme root
            File manifestFile = new File(themeDir, "manifest.json");
            
            if (manifestFile.exists()) {
                InputStream inputStream = new java.io.FileInputStream(manifestFile);
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                inputStream.close();
                
                String jsonString = new String(buffer, "UTF-8");
                JSONObject manifestJson = new JSONObject(jsonString);
                
                String name = manifestJson.optString("name", themeKey);
                String author = manifestJson.optString("author", null);
                String description = manifestJson.optString("description", "Custom theme");
                
                return new ThemeItem(name, description, themeKey, false, author);
            } else {
                // Fallback to old method (colors.json) for compatibility
                InputStream inputStream = new java.io.FileInputStream(colorsJsonFile);
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                inputStream.close();
                
                String jsonString = new String(buffer, "UTF-8");
                JSONObject themeJson = new JSONObject(jsonString);
                
                String name = themeJson.optString("name", themeKey);
                String author = themeJson.optString("author", null);
                String description = themeJson.optString("description", "Custom theme");
                
                return new ThemeItem(name, description, themeKey, false, author);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading .xtheme metadata: " + themeKey, e);
            // Return a basic theme item if we can't read metadata
            return new ThemeItem(themeKey, "Custom theme", themeKey, false, null);
        }
    }
    
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
    
    private void loadThemes() {
        themesList = new ArrayList<>();
        
        // Initialize ThemeManager
        ThemeManager themeManager = ThemeManager.getInstance(requireContext());
        
        // Load built-in themes from assets
        String[] availableThemes = themeManager.getAvailableThemes();
        for (String themeName : availableThemes) {
            ThemeManager.ThemeMetadata metadata = themeManager.getThemeMetadata(themeName);
            themesList.add(new ThemeItem(metadata.name, metadata.description, metadata.key, false, metadata.author));
        }
        
        // Load custom themes from directory (extracted .xtheme folders)
        if (themesDirectory.exists() && themesDirectory.isDirectory()) {
            File[] themeFolders = themesDirectory.listFiles(File::isDirectory);
            if (themeFolders != null) {
                for (File themeFolder : themeFolders) {
                    // Check if this folder contains a colors/colors.json file
                    File colorsJson = new File(themeFolder, "colors/colors.json");
                    if (colorsJson.exists()) {
                        String themeKey = themeFolder.getName();
                        
                        // Try to get theme metadata from colors.json
                        ThemeItem themeItem = getXThemeMetadata(colorsJson, themeKey);
                        if (themeItem != null) {
                            themesList.add(themeItem);
                        }
                    }
                }
            }
        }
        
        // Sync selectedTheme with ThemeManager after loading themes
        selectedTheme = ThemeManager.getInstance().getCurrentThemeName();
        
        displayThemes();
    }
    
    private void displayThemes() {
        themesContainer.removeAllViews();
        
        // Check if we only have the default theme
        boolean hasCustomThemes = themesList.size() > 1;
        
        if (!hasCustomThemes) {
            noThemesContainer.setVisibility(View.VISIBLE);
            // Still show the default theme
            for (ThemeItem theme : themesList) {
                createThemeCard(theme, 0);
            }
        } else {
            noThemesContainer.setVisibility(View.GONE);
            for (int i = 0; i < themesList.size(); i++) {
                ThemeItem theme = themesList.get(i);
                createThemeCard(theme, i);
                
                // Add spacing between cards
                if (i < themesList.size() - 1) {
                    View spacer = new View(getContext());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 
                        (int) (12 * getResources().getDisplayMetrics().density)
                    );
                    spacer.setLayoutParams(params);
                    themesContainer.addView(spacer);
                }
            }
        }
    }
    
    private void createThemeCard(ThemeItem theme, int position) {
    // Create card layout
    MaterialCardView card = new MaterialCardView(requireContext());
    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 
        LinearLayout.LayoutParams.WRAP_CONTENT
    );
    card.setLayoutParams(cardParams);
    card.setRadius(12 * getResources().getDisplayMetrics().density);
    card.setCardElevation(0); // Remove elevation for flat design
    card.setClickable(true);
    card.setFocusable(true);
    
    // Apply theme colors to card
    ThemeUtils.applyThemeToCard(card, requireContext());
    card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
    
    // Main container
    LinearLayout mainLayout = new LinearLayout(requireContext());
    mainLayout.setOrientation(LinearLayout.HORIZONTAL);
    mainLayout.setPadding(
        (int) (16 * getResources().getDisplayMetrics().density),
        (int) (16 * getResources().getDisplayMetrics().density),
        (int) (16 * getResources().getDisplayMetrics().density),
        (int) (16 * getResources().getDisplayMetrics().density)
    );
    mainLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
    
    // Text container (moved before radio button for better layout)
    LinearLayout textLayout = new LinearLayout(requireContext());
    textLayout.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
        0, 
        LinearLayout.LayoutParams.WRAP_CONTENT, 
        1.0f
    );
    textLayout.setLayoutParams(textParams);
    
    // Theme name
    TextView nameText = new TextView(requireContext());
    nameText.setText(theme.name);
    nameText.setTextSize(16);
    nameText.setTypeface(null, android.graphics.Typeface.BOLD);
    ThemeUtils.applyThemeToTextView(nameText, "onSurface");
    
    // Author text (if available)
    if (theme.author != null && !theme.author.isEmpty()) {
        TextView authorText = new TextView(requireContext());
        authorText.setText("by " + theme.author);
        authorText.setTextSize(14);
        ThemeUtils.applyThemeToTextView(authorText, "onSurfaceVariant");
        LinearLayout.LayoutParams authorParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        authorParams.topMargin = (int) (2 * getResources().getDisplayMetrics().density);
        authorText.setLayoutParams(authorParams);
        textLayout.addView(authorText);
    }
    
    // Theme description
    TextView descText = new TextView(requireContext());
    descText.setText(theme.description);
    descText.setTextSize(14);
    ThemeUtils.applyThemeToTextView(descText, "onSurfaceVariant");
    LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, 
        LinearLayout.LayoutParams.WRAP_CONTENT
    );
    descParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
    descText.setLayoutParams(descParams);
    
    textLayout.addView(nameText);
    textLayout.addView(descText);
    
    // Right side container for buttons
    LinearLayout rightContainer = new LinearLayout(requireContext());
    rightContainer.setOrientation(LinearLayout.HORIZONTAL);
    rightContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    );
    rightParams.setMarginStart((int) (16 * getResources().getDisplayMetrics().density));
    rightContainer.setLayoutParams(rightParams);
    
    // Info button (optional - you can add this if needed)
    ImageView infoButton = new ImageView(requireContext());
    LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
        (int) (24 * getResources().getDisplayMetrics().density),
        (int) (24 * getResources().getDisplayMetrics().density)
    );
    infoParams.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
    infoButton.setLayoutParams(infoParams);
    infoButton.setImageResource(android.R.drawable.ic_dialog_info); // You can replace with your own icon
    infoButton.setColorFilter(ThemeManager.getInstance().getColor("onSurfaceVariant"));
    
    // Create circle ripple background for info button
    android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
    circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
    circle.setColor(android.graphics.Color.TRANSPARENT);
    
    infoButton.setBackground(ThemeUtils.createCircularRipple("onSurfaceVariant"));
    infoButton.setClickable(true);
    infoButton.setFocusable(true);
    infoButton.setOnClickListener(v -> showThemeInfoDialog(theme));
    
    // Radio button / Selection indicator
    MaterialRadioButton radioButton = new MaterialRadioButton(requireContext());
    LinearLayout.LayoutParams radioParams = new LinearLayout.LayoutParams(
        (int) (24 * getResources().getDisplayMetrics().density),
        (int) (24 * getResources().getDisplayMetrics().density)
    );
    radioButton.setLayoutParams(radioParams);
    radioButton.setChecked(theme.key.equals(selectedTheme));
    radioButton.setClickable(false);
    radioButton.setFocusable(false);
    
    // Apply theme colors to radio button
    ThemeUtils.applyThemeToRadioButton(radioButton, requireContext());
    
    rightContainer.addView(infoButton);
    rightContainer.addView(radioButton);
    
    // Delete button (only for custom .xtheme themes, not built-in themes)
    if (!isBuiltInTheme(theme.key)) {
        ImageView deleteButton = new ImageView(requireContext());
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
            (int) (24 * getResources().getDisplayMetrics().density),
            (int) (24 * getResources().getDisplayMetrics().density)
        );
        deleteParams.setMarginStart((int) (8 * getResources().getDisplayMetrics().density));
        deleteButton.setLayoutParams(deleteParams);
        deleteButton.setImageResource(android.R.drawable.ic_menu_delete); // You can replace with your own icon
        deleteButton.setColorFilter(ThemeManager.getInstance().getColor("error"));
        
        // Create circle ripple background for delete button
        android.graphics.drawable.GradientDrawable deleteCircle = new android.graphics.drawable.GradientDrawable();
        deleteCircle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        deleteCircle.setColor(android.graphics.Color.TRANSPARENT);
        
        deleteButton.setBackground(ThemeUtils.createCircularRipple("error"));
        deleteButton.setClickable(true);
        deleteButton.setFocusable(true);
        deleteButton.setContentDescription("Delete theme");
        deleteButton.setOnClickListener(v -> showDeleteConfirmation(theme, position));
        
        rightContainer.addView(deleteButton);
    }
    
    mainLayout.addView(textLayout);
    mainLayout.addView(rightContainer);
    
    card.addView(mainLayout);
    
    // Set card click listener with ripple effect
    card.setOnClickListener(v -> {
        if (!theme.key.equals(selectedTheme)) {
            // Apply theme using ThemeManager
            ThemeManager themeManager = ThemeManager.getInstance();
            boolean success = themeManager.loadTheme(theme.key);
            
            if (success) {
                selectedTheme = theme.key; // Update selectedTheme after successful load
                displayThemes(); // Refresh to update radio buttons
                Toast.makeText(getContext(), "Theme applied: " + theme.name, Toast.LENGTH_SHORT).show();
                
                // Refresh the current view with new theme
                refreshTheme();
                
                // Also refresh the parent activity if it's a BaseThemedActivity
                if (getActivity() instanceof BaseThemedActivity) {
                    ((BaseThemedActivity) getActivity()).refreshTheme();
                }
            } else {
                Toast.makeText(getContext(), "Failed to apply theme", Toast.LENGTH_SHORT).show();
            }
        }
    });
    
    // Ripple effect is already applied by ThemeUtils.applyThemeToCard()
    
    themesContainer.addView(card);
}
    
    private void showDeleteConfirmation(ThemeItem theme, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Theme")
            .setMessage("Are you sure you want to delete \"" + theme.name + "\"? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteTheme(theme, position);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deleteTheme(ThemeItem theme, int position) {
        try {
            // Check if it's a built-in theme (can't delete)
            if (isBuiltInTheme(theme.key)) {
                Toast.makeText(getContext(), "Cannot delete built-in themes", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Delete the theme folder (extracted .xtheme)
            File themeDir = new File(themesDirectory, theme.key);
            if (themeDir.exists() && themeDir.isDirectory()) {
                deleteDirectory(themeDir);
                
                // If this was the selected theme, revert to default
                if (theme.key.equals(selectedTheme)) {
                    selectedTheme = DEFAULT_THEME;
                    ThemeManager.getInstance().loadTheme(DEFAULT_THEME);
                    refreshTheme();
                    
                    // Also refresh the parent activity
                    if (getActivity() instanceof BaseThemedActivity) {
                        ((BaseThemedActivity) getActivity()).refreshTheme();
                    }
                }
                
                themesList.remove(position);
                displayThemes();
                Toast.makeText(getContext(), "Theme deleted: " + theme.name, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to delete theme", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting theme", e);
            Toast.makeText(getContext(), "Error deleting theme: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private boolean isBuiltInTheme(String themeKey) {
        // Check if theme exists in assets (built-in themes)
        try {
            String jsonPath = "themes/" + themeKey + ".json";
            requireContext().getAssets().open(jsonPath).close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void showThemeInfoDialog(ThemeItem theme) {
        try {
            // Create dialog layout
            LinearLayout dialogLayout = new LinearLayout(requireContext());
            dialogLayout.setOrientation(LinearLayout.VERTICAL);
            dialogLayout.setPadding(24, 24, 24, 24);
            
            // Title
            TextView titleText = new TextView(requireContext());
            titleText.setText(theme.name);
            titleText.setTextSize(20);
            titleText.setTypeface(null, android.graphics.Typeface.BOLD);
            ThemeUtils.applyThemeToTextView(titleText, "onSurface");
            dialogLayout.addView(titleText);
            
            // Author (if available)
            if (theme.author != null && !theme.author.isEmpty()) {
                TextView authorText = new TextView(requireContext());
                authorText.setText("by " + theme.author);
                authorText.setTextSize(14);
                ThemeUtils.applyThemeToTextView(authorText, "onSurfaceVariant");
                LinearLayout.LayoutParams authorParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                authorParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
                authorText.setLayoutParams(authorParams);
                dialogLayout.addView(authorText);
            }
            
            // Preview image (if available for custom themes)
            if (!isBuiltInTheme(theme.key)) {
                File previewFile = new File(themesDirectory, theme.key + "/preview.png");
                if (previewFile.exists()) {
                    ImageView previewImage = new ImageView(requireContext());
                    Bitmap bitmap = BitmapFactory.decodeFile(previewFile.getAbsolutePath());
                    if (bitmap != null) {
                        previewImage.setImageBitmap(bitmap);
                        previewImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (int) (200 * getResources().getDisplayMetrics().density)
                        );
                        imageParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
                        imageParams.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density);
                        previewImage.setLayoutParams(imageParams);
                        dialogLayout.addView(previewImage);
                    }
                }
            }
            
            // Description
            TextView descriptionText = new TextView(requireContext());
            descriptionText.setText(theme.description);
            descriptionText.setTextSize(14);
            ThemeUtils.applyThemeToTextView(descriptionText, "onSurfaceVariant");
            LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            descParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
            descriptionText.setLayoutParams(descParams);
            dialogLayout.addView(descriptionText);
            
            // Show additional metadata for custom themes
            if (!isBuiltInTheme(theme.key)) {
                addManifestInfo(dialogLayout, theme.key);
            }
            
            // Create dialog with proper theming
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setView(dialogLayout);
            builder.setPositiveButton("Close", null);
            
            // Apply theme to dialog
            androidx.appcompat.app.AlertDialog dialog = builder.show();
            ThemeUtils.applyThemeToDialog(dialog);
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing theme info dialog", e);
            Toast.makeText(getContext(), "Error loading theme information", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void addManifestInfo(LinearLayout layout, String themeKey) {
        try {
            File manifestFile = new File(themesDirectory, themeKey + "/manifest.json");
            if (manifestFile.exists()) {
                java.io.InputStream inputStream = new java.io.FileInputStream(manifestFile);
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                inputStream.close();
                
                String jsonString = new String(buffer, "UTF-8");
                JSONObject manifest = new JSONObject(jsonString);
                
                // Add divider
                View divider = new View(requireContext());
                divider.setBackgroundColor(ThemeManager.getInstance().getColor("outline"));
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    (int) (1 * getResources().getDisplayMetrics().density)
                );
                dividerParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
                dividerParams.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density);
                divider.setLayoutParams(dividerParams);
                layout.addView(divider);
                
                // Version
                if (manifest.has("version")) {
                    addInfoRow(layout, "Version", manifest.getString("version"));
                }
                
                // Package
                if (manifest.has("package")) {
                    addInfoRow(layout, "Package", manifest.getString("package"));
                }
                
                // License
                if (manifest.has("license")) {
                    addInfoRow(layout, "License", manifest.getString("license"));
                }
                
                // Created/Updated dates
                if (manifest.has("createdAt")) {
                    addInfoRow(layout, "Created", manifest.getString("createdAt"));
                }
                if (manifest.has("updatedAt")) {
                    addInfoRow(layout, "Updated", manifest.getString("updatedAt"));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading manifest info", e);
        }
    }
    
    private void addInfoRow(LinearLayout layout, String label, String value) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        
        TextView labelText = new TextView(requireContext());
        labelText.setText(label + ": ");
        labelText.setTextSize(12);
        labelText.setTypeface(null, android.graphics.Typeface.BOLD);
        ThemeUtils.applyThemeToTextView(labelText, "onSurfaceVariant");
        
        TextView valueText = new TextView(requireContext());
        valueText.setText(value);
        valueText.setTextSize(12);
        ThemeUtils.applyThemeToTextView(valueText, "onSurfaceVariant");
        
        row.addView(labelText);
        row.addView(valueText);
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
        row.setLayoutParams(rowParams);
        
        layout.addView(row);
    }
    
    @Override
    protected void onApplyTheme() {
        // Apply theme to the import button
        if (importThemeFab != null) {
            ThemeUtils.applyThemeToButton(importThemeFab, requireContext());
        }
        
        // Refresh the themes display to apply theme colors
        displayThemes();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Update Discord RPC when fragment resumes
        DiscordRPCHelper.getInstance().updateMenuPresence("Themes");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Update Discord RPC when leaving themes
        DiscordRPCHelper.getInstance().updateIdlePresence();
    }
    
    // Theme item class
    private static class ThemeItem {
        String name;
        String description;
        String key;
        boolean isDefault;
        String author;
        
        ThemeItem(String name, String description, String key, boolean isDefault, String author) {
            this.name = name;
            this.description = description;
            this.key = key;
            this.isDefault = isDefault;
            this.author = author;
        }
    }
}