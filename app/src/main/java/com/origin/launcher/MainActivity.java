package com.origin.launcher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.net.Uri;
import com.bumptech.glide.Glide;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction; 
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.animation.OvershootInterpolator;
import android.os.Handler;
import androidx.core.content.res.ResourcesCompat;
import android.content.res.ColorStateList;
import com.origin.launcher.Adapter.CreditsAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;

import java.util.List;
import java.util.ArrayList;

import androidx.annotation.NonNull;

import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import com.origin.launcher.Launcher.MinecraftLauncher;
import com.origin.launcher.versions.GameVersion;
import com.origin.launcher.versions.VersionManager;
import com.origin.launcher.databinding.ActivityMainBinding;
import com.origin.launcher.FeatureSettings;
import com.origin.launcher.animation.DynamicAnim;
import com.origin.launcher.HomeFragment;
import android.widget.Button;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModSizeStore;

public class MainActivity extends BaseThemedActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_DISCLAIMER_SHOWN = "disclaimer_shown";
    private static final String KEY_THEMES_DIALOG_SHOWN = "themes_dialog_shown";
private static final String KEY_CREDITS_SHOWN = "credits_shown";
private static final String KEY_STORAGE_PERMS_ASKED = "storage_perms_asked";
private static final int REQ_STORAGE_PERMS = 100;

    private SettingsFragment settingsFragment;
    private int currentFragmentIndex = 0;
    private LinearProgressIndicator globalProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);


        checkFirstLaunch();
        InbuiltModSizeStore.getInstance().init(getApplicationContext());
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        ThemeUtils.applyThemeToBottomNavigation(bottomNavigationView);
        
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String presenceActivity = "";
            int newIndex = -1;
            
            if (item.getItemId() == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
                presenceActivity = "In Home";
                newIndex = 0;
            } else if (item.getItemId() == R.id.navigation_dashboard) {
                selectedFragment = new DashboardFragment();
                presenceActivity = "In Dashboard";
                newIndex = 1;
            } else if (item.getItemId() == R.id.navigation_settings) {
                if (settingsFragment == null) {
                    settingsFragment = new SettingsFragment();
                }
                selectedFragment = settingsFragment;
                presenceActivity = "In Settings";
                newIndex = 2;
            }

            if (selectedFragment != null) {
                // Determine direction based on tab indices
                boolean isForward = newIndex > getCurrentFragmentIndex();
                
                navigateToFragmentWithAnimation(selectedFragment, isForward);
                
                setCurrentFragmentIndex(newIndex);
                
                DiscordRPCHelper.getInstance().updatePresence(presenceActivity, "Using the best MCPE Client");
                
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
            setCurrentFragmentIndex(0);
        }
    }
    
    private int getCurrentFragmentIndex() {
        return currentFragmentIndex;
    }

    private void setCurrentFragmentIndex(int index) {
        this.currentFragmentIndex = index;
    }

    private void navigateToFragmentWithAnimation(Fragment fragment, boolean isForward) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        if (isForward) {
            transaction.setCustomAnimations(
                R.anim.slide_in_right, 
                R.anim.slide_out_left, 
                R.anim.slide_in_left,  
                R.anim.slide_out_right 
            );
        } else {
            transaction.setCustomAnimations(
                R.anim.slide_in_left,  
                R.anim.slide_out_right,  
                R.anim.slide_in_right,  
                R.anim.slide_out_left 
            );
        }
        
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

 

private void checkFirstLaunch() {
    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    boolean disclaimerShown = prefs.getBoolean(KEY_DISCLAIMER_SHOWN, false);
    boolean themesDialogShown = prefs.getBoolean(KEY_THEMES_DIALOG_SHOWN, false);
    boolean creditsShown = prefs.getBoolean(KEY_CREDITS_SHOWN, false);
    boolean storageAsked = prefs.getBoolean(KEY_STORAGE_PERMS_ASKED, false);

    if (isFirstLaunch) {
        if (!storageAsked) {
            ensureStorageAccess(prefs);
            return;
        }
        
        showFirstLaunchDialog(prefs, disclaimerShown, themesDialogShown, creditsShown);
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    } else if (!storageAsked) {
        ensureStorageAccess(prefs);
    } else if (!disclaimerShown) {
        showDisclaimerDialog(prefs);
    } else if (!creditsShown) {
        showThanksDialog(prefs);
    }
}

private void ensureStorageAccess(SharedPreferences prefs) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_STORAGE_PERMS);
            return;
        }
    } else {
        int read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (read != PackageManager.PERMISSION_GRANTED || write != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQ_STORAGE_PERMS);
            return;
        }
    }
    
    prefs.edit().putBoolean(KEY_STORAGE_PERMS_ASKED, true).apply();
    continueFirstLaunchFlow(prefs);
}

private void createNoMediaFile() {
        try {
            java.io.File baseDir = new java.io.File(android.os.Environment.getExternalStorageDirectory(), "games/xelo_client");
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            java.io.File noMediaFile = new java.io.File(baseDir, ".nomedia");
            if (!noMediaFile.exists()) {
                noMediaFile.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

private void continueFirstLaunchFlow(SharedPreferences prefs) {
    boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
    boolean disclaimerShown = prefs.getBoolean(KEY_DISCLAIMER_SHOWN, false);
    boolean creditsShown = prefs.getBoolean(KEY_CREDITS_SHOWN, false);
    boolean themesDialogShown = prefs.getBoolean(KEY_THEMES_DIALOG_SHOWN, false);

    if (isFirstLaunch) {
        showFirstLaunchDialog(prefs, disclaimerShown, themesDialogShown, creditsShown);
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    } else if (!disclaimerShown) {
        showDisclaimerDialog(prefs);
    } else if (!creditsShown) {
        showThanksDialog(prefs);
    } else if (!themesDialogShown) {
        
    }
}

private void showFirstLaunchDialog(SharedPreferences prefs,
                                   boolean disclaimerShown,
                                   boolean themesDialogShown,
                                   boolean creditsShown) {
    new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Welcome to Xelo Client")
            .setMessage("Launch Minecraft once before doing anything, to make the config load properly")
            .setIcon(R.drawable.ic_info)
            .setPositiveButton("Proceed", (dialog, which) -> {
                dialog.dismiss();
                if (!disclaimerShown) {
                    showDisclaimerDialog(prefs);
                } else if (!creditsShown) {
                    showThanksDialog(prefs);
                } else if (!themesDialogShown) {
                    
                }
            })
            .setCancelable(false)
            .show();
}

private void showDisclaimerDialog(SharedPreferences prefs) {
    new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Important Disclaimer")
            .setMessage("This application is not affiliated with, endorsed by, or related to Mojang Studios, Microsoft Corporation, or any of their subsidiaries." 
+
                       "Minecraft is a trademark of Mojang Studios. This is an independent third-party launcher." 
+
                       "By clicking 'I Understand', you acknowledge that you use this launcher at your own risk and that the developers are not responsible for any issues that may arise.")
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("I Understand", (dialog, which) -> {
                dialog.dismiss();
                prefs.edit().putBoolean(KEY_DISCLAIMER_SHOWN, true).apply();
               
                boolean creditsShown = prefs.getBoolean(KEY_CREDITS_SHOWN, false);
                boolean themesDialogShown = prefs.getBoolean(KEY_THEMES_DIALOG_SHOWN, false);
                if (!creditsShown) {
                    showThanksDialog(prefs);
                } else if (!themesDialogShown) {
                    
                }
            })
            .setCancelable(false)
            .show();
}

private void showThanksDialog(SharedPreferences prefs) {
    LayoutInflater inflater = LayoutInflater.from(this);
    View customView = inflater.inflate(R.layout.dialog_credits, null);

    RecyclerView recycler = customView.findViewById(R.id.credits_recycler);

    List<CreditsAdapter.CreditCard> cards = new ArrayList<>();
    cards.add(new CreditsAdapter.CreditCard("Yami", "Sukrisus", "https://avatars.githubusercontent.com/u/99645769?v=4", "OWNER"));
    cards.add(new CreditsAdapter.CreditCard("VCX", "Viablecobra", "https://avatars.githubusercontent.com/u/88580298?v=4", "I am viable👍🏻"));
    cards.add(new CreditsAdapter.CreditCard("Light", "RadiantByte", "https://avatars.githubusercontent.com/u/198057285?v=4", "💭"));
    cards.add(new CreditsAdapter.CreditCard("Kitsuri", "Kitsuri-Studios", "https://avatars.githubusercontent.com/u/220755073?v=4", "One Place For All Case: Native Development..."));
    cards.add(new CreditsAdapter.CreditCard("GX", "dreamguxiang", "https://avatars.githubusercontent.com/u/62042544?v=4", "No Tag line Needed, Already Perfect"));

    CreditsAdapter adapter = new CreditsAdapter(this, cards);
    recycler.setAdapter(adapter);

    LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
    recycler.setLayoutManager(layoutManager);

    
    PagerSnapHelper snapHelper = new PagerSnapHelper();
    snapHelper.attachToRecyclerView(recycler);

    
    recycler.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                               int oldLeft, int oldTop, int oldRight, int oldBottom) {
        recycler.removeOnLayoutChangeListener(this);
        RecyclerView.ViewHolder vh = recycler.findViewHolderForAdapterPosition(0);
        if (vh != null) {
            int[] snapDistance = snapHelper.calculateDistanceToFinalSnap(
                    recycler.getLayoutManager(), vh.itemView);
            if (snapDistance != null) {
                recycler.smoothScrollBy(snapDistance[0], snapDistance[1]);
            }
        }
    }
});

    
    recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
            for (int i = 0; i < rv.getChildCount(); i++) {
                View child = rv.getChildAt(i);
                int center = rv.getWidth() / 2;
                int childCenter = (child.getLeft() + child.getRight()) / 2;
                float distance = Math.abs(center - childCenter);
                float scale = Math.max(0.85f, 1f - (distance / rv.getWidth()) * 0.3f);
                child.setScaleX(scale);
                child.setScaleY(scale);
            }
        }
    });

    new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setView(customView)
            .setPositiveButton("Continue", (dialog, which) -> {
                dialog.dismiss();
                prefs.edit().putBoolean(KEY_CREDITS_SHOWN, true).apply();
                
            })
            .setCancelable(false)
            .show();
}

private void animateCardsSequentially(LinearLayout container, int index) {
    if (index >= container.getChildCount()) return;
    
    View card = container.getChildAt(index);
    animateCardEntrance(card);
    
    new Handler().postDelayed(() -> {
        animateCardsSequentially(container, index + 1);
    }, 100);  
}


private void addCreditCard(LinearLayout container, String handle, String username, String picUrl, String tagline) {
    LayoutInflater inflater = LayoutInflater.from(this);
    View card = inflater.inflate(R.layout.credit_card_item, container, false);

    ImageView profilePic = card.findViewById(R.id.profile_pic);
    TextView profileName = card.findViewById(R.id.profile_name);
    TextView profileTagline = card.findViewById(R.id.profile_tagline);

    Glide.with(this).load(picUrl).circleCrop().into(profilePic);

    profileName.setText(username);
    profileTagline.setText(tagline);

    if (handle.equals("Yami")) {
        profileName.setTextColor(Color.parseColor("#FFD700"));
        profileTagline.setTextColor(Color.parseColor("#FFD700"));
        card.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF8DC")));
    }

    if (handle.equals("VCX")) {
        int neon = Color.parseColor("#00FFFF"); // neon blue/cyan
        profileName.setTextColor(neon);
        profileTagline.setTextColor(neon);
    }

    
    animateCardEntrance(card);

    
    card.setScaleX(0.9f);
    card.setScaleY(0.9f);

    
    card.setOnClickListener(v -> {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/" + username));
        startActivity(browserIntent);
    });

    container.addView(card);
}

private void animateCardEntrance(View card) {
    card.setAlpha(0f);
    card.setScaleX(0.8f);
    card.setScaleY(0.8f);
    
    card.animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(600)
        .setInterpolator(new OvershootInterpolator())
        .start();
}

private void animateCardClick(View card) {
    card.animate()
        .scaleX(0.95f)
        .scaleY(0.95f)
        .setDuration(100)
        .withEndAction(() -> {
            card.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150)
                .start();
        })
        .start();
}

private void focusCard(LinearLayout container, View focused) {
    for (int i = 0; i < container.getChildCount(); i++) {
        View child = container.getChildAt(i);
        float targetScale = (child == focused) ? 1.05f : 0.9f;

        child.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(200)
                .start();
    }
}

private void showThemesDialog(SharedPreferences prefs, boolean disclaimerShown) {
    new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("THEMES!!🎉")
            .setMessage("xelo client now supports custom themes! download themes from https://themes.xeloclient.in or make your own themes from https://docs.xeloclient.com")
            .setIcon(R.drawable.ic_info)
            .setPositiveButton("Proceed", (dialog, which) -> {
                dialog.dismiss();
                prefs.edit().putBoolean(KEY_THEMES_DIALOG_SHOWN, true).apply();
                
            })
            .setCancelable(false)
            .show();
}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d(TAG, "MainActivity onActivityResult: requestCode=" + requestCode + 
              ", resultCode=" + resultCode + ", data=" + (data != null ? "present" : "null"));
              
        if (requestCode == REQ_STORAGE_PERMS) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
createNoMediaFile();
        prefs.edit().putBoolean(KEY_STORAGE_PERMS_ASKED, true).apply();
        continueFirstLaunchFlow(prefs);
        return;
    }
        
        if (requestCode == DiscordLoginActivity.DISCORD_LOGIN_REQUEST_CODE && settingsFragment != null) {
            Log.d(TAG, "Forwarding Discord login result to SettingsFragment");
            settingsFragment.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    @Override
public void onRequestPermissionsResult(int requestCode,
                                       @NonNull String[] permissions,
                                       @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQ_STORAGE_PERMS) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_STORAGE_PERMS_ASKED, true).apply();
        continueFirstLaunchFlow(prefs);
    }
}

    @Override
    protected void onResume() {
        super.onResume();
        
        DiscordRPCHelper.getInstance().updatePresence("Using Xelo Client", "Using the best MCPE Client");
    }
    
    @Override
    protected void onApplyTheme() {
        super.onApplyTheme();
        
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            try {
                int currentBackground = Color.parseColor("#141414"); 
                if (bottomNavigationView.getBackground() != null) {
                    try {
                        currentBackground = ((android.graphics.drawable.ColorDrawable) bottomNavigationView.getBackground()).getColor();
                    } catch (Exception e) {
                    }
                }
                
                int targetBackground = ThemeManager.getInstance().getColor("surface");
                
                ThemeUtils.animateBackgroundColorTransition(bottomNavigationView, currentBackground, targetBackground, 300);
                
                ThemeUtils.applyThemeToBottomNavigation(bottomNavigationView);
            } catch (Exception e) {
                ThemeUtils.applyThemeToBottomNavigation(bottomNavigationView);
            }
        }
    }

    public void showGlobalProgress(int max) {
        if (globalProgress == null) {
            globalProgress = findViewById(R.id.global_download_progress);
        }
        if (globalProgress != null) {
            if (max > 0) {
                globalProgress.setIndeterminate(false);
                globalProgress.setMax(max);
                globalProgress.setProgress(0);
            } else {
                globalProgress.setIndeterminate(true);
            }
            globalProgress.setVisibility(View.VISIBLE);
            globalProgress.bringToFront();
        }
    }

    public void updateGlobalProgress(int value) {
        if (globalProgress != null) {
            globalProgress.setIndeterminate(false);
            globalProgress.setProgressCompat(value, true);
        }
    }

    public void hideGlobalProgress() {
        if (globalProgress != null) {
            globalProgress.setVisibility(View.GONE);
            globalProgress.setIndeterminate(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        DiscordRPCHelper.getInstance().updatePresence("Xelo Client", "Using the best MCPE Client");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DiscordRPCHelper.getInstance().cleanup();
    }
}