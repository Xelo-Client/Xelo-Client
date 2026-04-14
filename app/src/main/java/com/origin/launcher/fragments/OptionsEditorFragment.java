package com.origin.launcher.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.origin.launcher.R;
import com.origin.launcher.utils.ThemeUtils;

public class OptionsEditorFragment extends BaseThemedFragment {

    // Source location constants
    private static final int SOURCE_EXTERNAL = 0;
    private static final int SOURCE_INTERNAL = 1;
    private static final int SOURCE_VERSION_ISOLATION = 2;

    private static final String VERSION_ISOLATION_PATH =
        "/storage/emulated/0/games/xelo_client/minecraft/minecraftpe/options.txt";

    private int currentSource = SOURCE_EXTERNAL;

    private File optionsFile;
    private String originalContent = "";
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();

    private EditText textEditor;
    private TextInputLayout searchInputLayout;
    private TextInputEditText searchEditText;
    private TextView sourceLabel;

    private String currentSearchTerm = "";
    private List<Integer> searchMatches = new ArrayList<>();
    private int currentMatchIndex = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_options_editor, container, false);

        textEditor = view.findViewById(R.id.optionsEditorText);
        searchInputLayout = view.findViewById(R.id.optionsSearchInputLayout);
        searchEditText = view.findViewById(R.id.optionsSearchEditText);
        sourceLabel = view.findViewById(R.id.optionsSourceLabel);

        MaterialButton btnEdit = view.findViewById(R.id.optionsBtnEdit);
        MaterialButton btnSave = view.findViewById(R.id.optionsBtnSave);
        MaterialButton btnUndo = view.findViewById(R.id.optionsBtnUndo);
        MaterialButton btnRedo = view.findViewById(R.id.optionsBtnRedo);
        MaterialButton btnSearch = view.findViewById(R.id.optionsBtnSearch);
        MaterialButton btnShare = view.findViewById(R.id.optionsBtnShare);
        MaterialButton btnSource = view.findViewById(R.id.optionsBtnSource);
        MaterialButton btnBack = view.findViewById(R.id.optionsBtnBack);

        // Apply theme to all buttons
        ThemeUtils.applyThemeToButton(btnEdit, requireContext());
        ThemeUtils.applyThemeToButton(btnSave, requireContext());
        ThemeUtils.applyThemeToButton(btnUndo, requireContext());
        ThemeUtils.applyThemeToButton(btnRedo, requireContext());
        ThemeUtils.applyThemeToButton(btnSearch, requireContext());
        ThemeUtils.applyThemeToButton(btnShare, requireContext());
        ThemeUtils.applyThemeToButton(btnSource, requireContext());
        ThemeUtils.applyThemeToButton(btnBack, requireContext());

        resolveOptionsFile();
        updateSourceLabel();
        loadFileIntoEditor();

        btnEdit.setOnClickListener(v -> {
            textEditor.setEnabled(true);
            textEditor.requestFocus();
            Toast.makeText(requireContext(), "Editing enabled", Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> saveFile());

        btnUndo.setOnClickListener(v -> undoChanges());

        btnRedo.setOnClickListener(v -> redoChanges());

        btnSearch.setOnClickListener(v -> {
            if (searchInputLayout.getVisibility() == View.GONE) {
                searchInputLayout.setVisibility(View.VISIBLE);
                searchEditText.requestFocus();
            } else {
                String term = searchEditText.getText() != null ? searchEditText.getText().toString().trim() : "";
                if (!term.isEmpty()) {
                    findNextMatch(term);
                }
            }
        });

        btnShare.setOnClickListener(v -> shareFile());

        btnSource.setOnClickListener(v -> showSourceChooser());

        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        setupSearchListener();
        setupTextChangeListener();

        return view;
    }

    private void resolveOptionsFile() {
        switch (currentSource) {
            case SOURCE_EXTERNAL:
                File extBase = requireContext().getExternalFilesDir(null);
                optionsFile = new File(extBase, "games/com.mojang/minecraftpe/options.txt");
                break;
            case SOURCE_INTERNAL:
                File intBase = requireContext().getFilesDir();
                optionsFile = new File(intBase, "games/com.mojang/minecraftpe/options.txt");
                break;
            case SOURCE_VERSION_ISOLATION:
                optionsFile = new File(VERSION_ISOLATION_PATH);
                break;
        }
    }

    private void updateSourceLabel() {
        if (sourceLabel == null) return;
        switch (currentSource) {
            case SOURCE_EXTERNAL:
                sourceLabel.setText("Source: External (" + (optionsFile != null ? optionsFile.getAbsolutePath() : "N/A") + ")");
                break;
            case SOURCE_INTERNAL:
                sourceLabel.setText("Source: Internal (" + (optionsFile != null ? optionsFile.getAbsolutePath() : "N/A") + ")");
                break;
            case SOURCE_VERSION_ISOLATION:
                sourceLabel.setText("Source: Version Isolation (" + VERSION_ISOLATION_PATH + ")");
                break;
        }
    }

    private void loadFileIntoEditor() {
        if (optionsFile == null || !optionsFile.exists()) {
            textEditor.setText("");
            textEditor.setEnabled(false);
            Toast.makeText(requireContext(), "options.txt not found at selected source", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(optionsFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            originalContent = content.toString();
            textEditor.setText(originalContent);
            textEditor.setEnabled(false);

            undoStack.clear();
            redoStack.clear();
            undoStack.push(originalContent);

            Toast.makeText(requireContext(), "Loaded options.txt", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to load: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void saveFile() {
        if (optionsFile == null) {
            Toast.makeText(requireContext(), "No file selected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String content = textEditor.getText().toString();
            File parent = optionsFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileWriter writer = new FileWriter(optionsFile)) {
                writer.write(content);
            }

            originalContent = content;
            Toast.makeText(requireContext(), "Saved successfully", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(requireContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void undoChanges() {
        if (undoStack.size() > 1) {
            int cursor = textEditor.getSelectionStart();
            redoStack.push(textEditor.getText().toString());
            undoStack.pop();
            String prev = undoStack.peek();
            textEditor.setText(prev);
            textEditor.setSelection(Math.min(cursor, prev.length()));
        } else {
            Toast.makeText(requireContext(), "Nothing to undo", Toast.LENGTH_SHORT).show();
        }
    }

    private void redoChanges() {
        if (!redoStack.isEmpty()) {
            int cursor = textEditor.getSelectionStart();
            String redoText = redoStack.pop();
            undoStack.push(redoText);
            textEditor.setText(redoText);
            textEditor.setSelection(Math.min(cursor, redoText.length()));
        } else {
            Toast.makeText(requireContext(), "Nothing to redo", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile() {
        if (optionsFile == null || !optionsFile.exists()) {
            Toast.makeText(requireContext(), "No file to share", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri fileUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                optionsFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share options.txt"));

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Share failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void showSourceChooser() {
        String[] options = {
            "External (App external folder)",
            "Internal (App internal folder)",
            "Version Isolation (/games/xelo_client/minecraft/)"
        };

        new AlertDialog.Builder(requireContext())
            .setTitle("Select options.txt Source")
            .setSingleChoiceItems(options, currentSource, (dialog, which) -> {
                currentSource = which;
                dialog.dismiss();
                resolveOptionsFile();
                updateSourceLabel();
                loadFileIntoEditor();
                clearSearchResults();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void setupSearchListener() {
        if (searchEditText == null) return;

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String term = s.toString().trim();
                if (!term.isEmpty()) {
                    searchInText(term);
                } else {
                    // Clear search results when search term is empty
                    clearSearchResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Handle Enter key in search
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String term = searchEditText.getText() != null ? searchEditText.getText().toString().trim() : "";
                if (!term.isEmpty()) {
                    findNextMatch(term);
                }
                return true;
            }
            return false;
        });
    }

    private void setupTextChangeListener() {
        if (textEditor == null) return;

        textEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Add to undo stack when text changes
                String current = s.toString();
                if (!current.equals(originalContent) && !undoStack.isEmpty() && !current.equals(undoStack.peek())) {
                    undoStack.push(current);
                    redoStack.clear(); // Clear redo stack when new changes are made
                }
            }
        });

        // Handle touch events to ensure proper focus
        textEditor.setOnTouchListener((v, event) -> {
            v.requestFocus();
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
    }

    private void searchInText(String searchTerm) {
        if (searchTerm.isEmpty()) {
            clearSearchResults();
            return;
        }

        // Update current search term and find all matches
        currentSearchTerm = searchTerm;
        findAllMatches(searchTerm);

        String text = textEditor.getText().toString();
        SpannableString spannable = new SpannableString(text);

        // Highlight all matches
        for (int matchIndex : searchMatches) {
            spannable.setSpan(
                new BackgroundColorSpan(0xFFFFFF00), // Yellow highlight color
                matchIndex,
                matchIndex + searchTerm.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        textEditor.setText(spannable);

        // Reset match index when search term changes
        currentMatchIndex = -1;
    }

    private void findAllMatches(String searchTerm) {
        searchMatches.clear();
        String text = textEditor.getText().toString();
        String lowerText = text.toLowerCase();
        String lowerTerm = searchTerm.toLowerCase();

        int index = lowerText.indexOf(lowerTerm);
        while (index >= 0) {
            searchMatches.add(index);
            index = lowerText.indexOf(lowerTerm, index + 1);
        }
    }

    private void clearSearchResults() {
        searchMatches.clear();
        currentMatchIndex = -1;
        currentSearchTerm = "";
        // Clear highlighting by resetting text
        String plain = textEditor.getText().toString();
        textEditor.setText(plain);
    }

    private void findNextMatch(String searchTerm) {
        // If search term changed, find all matches first
        if (!searchTerm.equals(currentSearchTerm)) {
            currentSearchTerm = searchTerm;
            findAllMatches(searchTerm);
            currentMatchIndex = -1;
        }

        if (searchMatches.isEmpty()) {
            Toast.makeText(requireContext(), "No matches found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Move to next match
        currentMatchIndex++;
        if (currentMatchIndex >= searchMatches.size()) {
            currentMatchIndex = 0; // Wrap around to first match
        }

        int matchPos = searchMatches.get(currentMatchIndex);
        textEditor.setSelection(matchPos, matchPos + searchTerm.length());
        textEditor.requestFocus();
        scrollToPosition(matchPos);

        Toast.makeText(requireContext(),
            "Match " + (currentMatchIndex + 1) + " of " + searchMatches.size(),
            Toast.LENGTH_SHORT).show();
    }

    private void scrollToPosition(int position) {
        // Get the layout of the EditText
        android.text.Layout layout = textEditor.getLayout();
        if (layout != null) {
            // Get the line number for the position
            int line = layout.getLineForOffset(position);

            // Get the Y coordinate of the line
            int lineTop = layout.getLineTop(line);
            int lineBottom = layout.getLineBottom(line);
            int lineHeight = lineBottom - lineTop;

            // Calculate scroll position to center the line in view
            int editorHeight = textEditor.getHeight();
            int scrollY = Math.max(0, lineTop - (editorHeight / 2) + (lineHeight / 2));

            // Scroll to the calculated position
            textEditor.scrollTo(0, scrollY);
        }
    }

    @Override
    protected void onApplyTheme() {
        View root = getView();
        if (root == null) return;
        ThemeUtils.refreshRippleEffects(root);
    }
}
