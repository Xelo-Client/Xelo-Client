package com.origin.launcher.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import android.util.Log;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.io.File;

import com.origin.launcher.utils.ThemeUtils;
import com.origin.launcher.discord.DiscordRPCHelper;
import com.origin.launcher.utils.VersionsRepository;

public class VersionsStableFragment extends BaseThemedFragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_versions_stable, container, false);
        
        try {
            LinearLayout versionsContainer = view.findViewById(R.id.versionsContainerStable);
            LinearProgressIndicator progressBar = view.findViewById(R.id.download_progress_stable);
            if (versionsContainer != null) {
                populateFromRepo(versionsContainer);
            }
        } catch (Exception e) {
            Log.e("VersionsStable", "Failed to initialize version cards", e);
        }
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        DiscordRPCHelper.getInstance().updateMenuPresence("version switcher - stable");
    }

    private void populateFromRepo(LinearLayout container) {
        new Thread(() -> {
            try {
                Log.d("VersionsStable", "Starting to fetch versions...");
                VersionsRepository repo = new VersionsRepository();
                java.util.List<VersionsRepository.VersionEntry> entries = repo.getVersions(requireContext());
                Log.d("VersionsStable", "Got " + entries.size() + " total entries");
                java.util.List<VersionsRepository.VersionEntry> stable = new java.util.ArrayList<>();
                for (VersionsRepository.VersionEntry ve : entries) {
                    if (!ve.isBeta) {
                        stable.add(ve);
                        Log.d("VersionsStable", "Added stable version: " + ve.title);
                    }
                }
                Log.d("VersionsStable", "Found " + stable.size() + " stable versions");
                requireActivity().runOnUiThread(() -> {
                    container.removeAllViews();
                    for (int i = 0; i < stable.size(); i++) {
                        VersionsRepository.VersionEntry e = stable.get(i);
                        addVersionCard(container, e.title, "", e.url);
                    }
                    Log.d("VersionsStable", "Added " + stable.size() + " version cards to UI");
                });
            } catch (Exception ex) {
                Log.e("VersionsStable", "Failed to load versions", ex);
                requireActivity().runOnUiThread(() -> {
                    android.widget.Toast.makeText(requireContext(), "Failed to load versions: " + ex.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        DiscordRPCHelper.getInstance().updateIdlePresence();
    }

    private void addVersionCard(LinearLayout container, String title, String subtitle, String url) {
        // Create card
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        card.setLayoutParams(cardParams);
        card.setRadius(12 * getResources().getDisplayMetrics().density);
        card.setCardElevation(0);
        card.setClickable(true);
        card.setFocusable(true);
        ThemeUtils.applyThemeToCard(card, requireContext());

        // Main horizontal layout
        LinearLayout main = new LinearLayout(requireContext());
        main.setOrientation(LinearLayout.HORIZONTAL);
        main.setPadding(
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density),
            (int) (16 * getResources().getDisplayMetrics().density)
        );
        main.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Text column
        LinearLayout textCol = new LinearLayout(requireContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(textParams);

        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        ThemeUtils.applyThemeToTextView(titleView, "onSurface");

        TextView subView = new TextView(requireContext());
        subView.setText(subtitle);
        subView.setTextSize(14);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
        subView.setLayoutParams(subParams);
        ThemeUtils.applyThemeToTextView(subView, "onSurfaceVariant");

        textCol.addView(titleView);
        textCol.addView(subView);

        // Right action column
        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        actionsParams.setMarginStart((int) (16 * getResources().getDisplayMetrics().density));
        actions.setLayoutParams(actionsParams);

        android.view.ContextThemeWrapper buttonCtx = new android.view.ContextThemeWrapper(
            requireContext(), com.google.android.material.R.style.Widget_Material3_Button
        );
        MaterialButton downloadBtn = new MaterialButton(buttonCtx, null, 0);
        downloadBtn.setText("Download");
        // Match Home fragment button sizing/shape
        downloadBtn.setTextSize(14);
        downloadBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        downloadBtn.setPadding(pad, pad, pad, pad);
        downloadBtn.setCornerRadius((int) (28 * getResources().getDisplayMetrics().density));
        // Apply theme like Home fragment does
        ThemeUtils.applyThemeToButton(downloadBtn, requireContext());
        // Check if APK already exists and show appropriate button
        String fileName = buildApkFileNameFromTitle(title);
        File versionsDir = new File(requireContext().getExternalFilesDir(null), "versions");
        File apkFile = new File(versionsDir, fileName);
        
        if (apkFile.exists()) {
            downloadBtn.setText("Select");
            downloadBtn.setOnClickListener(v -> selectApk(apkFile, title));
        } else {
            downloadBtn.setText("Download");
            downloadBtn.setOnClickListener(v -> startDownload(url, title));
        }

        actions.addView(downloadBtn);

        main.addView(textCol);
        main.addView(actions);
        card.addView(main);

        // Add card and spacing
        container.addView(card);
        View spacer = new View(requireContext());
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int) (12 * getResources().getDisplayMetrics().density)
        );
        spacer.setLayoutParams(spacerParams);
        container.addView(spacer);
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void startDownload(String url, String title) {
        View root = getView();
        if (root == null) return;
        LinearProgressIndicator progress = root.findViewById(R.id.download_progress_stable);
        new Thread(() -> {
            boolean ok = false;
            try {
                String fileName = buildApkFileNameFromTitle(title);
                File versionsDir = new File(requireContext().getExternalFilesDir(null), "versions");
                if (!versionsDir.exists()) versionsDir.mkdirs();
                File outFile = new File(versionsDir, fileName);
                long total = fetchContentLength(url);
                int max = (total > 0 && total <= Integer.MAX_VALUE) ? (int) total : -1;
                if (getActivity() instanceof MainActivity) {
                    final int finalMax = max;
                    requireActivity().runOnUiThread(() -> ((MainActivity) getActivity()).showGlobalProgress(finalMax));
                }
                downloadToFileWithProgressResumable(url, outFile, total);
                ok = true;
            } catch (Exception ex) {
                Log.e("VersionsStable", "Download failed", ex);
            }
            boolean finalOk = ok;
            requireActivity().runOnUiThread(() -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).hideGlobalProgress();
                }
                Toast.makeText(requireContext(), finalOk ? "Downloaded" : "Download failed", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private long fetchContentLength(String urlStr) {
        try {
            java.net.HttpURLConnection conn = openConnectionFollowingRedirects(urlStr, 5);
            long len = conn.getContentLengthLong();
            conn.disconnect();
            return len;
        } catch (Exception ignored) {}
        return -1;
    }

    private String buildApkFileNameFromTitle(String title) {
        // Extract version numbers and remove dots to form filename like 121100.apk
        String version = title.replaceAll("[^0-9\\.]", "");
        version = version.replace(".", "");
        if (version.isEmpty()) version = String.valueOf(System.currentTimeMillis());
        return version + ".apk";
    }

    private void downloadToFile(String urlStr, File outFile) throws Exception {
        java.net.HttpURLConnection conn = null;
        try {
            conn = openConnectionFollowingRedirects(urlStr, 5);
            int code = conn.getResponseCode();
            if (code != java.net.HttpURLConnection.HTTP_OK && code != java.net.HttpURLConnection.HTTP_PARTIAL) {
                throw new Exception("HTTP " + code);
            }
            // Ensure parent dir exists
            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (java.io.InputStream in = conn.getInputStream();
                 java.io.FileOutputStream out = new java.io.FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) {
                    out.write(buf, 0, r);
                }
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void downloadToFileWithProgressResumable(String urlStr, File outFile, long total) throws Exception {
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        File tmp = new File(outFile.getAbsolutePath() + ".part");
        int maxRetries = 3;
        int attempt = 0;
        long downloaded = tmp.exists() ? tmp.length() : 0;
        long contentLength = total;

        while (attempt < maxRetries) {
            attempt++;
            java.net.HttpURLConnection conn = null;
            try {
                // Open connection (handling redirects)
                conn = openConnectionFollowingRedirects(urlStr, 5);
                // Re-open final hop to add Range/headers
                java.net.URL finalUrl = new java.net.URL(conn.getURL().toString());
                conn.disconnect();
                conn = (java.net.HttpURLConnection) finalUrl.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(45000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Mobile Safari/537.36");
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.setRequestProperty("Connection", "close");

                if (downloaded > 0) {
                    conn.setRequestProperty("Range", "bytes=" + downloaded + "-");
                }
                conn.connect();

                int code = conn.getResponseCode();
                if (code != java.net.HttpURLConnection.HTTP_OK && code != java.net.HttpURLConnection.HTTP_PARTIAL) {
                    throw new Exception("HTTP " + code);
                }

                // If server ignored range and sent full file
                if (code == java.net.HttpURLConnection.HTTP_OK && downloaded > 0) {
                    // Start over
                    downloaded = 0;
                    if (tmp.exists()) tmp.delete();
                }

                if (contentLength <= 0) {
                    long lenHeader = conn.getHeaderFieldLong("Content-Length", -1);
                    if (lenHeader > 0 && downloaded > 0) {
                        contentLength = downloaded + lenHeader;
                    } else if (lenHeader > 0) {
                        contentLength = lenHeader;
                    }
                    if (getActivity() instanceof MainActivity) {
                        final int max = (contentLength > 0 && contentLength <= Integer.MAX_VALUE) ? (int) contentLength : -1;
                        requireActivity().runOnUiThread(() -> ((MainActivity) getActivity()).showGlobalProgress(max));
                    }
                }

                try (java.io.BufferedInputStream in = new java.io.BufferedInputStream(conn.getInputStream(), 131072);
                     java.io.BufferedOutputStream out = new java.io.BufferedOutputStream(new java.io.FileOutputStream(tmp, true), 131072)) {
                    byte[] buf = new byte[131072];
                    int r;
                    long lastReportBytes = downloaded;
                    long lastTs = System.currentTimeMillis();
                    while ((r = in.read(buf)) != -1) {
                        out.write(buf, 0, r);
                        downloaded += r;
                        if (contentLength > 0 && getActivity() instanceof MainActivity) {
                            long now = System.currentTimeMillis();
                            if (downloaded - lastReportBytes >= 256 * 1024 || (now - lastTs) > 200) {
                                lastReportBytes = downloaded;
                                lastTs = now;
                                int value = (int) Math.min(Integer.MAX_VALUE, downloaded);
                                int finalValue = value;
                                requireActivity().runOnUiThread(() -> ((MainActivity) getActivity()).updateGlobalProgress(finalValue));
                            }
                        }
                    }
                    out.flush();
                }

                // Completed
                if (tmp.renameTo(outFile)) {
                    return;
                } else {
                    // Fallback copy
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(tmp);
                         java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
                        byte[] cbuf = new byte[131072];
                        int cr;
                        while ((cr = fis.read(cbuf)) != -1) {
                            fos.write(cbuf, 0, cr);
                        }
                    }
                    tmp.delete();
                    return;
                }
            } catch (Exception ex) {
                if (attempt >= maxRetries) {
                    throw ex;
                }
                // Backoff before retry
                try { Thread.sleep(1000L * attempt); } catch (InterruptedException ignored) {}
                // Continue loop to retry with Range
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        throw new Exception("Failed to download after retries");
    }

    private java.net.HttpURLConnection openConnectionFollowingRedirects(String urlStr, int maxRedirects) throws Exception {
        String current = urlStr;
        for (int i = 0; i < maxRedirects; i++) {
            java.net.URL url = new java.net.URL(current);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false); // we handle manually to copy headers
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(45000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Mobile Safari/537.36");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.connect();
            int code = conn.getResponseCode();
            if (code == java.net.HttpURLConnection.HTTP_MOVED_PERM || code == java.net.HttpURLConnection.HTTP_MOVED_TEMP || code == java.net.HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc == null) throw new Exception("Redirect without Location");
                if (!loc.startsWith("http")) {
                    java.net.URL base = url;
                    java.net.URL newUrl = new java.net.URL(base, loc);
                    current = newUrl.toString();
                } else {
                    current = loc;
                }
                continue;
            }
            return conn;
        }
        throw new Exception("Too many redirects");
    }

    private void selectApk(File apkFile, String title) {
        try {
            // Store the selected APK path in SharedPreferences for the launcher to use
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("selected_apk", android.content.Context.MODE_PRIVATE);
            prefs.edit().putString("apk_path", apkFile.getAbsolutePath()).apply();
            
            Toast.makeText(requireContext(), "Selected: " + title, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to select APK", Toast.LENGTH_SHORT).show();
        }
    }

}