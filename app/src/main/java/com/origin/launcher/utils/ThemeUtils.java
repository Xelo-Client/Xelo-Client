package com.origin.launcher.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.graphics.Color;
import com.google.android.material.materialswitch.MaterialSwitch;
import android.animation.ValueAnimator;
import android.animation.ArgbEvaluator;
import com.google.android.material.tabs.TabLayout;

import com.origin.launcher.manager.ThemeManager;

public class ThemeUtils {
    
    /**
     * Ensure corner radius is preserved on a MaterialCardView
     */
    public static void preserveCornerRadius(MaterialCardView card, Context context) {
        try {
            // Always ensure corner radius is set to 12dp
            card.setRadius(12 * context.getResources().getDisplayMetrics().density);
        } catch (Exception e) {
            // Handle error gracefully
        }
    }
    
    /**
     * Apply theme colors to a MaterialCardView
     */
    public static void applyThemeToCard(MaterialCardView card, Context context) {
        try {
            ThemeManager themeManager = ThemeManager.getInstance();
            
            if (themeManager != null && themeManager.isThemeLoaded()) {
                card.setCardBackgroundColor(themeManager.getColor("surface"));
                card.setStrokeColor(themeManager.getColor("outline"));
                card.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density)); // 1dp stroke
                card.setCardElevation(0f); // Remove elevation for flat design
                
                // DO NOT override corner radius - preserve existing value
                // card.setRadius(12 * context.getResources().getDisplayMetrics().density);
                
                // Create ripple effect with theme colors
                RippleDrawable ripple = new RippleDrawable(
                    ColorStateList.valueOf(createOptimizedRippleColor("onSurface", "card")),
                    null,
                    null
                );
                card.setForeground(ripple);
            } else {
                // Fallback to default colors if theme not ready
                card.setCardBackgroundColor(Color.parseColor("#141414"));
                card.setStrokeColor(Color.parseColor("#505050"));
                card.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density));
                card.setCardElevation(0f);
                
                // DO NOT override corner radius - preserve existing value
                // card.setRadius(12 * context.getResources().getDisplayMetrics().density);
            }
        } catch (Exception e) {
            // Fallback to default colors on error
            card.setCardBackgroundColor(Color.parseColor("#141414"));
            card.setStrokeColor(Color.parseColor("#505050"));
            card.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density));
            card.setCardElevation(0f);
            
            // DO NOT override corner radius - preserve existing value
            // card.setRadius(12 * context.getResources().getDisplayMetrics().density);
        }
    }
    
    /**
     * Apply theme colors to a MaterialButton
     */
    public static void applyThemeToButton(MaterialButton button, Context context) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        // Determine button type and apply appropriate styling
        String buttonType = determineButtonType(button);
        
        // Create lighter ripple colors for better visibility
        int lightRippleColor = createOptimizedRippleColor("primary", "button");
        int lightSurfaceRippleColor = createOptimizedRippleColor("surfaceVariant", "button");
        int lightOutlineRippleColor = createOptimizedRippleColor("outline", "button");
        
        switch (buttonType) {
            case "outlined":
                // Outlined button: transparent background, colored border and text
                button.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                
                // Check if this is an export/import button and use primary color for better visibility
                String resourceName = "";
                try {
                    resourceName = button.getContext().getResources().getResourceEntryName(button.getId()).toLowerCase();
                } catch (Exception e) {
                    // Ignore, use default
                }
                
                if (resourceName.contains("import") || resourceName.contains("export")) {
                    // Export/Import buttons use primary color for better visibility
                    button.setTextColor(themeManager.getColor("primary"));
                } else {
                    // Other outlined buttons use onSurface
                    button.setTextColor(themeManager.getColor("onSurface"));
                }
                
                button.setStrokeColor(ColorStateList.valueOf(themeManager.getColor("outline")));
                button.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density));
                button.setRippleColor(ColorStateList.valueOf(lightOutlineRippleColor));
                break;
            case "text":
                // Text button: transparent background, colored text only
                button.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                
                // Check if this is an export/import button and use primary color for better visibility
                String textResourceName = "";
                try {
                    textResourceName = button.getContext().getResources().getResourceEntryName(button.getId()).toLowerCase();
                } catch (Exception e) {
                    // Ignore, use default
                }
                
                if (textResourceName.contains("import") || textResourceName.contains("export")) {
                    // Export/Import buttons use primary color for better visibility
                    button.setTextColor(themeManager.getColor("primary"));
                } else {
                    // Other text buttons use onSurface
                    button.setTextColor(themeManager.getColor("onSurface"));
                }
                
                button.setRippleColor(ColorStateList.valueOf(lightRippleColor));
                break;
            case "filled":
            default:
                // Filled button: colored background, contrasting text
                ColorStateList enabledStates = getThemedColorStateList("primary", "surfaceVariant");
                button.setBackgroundTintList(enabledStates);
                button.setTextColor(themeManager.getColor("onPrimary"));
                button.setRippleColor(ColorStateList.valueOf(lightSurfaceRippleColor));
                break;
        }
    }
    
    /**
     * Determine button type based on current styling
     */
    private static String determineButtonType(MaterialButton button) {
        // Check view tag for button type hint
        Object tag = button.getTag();
        if (tag != null) {
            String tagStr = tag.toString().toLowerCase();
            if (tagStr.contains("outlined")) return "outlined";
            if (tagStr.contains("text")) return "text";
        }
        
        // Check if button has outlined style characteristics
        if (button.getStrokeWidth() > 0) {
            return "outlined";
        }
        
        // Check if background is transparent
        if (button.getBackgroundTintList() != null && 
            button.getBackgroundTintList().equals(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT))) {
            return "text";
        }
        
        // Check button ID to determine type
        String resourceName = "";
        try {
            resourceName = button.getContext().getResources().getResourceEntryName(button.getId()).toLowerCase();
        } catch (Exception e) {
            // Ignore, use default
        }
        
        if (resourceName.contains("import") || resourceName.contains("export")) {
            // Import/Export buttons are typically outlined
            return "outlined";
        }
        
        return "filled";
    }
    
    /**
     * Apply theme colors to a TextView
     */
    public static void applyThemeToTextView(TextView textView, String colorType) {
        try {
            ThemeManager themeManager = ThemeManager.getInstance();
            if (themeManager != null && themeManager.isThemeLoaded()) {
                textView.setTextColor(themeManager.getColor(colorType));
            } else {
                // Fallback to default colors if theme not ready
                switch (colorType) {
                    case "onSurface":
                        textView.setTextColor(Color.parseColor("#FFFFFF"));
                        break;
                    case "onSurfaceVariant":
                        textView.setTextColor(Color.parseColor("#CCCCCC"));
                        break;
                    default:
                        textView.setTextColor(Color.parseColor("#FFFFFF"));
                        break;
                }
            }
        } catch (Exception e) {
            // Fallback to default colors on error
            textView.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }
    
    /**
     * Apply theme colors to a MaterialRadioButton
     */
    public static void applyThemeToRadioButton(MaterialRadioButton radioButton, Context context) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        ColorStateList colorStateList = new ColorStateList(
            new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
            },
            new int[]{
                themeManager.getColor("primary"),
                themeManager.getColor("onSurfaceVariant")
            }
        );
        radioButton.setButtonTintList(colorStateList);
    }
    
    /**
     * Create a circular ripple drawable with theme colors
     */
    public static RippleDrawable createCircularRipple(String colorName) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(android.graphics.Color.TRANSPARENT);
        
        return new RippleDrawable(
            ColorStateList.valueOf(createOptimizedRippleColor(colorName, "button")),
            null,
            circle
        );
    }
    
    /**
     * Apply theme background to a view
     */
    public static void applyThemeBackground(View view, String colorName) {
        ThemeManager themeManager = ThemeManager.getInstance();
        view.setBackgroundColor(themeManager.getColor(colorName));
    }
    
    /**
     * Get themed color state list for various states
     */
    public static ColorStateList getThemedColorStateList(String enabledColor, String disabledColor) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        return new ColorStateList(
            new int[][]{
                new int[]{android.R.attr.state_enabled},
                new int[]{-android.R.attr.state_enabled}
            },
            new int[]{
                themeManager.getColor(enabledColor),
                themeManager.getColor(disabledColor)
            }
        );
    }
    
    /**
     * Apply theme to the root view (typically the activity's main layout)
     */
    public static void applyThemeToRootView(View rootView) {
        ThemeManager themeManager = ThemeManager.getInstance();
        rootView.setBackgroundColor(themeManager.getColor("background"));
        
        // Recursively apply themes to common view types
        applyThemeToViewHierarchy(rootView);
    }
    
    /**
     * Recursively apply theme to all views in the hierarchy
     */
    private static void applyThemeToViewHierarchy(View view) {
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                applyThemeToViewHierarchy(child);
            }
        }
        
        // Apply theme to specific view types more selectively
        if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            // Always update colors, but preserve stroke width if already set
            ThemeManager themeManager = ThemeManager.getInstance();
            card.setCardBackgroundColor(themeManager.getColor("surface"));
            card.setStrokeColor(themeManager.getColor("outline"));
            // Only set stroke width if it's currently 0 (not manually set)
            if (card.getStrokeWidth() == 0) {
                card.setStrokeWidth((int) (1 * view.getContext().getResources().getDisplayMetrics().density));
            }
        } else if (view instanceof MaterialButton) {
            // Always apply theming to override hardcoded colors from XML
            MaterialButton button = (MaterialButton) view;
            applyThemeToButton(button, view.getContext());
        } else if (view instanceof MaterialRadioButton) {
            applyThemeToRadioButton((MaterialRadioButton) view, view.getContext());
        } else if (view instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
            applyThemeToBottomNavigation(view);
        } else if (view instanceof TabLayout) {
            applyThemeToTabLayout((TabLayout) view);
        } else if (view instanceof TextInputLayout) {
            applyThemeToTextInputLayout((TextInputLayout) view);
        } else if (view instanceof EditText && !(view instanceof TextInputEditText)) {
            // Only theme EditTexts that don't have custom styling
            EditText editText = (EditText) view;
            if (editText.getBackground() == null || editText.getCurrentTextColor() == android.graphics.Color.BLACK) {
                applyThemeToEditText(editText);
            }
        } else if (view instanceof TextView) {
            // Apply theme to TextViews when they still have default colors
            TextView textView = (TextView) view;
            applyThemeToTextViewIfDefault(textView);
        }
        // Removed automatic ImageView theming to preserve custom styling
    }

    /**
     * Apply theme to TextView only if it appears to still be using default system colors,
     * to avoid overriding explicitly styled texts. Honors an opt-out via tag containing "preserveColor".
     */
    private static void applyThemeToTextViewIfDefault(TextView textView) {
        try {
            // Opt-out: if tag asks to preserve color
            Object tag = textView.getTag();
            if (tag != null) {
                String t = tag.toString().toLowerCase();
                if (t.contains("preservecolor") || t.contains("no-theme") || t.contains("notheme")) {
                    return;
                }
            }

            int color = textView.getCurrentTextColor();
            if (looksLikeDefaultTextColor(color)) {
                // Heuristic: large/bold -> primary text; otherwise secondary
                boolean isBold = textView.getTypeface() != null && textView.getTypeface().isBold();
                float sp = textView.getTextSize() / textView.getResources().getDisplayMetrics().scaledDensity;
                if (isBold || sp >= 16f) {
                    applyThemeToTextView(textView, "onSurface");
                } else {
                    applyThemeToTextView(textView, "onSurfaceVariant");
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Detects common default system text colors (black/white/near-black) that indicate no explicit theming.
     */
    private static boolean looksLikeDefaultTextColor(int color) {
        // Fully opaque black or white
        if (color == android.graphics.Color.BLACK || color == android.graphics.Color.WHITE) return true;
        // Common near-black defaults (#FF212121, #FF000000, Material defaults)
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        // Consider as default if fully opaque and very dark (typical default)
        if (a == 0xFF && r < 40 && g < 40 && b < 40) return true;
        // Consider as default if high contrast white-ish
        if (a == 0xFF && r > 240 && g > 240 && b > 240) return true;
        return false;
    }
    
    /**
 * Apply theme colors to BottomNavigationView
 */
public static void applyThemeToBottomNavigation(View bottomNavView) {
    if (bottomNavView instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
            (com.google.android.material.bottomnavigation.BottomNavigationView) bottomNavView;
        
        ThemeManager themeManager = ThemeManager.getInstance();
        
        // Try to use bottom_navigation color first, fallback to surface
        int backgroundColor;
        try {
            // Check if the theme has a specific bottom_navigation color
            backgroundColor = themeManager.getColor("bottom_navigation");
        } catch (Exception e) {
            // Fallback to surface color if bottom_navigation is not defined
            backgroundColor = themeManager.getColor("surface");
        }
        
        // Apply background color
        bottomNav.setBackgroundColor(backgroundColor);
        
        // Create color state list for selected/unselected states
        ColorStateList itemColorStateList = new ColorStateList(
            new int[][]{
                new int[]{android.R.attr.state_checked},      // Selected item
                new int[]{-android.R.attr.state_checked}      // Unselected item
            },
            new int[]{
                themeManager.getColor("primary"),              // Selected: primary color
                themeManager.getColor("onSurfaceVariant")     // Unselected: onSurfaceVariant
            }
        );
        
        // Apply colors to both text and icons
        bottomNav.setItemTextColor(itemColorStateList);
        bottomNav.setItemIconTintList(itemColorStateList);
        
        // Add ripple effect for better interaction feedback
        try {
            int rippleColor = createOptimizedRippleColor("primary", "button");
            bottomNav.setItemRippleColor(ColorStateList.valueOf(rippleColor));
        } catch (Exception e) {
            // Ignore ripple errors, some Android versions might not support this
        }
    }
}

    /**
     * Apply theme colors to TabLayout (top navigation)
     */
    public static void applyThemeToTabLayout(TabLayout tabLayout) {
        try {
            ThemeManager themeManager = ThemeManager.getInstance();
            // Background should match fragment background
            tabLayout.setBackgroundColor(themeManager.getColor("background"));

            // Text colors for selected/unselected
            ColorStateList textColors = new ColorStateList(
                new int[][]{
                    new int[]{android.R.attr.state_selected},
                    new int[]{-android.R.attr.state_selected}
                },
                new int[]{
                    themeManager.getColor("primary"),
                    themeManager.getColor("onSurfaceVariant")
                }
            );
            tabLayout.setTabTextColors(textColors);

            // Indicator color
            tabLayout.setSelectedTabIndicatorColor(themeManager.getColor("primary"));

            // Also color icons in tabs if any
            tabLayout.setTabIconTint(textColors);
        } catch (Exception ignored) {}
    }
    
    /**
     * Apply theme colors to TextInputLayout
     */
    public static void applyThemeToTextInputLayout(TextInputLayout textInputLayout) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        textInputLayout.setBoxBackgroundColor(themeManager.getColor("surfaceVariant"));
        textInputLayout.setHintTextColor(getThemedColorStateList("onSurfaceVariant", "onSurfaceVariant"));
        textInputLayout.setBoxStrokeColor(themeManager.getColor("outline"));
        
        // Apply theme to the EditText inside
        EditText editText = textInputLayout.getEditText();
        if (editText != null) {
            editText.setTextColor(themeManager.getColor("onSurface"));
            editText.setHintTextColor(themeManager.getColor("onSurfaceVariant"));
        }
    }
    
    /**
     * Apply theme colors to EditText
     */
    public static void applyThemeToEditText(EditText editText) {
        ThemeManager themeManager = ThemeManager.getInstance();
        editText.setTextColor(themeManager.getColor("onSurface"));
        editText.setHintTextColor(themeManager.getColor("onSurfaceVariant"));
        editText.setBackgroundTintList(ColorStateList.valueOf(themeManager.getColor("outline")));
    }
    
    /**
     * Apply theme colors to share/action buttons
     */
    public static void applyThemeToActionButton(MaterialButton button, String colorType) {
        ThemeManager themeManager = ThemeManager.getInstance();
        
        switch (colorType) {
            case "primary":
                button.setBackgroundTintList(ColorStateList.valueOf(themeManager.getColor("primary")));
                button.setTextColor(themeManager.getColor("onPrimary"));
                break;
            case "secondary":
                button.setBackgroundTintList(ColorStateList.valueOf(themeManager.getColor("secondary")));
                button.setTextColor(themeManager.getColor("onSecondary"));
                break;
            case "error":
                button.setBackgroundTintList(ColorStateList.valueOf(themeManager.getColor("error")));
                button.setTextColor(themeManager.getColor("onError"));
                break;
            case "success":
                button.setBackgroundTintList(ColorStateList.valueOf(themeManager.getColor("success")));
                button.setTextColor(themeManager.getColor("onSurface"));
                break;
            default:
                applyThemeToButton(button, button.getContext());
                break;
        }
    }
    
    /**
     * Apply theme colors to Material AlertDialog
     */
    public static void applyThemeToDialog(androidx.appcompat.app.AlertDialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;
        
        try {
            // Apply background color to dialog
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            
            // Get the root view and apply theme
            View dialogView = dialog.findViewById(android.R.id.content);
            if (dialogView != null) {
                dialogView.setBackgroundColor(ThemeManager.getInstance().getColor("surface"));
            }
            
            // Apply theme to buttons
            android.widget.Button positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            android.widget.Button negativeButton = dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE);
            android.widget.Button neutralButton = dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL);
            
            if (positiveButton != null) {
                positiveButton.setTextColor(ThemeManager.getInstance().getColor("primary"));
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(ThemeManager.getInstance().getColor("onSurfaceVariant"));
            }
            if (neutralButton != null) {
                neutralButton.setTextColor(ThemeManager.getInstance().getColor("onSurfaceVariant"));
            }
            
        } catch (Exception e) {
            // Ignore theming errors
        }
    }

    /**
     * Apply theme colors to MaterialSwitch
     */
    public static void applyThemeToSwitch(MaterialSwitch materialSwitch, Context context) {
        try {
            ThemeManager themeManager = ThemeManager.getInstance();
            if (themeManager != null && themeManager.isThemeLoaded()) {
                // Use toggle colors if available, otherwise fall back to theme colors
                int trackColor, trackCheckedColor, thumbColor, thumbCheckedColor;
                
                if (themeManager.hasToggleColors()) {
                    // Use theme-specific toggle colors
                    trackColor = themeManager.getToggleColor("track");
                    trackCheckedColor = themeManager.getToggleColor("trackChecked");
                    thumbColor = themeManager.getToggleColor("thumb");
                    thumbCheckedColor = themeManager.getToggleColor("thumbChecked");
                } else {
                    // Fall back to theme colors
                    trackColor = themeManager.getColor("surfaceVariant");
                    trackCheckedColor = themeManager.getColor("primary");
                    thumbColor = themeManager.getColor("onSurface");
                    thumbCheckedColor = themeManager.getColor("onSurface");
                }
                
                // Create color state lists for different states
                ColorStateList trackColorStateList = new ColorStateList(
                    new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                    },
                    new int[]{trackCheckedColor, trackColor}
                );
                
                ColorStateList thumbColorStateList = new ColorStateList(
                    new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                    },
                    new int[]{thumbCheckedColor, thumbColor}
                );
                
                // Apply colors to the switch
                materialSwitch.setTrackTintList(trackColorStateList);
                materialSwitch.setThumbTintList(thumbColorStateList);
                
                // Note: MaterialSwitch doesn't support setRippleColor, so we skip that
                // The ripple effect is handled internally by the MaterialSwitch component
            } else {
                // Fallback to default colors if theme not ready
                materialSwitch.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#2A2A2A")));
                materialSwitch.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
            }
        } catch (Exception e) {
            // Fallback to default colors on error
            materialSwitch.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#2A2A2A")));
            materialSwitch.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
        }
    }

    /**
     * Create a lighter ripple color for better visibility
     */
    private static int createLightRippleColor(int baseColor) {
        // Convert to HSV for better color manipulation
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(baseColor, hsv);
        
        // For very dark colors, create a light gray ripple
        if (hsv[2] < 0.3f) {
            // Create a light gray with slight tint of the original color
            hsv[0] = hsv[0]; // Keep hue
            hsv[1] = 0.1f;   // Very low saturation
            hsv[2] = 0.7f;   // High lightness
        } else {
            // For lighter colors, increase lightness and reduce saturation
            hsv[1] = Math.max(0.15f, hsv[1] * 0.5f); // Reduce saturation more
            hsv[2] = Math.min(0.85f, hsv[2] * 1.4f); // Increase lightness more
        }
        
        // Convert back to color and add transparency
        int lightColor = android.graphics.Color.HSVToColor(hsv);
        
        // Make it semi-transparent for a subtle ripple effect
        return (lightColor & 0x00FFFFFF) | 0x50000000; // 31% opacity for better visibility
    }
    
    /**
     * Create a ripple color optimized for specific UI element types
     */
    public static int createOptimizedRippleColor(String colorName, String elementType) {
        ThemeManager themeManager = ThemeManager.getInstance();
        int baseColor = themeManager.getColor(colorName);
        
        switch (elementType) {
            case "button":
                // Buttons get lighter, more visible ripples
                return createLightRippleColor(baseColor);
            case "card":
                // Cards get subtle, elegant ripples
                return createSubtleRippleColor(baseColor);
            case "switch":
                // Switches get medium visibility ripples
                return createMediumRippleColor(baseColor);
            default:
                return createLightRippleColor(baseColor);
        }
    }
    
    /**
     * Create a subtle ripple color for cards and other subtle elements
     */
    private static int createSubtleRippleColor(int baseColor) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(baseColor, hsv);
        
        // Create very subtle ripple
        hsv[1] = Math.max(0.05f, hsv[1] * 0.3f); // Very low saturation
        hsv[2] = Math.min(0.8f, hsv[2] * 1.2f);  // Moderate lightness increase
        
        int subtleColor = android.graphics.Color.HSVToColor(hsv);
        return (subtleColor & 0x00FFFFFF) | 0x30000000; // 19% opacity for subtle effect
    }
    
    /**
     * Create a medium visibility ripple color for switches and other medium elements
     */
    private static int createMediumRippleColor(int baseColor) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(baseColor, hsv);
        
        // Create medium visibility ripple
        hsv[1] = Math.max(0.1f, hsv[1] * 0.4f);  // Low saturation
        hsv[2] = Math.min(0.8f, hsv[2] * 1.3f);  // Good lightness increase
        
        int mediumColor = android.graphics.Color.HSVToColor(hsv);
        return (mediumColor & 0x00FFFFFF) | 0x40000000; // 25% opacity for medium effect
    }

    /**
     * Refresh all ripple effects in a view hierarchy
     */
    public static void refreshRippleEffects(View view) {
        try {
            if (view instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) view;
                // Reapply theme to refresh ripple effects
                applyThemeToCard(card, view.getContext());
            } else if (view instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) view;
                // Reapply theme to refresh ripple effects
                applyThemeToButton(button, view.getContext());
            }
            
            // Recursively refresh child views
            if (view instanceof android.view.ViewGroup) {
                android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View child = viewGroup.getChildAt(i);
                    refreshRippleEffects(child);
                }
            }
        } catch (Exception e) {
            // Handle error gracefully
        }
    }

    /**
     * Apply theme colors to a MaterialCardView with fade animation
     */
    public static void applyThemeToCardWithAnimation(MaterialCardView card, Context context, int duration) {
        try {
            ThemeManager themeManager = ThemeManager.getInstance();
            
            if (themeManager != null && themeManager.isThemeLoaded()) {
                // Get current colors
                int currentBackground = card.getCardBackgroundColor().getDefaultColor();
                int currentStroke = card.getStrokeColor();
                
                // Get target colors
                int targetBackground = themeManager.getColor("surface");
                int targetStroke = themeManager.getColor("outline");
                
                // Animate background color transition
                animateBackgroundColorTransition(card, currentBackground, targetBackground, duration);
                
                // Animate stroke color transition
                animateColorTransition(currentStroke, targetStroke, duration, 
                    va -> {
                        int animated = (int) va.getAnimatedValue();
                        card.setStrokeColor(animated);
                    });
                
                // Apply other properties immediately
                card.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density));
                card.setCardElevation(0f);
                
                // Create ripple effect with theme colors
                RippleDrawable ripple = new RippleDrawable(
                    ColorStateList.valueOf(createOptimizedRippleColor("onSurface", "card")),
                    null,
                    null
                );
                card.setForeground(ripple);
                
                // DO NOT override corner radius - preserve existing value
                // preserveCornerRadius(card, context);
            }
        } catch (Exception e) {
            // Fallback to immediate application on error
            applyThemeToCard(card, context);
        }
    }
    
    /**
     * Apply theme colors to a MaterialButton with fade animation
     */
    public static void applyThemeToButtonWithAnimation(MaterialButton button, Context context, int duration) {
        try {
            ThemeManager themeManager = ThemeManager.getInstance();
            
            // Determine button type and apply appropriate styling
            String buttonType = determineButtonType(button);
            
            // Create lighter ripple colors for better visibility
            int lightRippleColor = createOptimizedRippleColor("primary", "button");
            int lightSurfaceRippleColor = createOptimizedRippleColor("surfaceVariant", "button");
            int lightOutlineRippleColor = createOptimizedRippleColor("outline", "button");
            
            switch (buttonType) {
                case "outlined":
                    // Animate text color transition
                    int currentTextColor = button.getCurrentTextColor();
                    int targetTextColor = getExportImportButtonTextColor(button, themeManager);
                    animateTextColorTransition(button, currentTextColor, targetTextColor, duration);
                    
                    // Animate stroke color transition
                    if (button.getStrokeColor() != null) {
                        int currentStroke = button.getStrokeColor().getDefaultColor();
                        int targetStroke = themeManager.getColor("outline");
                        animateColorTransition(currentStroke, targetStroke, duration, 
                            va -> button.setStrokeColor(ColorStateList.valueOf((int) va.getAnimatedValue())));
                    }
                    
                    // Apply other properties immediately
                    button.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                    button.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density));
                    button.setRippleColor(ColorStateList.valueOf(lightOutlineRippleColor));
                    break;
                    
                case "text":
                    // Animate text color transition
                    int currentTextColorText = button.getCurrentTextColor();
                    int targetTextColorText = getExportImportButtonTextColor(button, themeManager);
                    animateTextColorTransition(button, currentTextColorText, targetTextColorText, duration);
                    
                    // Apply other properties immediately
                    button.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                    button.setRippleColor(ColorStateList.valueOf(lightRippleColor));
                    break;
                    
                case "filled":
                default:
                    // Animate background color transition
                    if (button.getBackgroundTintList() != null) {
                        int currentBackground = button.getBackgroundTintList().getDefaultColor();
                        int targetBackground = themeManager.getColor("primary");
                        animateColorTransition(currentBackground, targetBackground, duration, 
                            va -> button.setBackgroundTintList(ColorStateList.valueOf((int) va.getAnimatedValue())));
                    }
                    
                    // Animate text color transition
                    int currentTextColorFilled = button.getCurrentTextColor();
                    int targetTextColorFilled = themeManager.getColor("onPrimary");
                    animateTextColorTransition(button, currentTextColorFilled, targetTextColorFilled, duration);
                    
                    button.setRippleColor(ColorStateList.valueOf(lightSurfaceRippleColor));
                    break;
            }
        } catch (Exception e) {
            // Fallback to immediate application on error
            applyThemeToButton(button, context);
        }
    }
    
    /**
     * Helper method to get the appropriate text color for export/import buttons
     */
    private static int getExportImportButtonTextColor(MaterialButton button, ThemeManager themeManager) {
        String resourceName = "";
        try {
            resourceName = button.getContext().getResources().getResourceEntryName(button.getId()).toLowerCase();
        } catch (Exception e) {
            // Ignore, use default
        }
        
        if (resourceName.contains("import") || resourceName.contains("export")) {
            return themeManager.getColor("primary");
        } else {
            return themeManager.getColor("onSurface");
        }
    }
    
    /**
     * Animate color transition from one color to another with better error handling
     */
    public static void animateColorTransition(int fromColor, int toColor, int duration, 
                                            android.animation.ValueAnimator.AnimatorUpdateListener listener) {
        if (fromColor == toColor) return; // No animation needed
        
        try {
            ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
            colorAnimator.setDuration(duration);
            colorAnimator.addUpdateListener(animation -> {
                try {
                    if (listener != null) {
                        listener.onAnimationUpdate(animation);
                    }
                } catch (Exception e) {
                    // Handle error gracefully
                }
            });
            colorAnimator.start();
        } catch (Exception e) {
            // Fallback to immediate change
            if (listener != null) {
                try {
                    // Create a fake animation object with the target value
                    ValueAnimator fakeAnim = ValueAnimator.ofInt(toColor);
                    fakeAnim.setCurrentPlayTime(duration);
                    listener.onAnimationUpdate(fakeAnim);
                } catch (Exception ex) {
                    // Ignore fallback errors
                }
            }
        }
    }
    
    /**
     * Apply fade animation to view background color transition
     */
    public static void animateBackgroundColorTransition(View view, int fromColor, int toColor, int duration) {
        if (fromColor == toColor) return; // No animation needed
        
        try {
            ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
            colorAnimator.setDuration(duration);
            colorAnimator.addUpdateListener(animation -> {
                try {
                    int animatedColor = (int) animation.getAnimatedValue();
                    view.setBackgroundColor(animatedColor);
                } catch (Exception e) {
                    // Handle error gracefully
                }
            });
            colorAnimator.start();
        } catch (Exception e) {
            // Fallback to immediate change
            view.setBackgroundColor(toColor);
        }
    }
    
    /**
     * Apply fade animation to text color transition
     */
    public static void animateTextColorTransition(TextView textView, int fromColor, int toColor, int duration) {
        if (fromColor == toColor) return; // No animation needed
        
        try {
            ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
            colorAnimator.setDuration(duration);
            colorAnimator.addUpdateListener(animation -> {
                try {
                    int animatedColor = (int) animation.getAnimatedValue();
                    textView.setTextColor(animatedColor);
                } catch (Exception e) {
                    // Handle error gracefully
                }
            });
            colorAnimator.start();
        } catch (Exception e) {
            // Fallback to immediate change
            textView.setTextColor(toColor);
        }
    }
}