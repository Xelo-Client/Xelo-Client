package com.origin.launcher.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionsRepository {
    private static final String TAG = "VersionsRepository";
    private static final String REMOTE_URL = "https://raw.githubusercontent.com/Xelo-Client/cdn/refs/heads/main/results.txt";
    private static final String CACHE_FILE_NAME = "mcpe_versions.txt";

    public static class VersionEntry {
        public final String title;
        public final String url;
        public final boolean isBeta; // true: 4 dots, false: 3 dots

        public VersionEntry(String title, String url, boolean isBeta) {
            this.title = title;
            this.url = url;
            this.isBeta = isBeta;
        }
    }

    public List<VersionEntry> getVersions(Context context) {
        Log.d(TAG, "Fetching versions from: " + REMOTE_URL);
        // Try refresh cache; if fails, fall back to cached file
        File cacheFile = new File(context.getCacheDir(), CACHE_FILE_NAME);
        try {
            List<String> lines = downloadLines();
            Log.d(TAG, "Downloaded " + lines.size() + " lines");
            if (!lines.isEmpty()) {
                writeCache(cacheFile, lines);
                List<VersionEntry> entries = parse(lines);
                Log.d(TAG, "Parsed " + entries.size() + " version entries");
                return entries;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to fetch remote versions, using cache if available", e);
        }

        // Fallback to cache
        try {
            if (cacheFile.exists()) {
                Log.d(TAG, "Using cached versions");
                List<String> cachedLines = readCache(cacheFile);
                List<VersionEntry> entries = parse(cachedLines);
                Log.d(TAG, "Parsed " + entries.size() + " cached version entries");
                return entries;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read cached versions", e);
        }

        Log.w(TAG, "No versions found, returning empty list");
        return new ArrayList<>();
    }

    public void clearCache(Context context) {
        File cacheFile = new File(context.getCacheDir(), CACHE_FILE_NAME);
        if (cacheFile.exists()) {
            cacheFile.delete();
            Log.d(TAG, "Cleared version cache");
        }
    }

    private List<String> downloadLines() throws Exception {
        HttpURLConnection connection = null;
        List<String> result = new ArrayList<>();
        try {
            URL url = new URL(REMOTE_URL);
            Log.d(TAG, "Connecting to: " + url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36");
            connection.connect();
            int code = connection.getResponseCode();
            Log.d(TAG, "HTTP response code: " + code);
            if (code != 200) {
                // Try to read error response
                try (InputStream errorStream = connection.getErrorStream();
                     BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine).append("\n");
                    }
                    Log.e(TAG, "Error response: " + errorResponse.toString());
                } catch (Exception e) {
                    Log.e(TAG, "Could not read error response", e);
                }
                throw new Exception("HTTP " + code);
            }
            try (InputStream in = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        result.add(line);
                        Log.d(TAG, "Downloaded line: " + line);
                    }
                }
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
        return result;
    }


    private void writeCache(File file, List<String> lines) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String l : lines) {
                writer.write(l);
                writer.newLine();
            }
        }
    }

    private List<String> readCache(File file) throws Exception {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) lines.add(line);
            }
        }
        return lines;
    }

    private List<VersionEntry> parse(List<String> lines) {
        List<VersionEntry> list = new ArrayList<>();
        Log.d(TAG, "Parsing " + lines.size() + " lines");
        for (String raw : lines) {
            Log.d(TAG, "Parsing line: " + raw);
            Parsed p = parseLine(raw);
            if (p != null) {
                Log.d(TAG, "Parsed: " + p.title + " -> " + p.url + " (beta: " + p.isBeta + ")");
                list.add(new VersionEntry(p.title, p.url, p.isBeta));
            } else {
                Log.w(TAG, "Failed to parse line: " + raw);
            }
        }
        Log.d(TAG, "Parsed " + list.size() + " version entries");
        return list;
    }

    private static class Parsed {
        String title;
        String url;
        boolean isBeta;
    }

    private Parsed parseLine(String raw) {
        // Try common separators: |, tab, comma, or last-whitespace URL
        String title = null;
        String url = null;

        if (raw.contains("|") ) {
            String[] parts = raw.split("\\|", 2);
            if (parts.length == 2) {
                title = parts[0].trim();
                url = parts[1].trim();
            }
        }
        if (title == null || url == null) {
            String[] parts = raw.split("\t", 2);
            if (parts.length == 2 && parts[1].startsWith("http")) {
                title = parts[0].trim();
                url = parts[1].trim();
            }
        }
        if (title == null || url == null) {
            int idx = raw.lastIndexOf(" http");
            if (idx == -1) idx = raw.lastIndexOf("\thttp");
            if (idx == -1) {
                // fallback: find first http
                int h = raw.indexOf("http");
                if (h > 0) {
                    title = raw.substring(0, h).trim();
                    url = raw.substring(h).trim();
                }
            } else {
                title = raw.substring(0, idx).trim();
                url = raw.substring(idx + 1).trim();
            }
        }

        if (title == null || url == null || !url.startsWith("http")) {
            Log.w(TAG, "Skipping unparseable line: " + raw);
            return null;
        }

        // Determine isBeta by counting dots in version pattern inside title
        String version = extractVersion(title);
        int dotCount = version != null ? count(version, '.') : count(title, '.');
        // Classification rule:
        // - 2 dots (e.g., 1.21.100) => stable
        // - 3 dots (e.g., 1.21.100.10) => beta
        // Fallback: >=3 => beta, otherwise stable
        boolean isBeta = (dotCount == 3) || (dotCount > 3);

        // Clean up title: remove any ':' characters
        String cleanTitle = title.replace(":", "").trim();

        Parsed p = new Parsed();
        p.title = cleanTitle;
        p.url = url;
        p.isBeta = isBeta;
        return p;
    }

    private String extractVersion(String title) {
        try {
            Pattern pattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+(?:\\.\\d+)*)");
            Matcher m = pattern.matcher(title);
            if (m.find()) return m.group(1);
        } catch (Exception ignored) {}
        return null;
    }

    private int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }
}

