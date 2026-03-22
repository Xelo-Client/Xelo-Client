package com.origin.launcher.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseThemedActivity extends AppCompatActivity {
    private static final String TAG = "BaseThemedActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize ThemeManager first
        initializeThemeManager();
        
        // Apply theme after initialization
        applyTheme();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Reapply theme when activity resumes (in case theme was changed)
        applyTheme();
    }
    
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        
        // Apply theme to root view after content is set
        applyThemeToViews();
    }
    
    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        
        // Apply theme to root view after content is set
        applyThemeToViews();
    }
    
    private void initializeThemeManager() {
        try {
            // Initialize ThemeManager if not already done
            ThemeManager.getInstance(this);
            Log.d(TAG, "ThemeManager initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ThemeManager", e);
        }
    }
    
    private void applyTheme() {
        try {
            // Ensure ThemeManager is initialized and ready
            ThemeManager themeManager = ThemeManager.getInstance();
            if (themeManager != null) {
                Log.d(TAG, "Theme applied successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply theme", e);
            // Try to reinitialize
            initializeThemeManager();
        }
    }
    
    private void applyThemeToViews() {
        try {
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                ThemeUtils.applyThemeToRootView(rootView);
                Log.d(TAG, "Theme applied to views successfully");
            }
            
            // Allow subclasses to apply additional theming
            onApplyTheme();
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply theme to views", e);
        }
    }
    
    /**
     * Override this method in subclasses to apply theme to specific views
     */
    protected void onApplyTheme() {
        // Default implementation does nothing
    }
    
    /**
     * Call this method when theme changes to refresh the current activity
     */
    protected void refreshTheme() {
        applyThemeToViews();
    }
}