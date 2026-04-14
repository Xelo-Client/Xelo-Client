package com.origin.launcher.discord;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.CookieManager;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPublicKeyCredentialOption;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.exceptions.GetCredentialException;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONObject;

import com.origin.launcher.discord.DiscordManager;
import com.origin.launcher.activity.BaseThemedActivity;
import com.origin.launcher.R;

public class DiscordLoginActivity extends BaseThemedActivity {
    private static final String TAG = "DiscordLoginActivity";
    public static final int DISCORD_LOGIN_REQUEST_CODE = 1001;

    // biometric api
    private static final int BIOMETRIC_STRONG_OR_DEVICE = BiometricManager.Authenticators.BIOMETRIC_STRONG
            | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    private WebView webView;
    private ProgressBar progressBar;
    private ExtendedFloatingActionButton backButton;

    // biometric api
    private ExtendedFloatingActionButton biometricButton;

    private ExecutorService executor;
    private Handler mainHandler;
    private boolean isTokenExtractionInProgress = false;

    // biometric api
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo biometricPromptInfo;

    // credential manager api
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discord_login);

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        setupToolbar();
        initializeViews();
        setupWebView();

        // biometric api
        setupBiometricAuth();

        // credential manager api
        setupCredentialManager();

        loadDiscordLogin();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Login to Discord");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    private void initializeViews() {
        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);
        backButton = findViewById(R.id.back_button);

        // biometric api
        biometricButton = findViewById(R.id.biometric_button);

        if (webView == null) {
            Log.e(TAG, "WebView not found in layout");
            finishWithError("WebView initialization failed");
            return;
        }

        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }

        // biometric api
        if (biometricButton != null) {
            biometricButton.setOnClickListener(v -> showAuthOptions());
        }
    }

    // biometric api
    private void setupBiometricAuth() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG_OR_DEVICE);

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPromptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Login to Discord")
                    .setSubtitle("Use your fingerprint or screen lock to continue")
                    .setAllowedAuthenticators(BIOMETRIC_STRONG_OR_DEVICE)
                    .build();

            biometricPrompt = new BiometricPrompt(this,
                    ContextCompat.getMainExecutor(this),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            Log.e(TAG, "Biometric auth error: " + errString);
                            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                    && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                Toast.makeText(DiscordLoginActivity.this,
                                        "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            Log.d(TAG, "Biometric authentication succeeded");
                            onBiometricAuthSuccess();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            Log.w(TAG, "Biometric authentication failed");
                            Toast.makeText(DiscordLoginActivity.this,
                                    "Authentication failed. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });

            if (biometricButton != null) {
                biometricButton.setVisibility(View.VISIBLE);
            }
        } else {
            Log.w(TAG, "Biometric authentication not available: " + canAuthenticate);
            if (biometricButton != null) {
                biometricButton.setVisibility(View.GONE);
            }
        }
    }

    // credential manager api
    private void setupCredentialManager() {
        credentialManager = CredentialManager.create(this);
    }

    // biometric api
    private void showAuthOptions() {
        BiometricManager biometricManager = BiometricManager.from(this);
        boolean hasBiometric = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;

        if (hasBiometric) {
            showBiometricPrompt();
        } else {
            // credential manager api
            launchPasskeyAuth();
        }
    }

    // biometric api
    private void showBiometricPrompt() {
        if (biometricPrompt != null && biometricPromptInfo != null) {
            biometricPrompt.authenticate(biometricPromptInfo);
        }
    }

    // biometric api
    private void onBiometricAuthSuccess() {
        String savedToken = new DiscordManager(this).getStoredToken();
        if (savedToken != null && !savedToken.isEmpty()) {
            Log.d(TAG, "Biometric auth success, found saved token, validating...");
            mainHandler.post(() -> {
                progressBar.setVisibility(View.VISIBLE);
                if (backButton != null) {
                    backButton.setEnabled(false);
                }
                if (biometricButton != null) {
                    biometricButton.setEnabled(false);
                }
            });
            executor.execute(() -> validateTokenAndGetUserInfo(savedToken));
        } else {
            Log.d(TAG, "Biometric auth success but no saved token, proceeding with WebView login");
            Toast.makeText(this,
                    "No saved session found. Please log in with your credentials first.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // credential manager api
    private void launchPasskeyAuth() {
        try {
            String requestJson = buildPasskeyRequestJson();

            GetPublicKeyCredentialOption passkeyOption =
                    new GetPublicKeyCredentialOption(requestJson);

            GetCredentialRequest request = new GetCredentialRequest.Builder()
                    .addCredentialOption(passkeyOption)
                    .build();

            credentialManager.getCredentialAsync(
                    this,
                    request,
                    null,
                    ContextCompat.getMainExecutor(this),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            handlePasskeyResult(result);
                        }

                        @Override
                        public void onError(GetCredentialException e) {
                            Log.e(TAG, "Passkey authentication failed", e);
                            Toast.makeText(DiscordLoginActivity.this,
                                    "Passkey authentication failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error launching passkey auth", e);
            Toast.makeText(this, "Passkey not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    // credential manager api
    private String buildPasskeyRequestJson() {
        try {
            JSONObject allowCredentials = new JSONObject();

            JSONObject publicKey = new JSONObject();
            publicKey.put("rpId", "discord.com");
            publicKey.put("timeout", 60000);
            publicKey.put("userVerification", "required");
            publicKey.put("allowCredentials", new org.json.JSONArray());

            JSONObject requestJson = new JSONObject();
            requestJson.put("publicKey", publicKey);

            return requestJson.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error building passkey request JSON", e);
            return "{\"publicKey\":{\"rpId\":\"discord.com\",\"timeout\":60000,\"userVerification\":\"required\",\"allowCredentials\":[]}}";
        }
    }

    // credential manager api
    private void handlePasskeyResult(GetCredentialResponse result) {
        try {
            if (result.getCredential() instanceof PublicKeyCredential) {
                PublicKeyCredential publicKeyCredential = (PublicKeyCredential) result.getCredential();
                String responseJson = publicKeyCredential.getAuthenticationResponseJson();

                Log.d(TAG, "Passkey credential obtained, injecting into WebView...");

                mainHandler.post(() -> {
                    String js = "javascript:(function() {"
                            + "try {"
                            + "  var passkeyResponse = " + responseJson + ";"
                            + "  if (window.discordPasskeyHandler) {"
                            + "    window.discordPasskeyHandler(passkeyResponse);"
                            + "  } else {"
                            + "    console.log('XeloClient passkey response ready:', JSON.stringify(passkeyResponse));"
                            + "  }"
                            + "} catch(e) { console.error('Passkey inject error:', e); }"
                            + "})();";
                    webView.loadUrl(js);
                    Toast.makeText(DiscordLoginActivity.this,
                            "Passkey verified. Completing login...", Toast.LENGTH_SHORT).show();
                });
            } else {
                Log.w(TAG, "Unexpected credential type: " + result.getCredential().getClass().getName());
                Toast.makeText(this, "Unsupported credential type", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling passkey result", e);
            Toast.makeText(this, "Error processing passkey: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupWebView() {
        try {
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);
            settings.setUserAgentString(settings.getUserAgentString() + " XeloClient/1.0");

            // Clear existing data
            CookieManager.getInstance().removeAllCookies(null);
            webView.clearCache(true);
            webView.clearHistory();
            webView.clearFormData();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up WebView", e);
            finishWithError("WebView setup failed: " + e.getMessage());
            return;
        }

        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "shouldOverrideUrlLoading: " + url);

                // Check if user reached Discord's main app page (indicates successful login)
                if (url != null && (url.contains("/app") || url.contains("/channels/@me")) && !isTokenExtractionInProgress) {
                    Log.d(TAG, "User reached Discord app page, extracting token...");
                    isTokenExtractionInProgress = true;
                    webView.stopLoading();
                    extractTokenAndFinish();
                    return true;
                }

                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "onPageStarted: " + url);
                progressBar.setVisibility(View.VISIBLE);

                // Also check here for app page
                if (url != null && (url.contains("/app") || url.contains("/channels/@me")) && !isTokenExtractionInProgress) {
                    Log.d(TAG, "User reached Discord app page (onPageStarted), extracting token...");
                    isTokenExtractionInProgress = true;
                    extractTokenAndFinish();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "onPageFinished: " + url);
                progressBar.setVisibility(View.GONE);

                // Final check for app page
                if (url != null && (url.contains("/app") || url.contains("/channels/@me")) && !isTokenExtractionInProgress) {
                    Log.d(TAG, "User reached Discord app page (onPageFinished), extracting token...");
                    isTokenExtractionInProgress = true;
                    extractTokenAndFinish();
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "WebView error: " + description + " for URL: " + failingUrl + " (Code: " + errorCode + ")");
                progressBar.setVisibility(View.GONE);

                Toast.makeText(DiscordLoginActivity.this,
                        "Error loading page: " + description, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadDiscordLogin() {
        try {
            Log.d(TAG, "Loading Discord login page");
            webView.loadUrl("https://discord.com/login");
        } catch (Exception e) {
            Log.e(TAG, "Error loading Discord login URL", e);
            finishWithError("Error loading Discord login: " + e.getMessage());
        }
    }

    private void extractTokenAndFinish() {
        mainHandler.post(() -> {
            progressBar.setVisibility(View.VISIBLE);
            if (backButton != null) {
                backButton.setText("Extracting token...");
                backButton.setEnabled(false);
            }
        });

        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting token extraction process");

                // Wait longer for WebView to save data
                Thread.sleep(3000);

                String token = extractTokenFromStorage();

                if (token != null && !token.isEmpty()) {
                    Log.d(TAG, "Token extracted successfully, validating...");
                    validateTokenAndGetUserInfo(token);
                } else {
                    Log.e(TAG, "Failed to extract token from storage");
                    // Try again with a longer delay
                    Thread.sleep(2000);
                    token = extractTokenFromStorage();

                    if (token != null && !token.isEmpty()) {
                        Log.d(TAG, "Token extracted on retry, validating...");
                        validateTokenAndGetUserInfo(token);
                    } else {
                        mainHandler.post(() -> finishWithError("Failed to extract Discord token. Please try again."));
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error during token extraction", e);
                mainHandler.post(() -> finishWithError("Error extracting token: " + e.getMessage()));
            }
        });
    }

    private String extractTokenFromStorage() {
        try {
            // Path to WebView's local storage (similar to your friend's implementation)
            File webViewDir = new File(getFilesDir().getParentFile(), "app_webview/Default/Local Storage/leveldb");

            Log.d(TAG, "Looking for token in: " + webViewDir.getAbsolutePath());

            if (!webViewDir.exists()) {
                Log.w(TAG, "WebView storage directory not found: " + webViewDir.getAbsolutePath());
                return null;
            }

            File[] logFiles = webViewDir.listFiles((dir, name) -> name.endsWith(".log"));

            if (logFiles == null || logFiles.length == 0) {
                Log.w(TAG, "No log files found in WebView storage");
                return null;
            }

            Log.d(TAG, "Found " + logFiles.length + " log files, searching for token...");

            // Read all log files to find token
            for (File logFile : logFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("token") && line.contains("\"")) {
                            // Extract token using similar logic as your friend's code
                            int tokenIndex = line.indexOf("token");
                            if (tokenIndex == -1) continue;

                            String substring = line.substring(tokenIndex + 5);
                            int firstQuote = substring.indexOf("\"");
                            if (firstQuote == -1) continue;

                            String tokenPart = substring.substring(firstQuote + 1);
                            int secondQuote = tokenPart.indexOf("\"");

                            if (secondQuote > 0) {
                                String token = tokenPart.substring(0, secondQuote);
                                if (token.length() > 50) { // Discord tokens are typically longer
                                    Log.d(TAG, "Found potential token of length: " + token.length());
                                    return token;
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error reading WebView storage", e);
        }

        return null;
    }

    private void validateTokenAndGetUserInfo(String token) {
        // Test the token with Discord Gateway (similar to your friend's validation)
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url("wss://gateway.discord.gg/?v=10&encoding=json")
                    .build();

            WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
                private boolean identificationSent = false;
                private boolean finished = false;

                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    Log.d(TAG, "Gateway WebSocket opened for token validation");
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    if (finished) return;

                    try {
                        JSONObject json = new JSONObject(text);
                        int op = json.getInt("op");

                        switch (op) {
                            case 10: // Hello
                                if (!identificationSent) {
                                    sendIdentify(webSocket, token);
                                    identificationSent = true;
                                }
                                break;
                            case 0: // Dispatch
                                String eventType = json.getString("t");
                                if ("READY".equals(eventType)) {
                                    finished = true;
                                    JSONObject data = json.getJSONObject("d");
                                    JSONObject user = data.getJSONObject("user");

                                    String userId = user.getString("id");
                                    String username = user.getString("username");
                                    String discriminator = user.optString("discriminator", "0");
                                    String avatarHash = user.optString("avatar", "");

                                    String avatarUrl;
                                    if (avatarHash.isEmpty()) {
                                        int defaultAvatar = discriminator.equals("0") ? 0 : Integer.parseInt(discriminator) % 5;
                                        avatarUrl = "https://cdn.discordapp.com/embed/avatars/" + defaultAvatar + ".png";
                                    } else {
                                        avatarUrl = "https://cdn.discordapp.com/avatars/" + userId + "/" + avatarHash + ".png";
                                    }

                                    Log.d(TAG, "Token validation successful for user: " + username);

                                    mainHandler.post(() ->
                                            finishWithSuccess(token, userId, username, discriminator, avatarUrl)
                                    );

                                    webSocket.close(1000, "Validation complete");
                                }
                                break;
                        }
                    } catch (Exception e) {
                        if (!finished) {
                            Log.e(TAG, "Error processing gateway message", e);
                            finished = true;
                            mainHandler.post(() -> finishWithError("Token validation failed"));
                            webSocket.close(1000, "Error");
                        }
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    if (!finished) {
                        Log.e(TAG, "Gateway WebSocket failed during validation", t);
                        finished = true;
                        mainHandler.post(() -> finishWithError("Failed to validate token"));
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error validating token", e);
            mainHandler.post(() -> finishWithError("Error validating token: " + e.getMessage()));
        }
    }

    private void sendIdentify(WebSocket webSocket, String token) {
        try {
            JSONObject properties = new JSONObject();
            properties.put("os", "Android");
            properties.put("browser", "XeloClient");
            properties.put("device", "XeloClient");

            JSONObject data = new JSONObject();
            data.put("token", token);
            data.put("intents", 0);
            data.put("properties", properties);

            JSONObject payload = new JSONObject();
            payload.put("op", 2);
            payload.put("d", data);

            webSocket.send(payload.toString());
            Log.d(TAG, "Identify payload sent for token validation");

        } catch (Exception e) {
            Log.e(TAG, "Error sending identify payload", e);
        }
    }

    private void finishWithSuccess(String token, String userId, String username, String discriminator, String avatarUrl) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("access_token", token);
        resultIntent.putExtra("user_id", userId);
        resultIntent.putExtra("username", username);
        resultIntent.putExtra("discriminator", discriminator);
        resultIntent.putExtra("avatar", avatarUrl);
        resultIntent.putExtra("success", true);

        Log.d(TAG, "Setting result OK with user data for: " + username);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void finishWithError(String error) {
        Intent errorIntent = new Intent();
        errorIntent.putExtra("error", error);
        errorIntent.putExtra("success", false);
        Log.d(TAG, "Setting result CANCELED with error: " + error);
        setResult(RESULT_CANCELED, errorIntent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        finishWithError("Login cancelled by user");
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        // Clear WebView to prevent memory leaks
        if (webView != null) {
            webView.destroy();
        }
    }
}