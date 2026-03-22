package com.origin.launcher.fragments;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import android.app.ActivityManager;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.text.ParseException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class AboutFragment extends BaseThemedFragment {

    private TextView versionText;
    private TextView deviceModelText;
    private TextView androidVersionText;
    private TextView buildNumberText;
    private TextView deviceManufacturerText;
    private TextView deviceArchitectureText;
    private TextView screenResolutionText;
    private TextView totalMemoryText;
    private LinearLayout commitsContainer;
    private LinearLayout githubButton;
    private LinearLayout discordButton;
    
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Xelo-Client/Xelo-Client/commits";
    private static final String GITHUB_URL = "https://github.com/Xelo-Client/Xelo-Client";
    private static final String DISCORD_URL = "https://discord.gg/CHUchrEWwc";
    private static final String TAG = "AboutFragment";
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        
        // Initialize version TextView
        versionText = view.findViewById(R.id.version_text);
        
        // Initialize device information TextViews
        deviceModelText = view.findViewById(R.id.device_model);
        androidVersionText = view.findViewById(R.id.android_version);
        buildNumberText = view.findViewById(R.id.build_number);
        deviceManufacturerText = view.findViewById(R.id.device_manufacturer);
        deviceArchitectureText = view.findViewById(R.id.device_architecture);
        screenResolutionText = view.findViewById(R.id.screen_resolution);
        totalMemoryText = view.findViewById(R.id.total_memory);
        
        // Initialize commits container
        commitsContainer = view.findViewById(R.id.commits_container);
        
        // Initialize social media buttons
        githubButton = view.findViewById(R.id.github_button);
        discordButton = view.findViewById(R.id.discord_button);
        
        // Initialize executor and handler
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Set up button click listeners
        setupButtonListeners();
        
        // Load app version information
        loadVersionInformation();
        
        // Load device information
        loadDeviceInformation();
        
        // Load commits
        loadCommits();
        
        return view;
    }
    
    private void setupButtonListeners() {
        githubButton.setOnClickListener(v -> openUrl(GITHUB_URL, "GitHub"));
        discordButton.setOnClickListener(v -> openUrl(DISCORD_URL, "Discord"));
    }
    
    private void openUrl(String url, String appName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening " + appName + " URL", e);
            if (isAdded()) {
                Toast.makeText(getContext(), "Unable to open " + appName, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void loadVersionInformation() {
        try {
            PackageInfo packageInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;
            
            String versionInfo = "Xelo Client v" + versionName + " (Build " + versionCode + ")";
            versionText.setText(versionInfo);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version", e);
            versionText.setText("Xelo Client - Version unavailable");
        }
    }
    
    private void loadDeviceInformation() {
        // Device Model
        String model = Build.MODEL;
        deviceModelText.setText("Model: " + model);
        
        // Android Version
        String androidVersion = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
        androidVersionText.setText("Android Version: " + androidVersion);
        
        // Build Number
        String buildNumber = Build.DISPLAY;
        buildNumberText.setText("Build Number: " + buildNumber);
        
        // Device Manufacturer
        String manufacturer = Build.MANUFACTURER;
        deviceManufacturerText.setText("Manufacturer: " + manufacturer);
        
        // Device Architecture
        String architecture = Build.SUPPORTED_ABIS[0];
        deviceArchitectureText.setText("Architecture: " + architecture);
        
        // Screen Resolution
        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        String resolution = displayMetrics.widthPixels + " x " + displayMetrics.heightPixels;
        screenResolutionText.setText("Screen Resolution: " + resolution);
        
        // Total Memory
        ActivityManager activityManager = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        long totalMemoryMB = memoryInfo.totalMem / (1024 * 1024);
        totalMemoryText.setText("Total Memory: " + totalMemoryMB + " MB");
    }
    
    private void loadCommits() {
        executor.execute(() -> {
            String result = fetchCommitsFromApi();
            mainHandler.post(() -> {
                if (result != null && isAdded()) {
                    parseAndDisplayCommits(result);
                } else if (isAdded()) {
                    displayErrorMessage();
                }
            });
        });
    }
    
    private String fetchCommitsFromApi() {
        try {
            URL url = new URL(GITHUB_API_URL + "?per_page=5");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else {
                Log.e(TAG, "HTTP Error: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching commits", e);
            return null;
        }
    }
    
    private void parseAndDisplayCommits(String jsonResponse) {
        try {
            JSONArray commits = new JSONArray(jsonResponse);
            commitsContainer.removeAllViews();
            
            for (int i = 0; i < Math.min(commits.length(), 5); i++) {
                JSONObject commit = commits.getJSONObject(i);
                JSONObject commitData = commit.getJSONObject("commit");
                JSONObject author = commitData.getJSONObject("author");
                
                String message = commitData.getString("message");
                String authorName = author.getString("name");
                String sha = commit.getString("sha").substring(0, 7);
                String dateStr = author.getString("date");
                
                // Parse and format date
                String formattedDate = formatDate(dateStr);
                
                // Create commit view
                createCommitView(message, authorName, sha, formattedDate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing commits", e);
            displayErrorMessage();
        }
    }
    
    private void createCommitView(String message, String author, String sha, String date) {
        // Create main container
        LinearLayout commitLayout = new LinearLayout(getContext());
        commitLayout.setOrientation(LinearLayout.VERTICAL);
        commitLayout.setPadding(0, 16, 0, 16);
        
        // Create header with author and commit info
        LinearLayout headerLayout = new LinearLayout(getContext());
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        // Author name
        TextView authorText = new TextView(getContext());
        authorText.setText(author);
        authorText.setTextSize(14);
        authorText.setTextColor(0xFFFFFFFF); // White text
        
        // Bullet separator
        TextView bulletText = new TextView(getContext());
        bulletText.setText(" • ");
        bulletText.setTextSize(14);
        bulletText.setTextColor(0xFF888888); // Gray text
        
        // SHA
        TextView shaText = new TextView(getContext());
        shaText.setText(sha);
        shaText.setTextSize(14);
        shaText.setTextColor(0xFF888888); // Gray text
        
        // Date
        TextView dateText = new TextView(getContext());
        dateText.setText(date);
        dateText.setTextSize(12);
        dateText.setTextColor(0xFF888888); // Gray text
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dateParams.weight = 1;
        dateParams.gravity = android.view.Gravity.END;
        dateText.setLayoutParams(dateParams);
        dateText.setGravity(android.view.Gravity.END);
        
        headerLayout.addView(authorText);
        headerLayout.addView(bulletText);
        headerLayout.addView(shaText);
        headerLayout.addView(dateText);
        
        // Commit message
        TextView messageText = new TextView(getContext());
        messageText.setText(message);
        messageText.setTextSize(16);
        messageText.setTextColor(0xFFFFFFFF); // White text
        messageText.setPadding(0, 8, 0, 0);
        
        commitLayout.addView(headerLayout);
        commitLayout.addView(messageText);
        
        commitsContainer.addView(commitLayout);
        
        // Add separator line (except for last item)
        View separator = new View(getContext());
        separator.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1));
        separator.setBackgroundColor(0xFF333333); // Dark gray separator
        LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
        separatorParams.setMargins(0, 16, 0, 0);
        separator.setLayoutParams(separatorParams);
        commitsContainer.addView(separator);
    }
    
    private String formatDate(String isoDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yy", Locale.US);
            Date date = inputFormat.parse(isoDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + isoDate, e);
            return "Unknown";
        }
    }
    
    private void displayErrorMessage() {
        commitsContainer.removeAllViews();
        TextView errorText = new TextView(getContext());
        errorText.setText("Unable to load commits. Check your internet connection.");
        errorText.setTextColor(0xFF888888);
        errorText.setTextSize(14);
        errorText.setPadding(0, 16, 0, 16);
        commitsContainer.addView(errorText);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Update Discord RPC when fragment resumes
        DiscordRPCHelper.getInstance().updateMenuPresence("About");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Update Discord RPC when leaving about
        DiscordRPCHelper.getInstance().updateIdlePresence();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
