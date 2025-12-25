package com.origin.launcher;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.origin.launcher.animation.DynamicAnim;
import com.origin.launcher.Adapter.SettingsAdapter;

public class ConfigurationFragment extends BaseThemedFragment {

    private LinearLayout settingsItemsContainer;
    private RecyclerView settingsRecyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_configuration, container, false);

        DynamicAnim.applyPressScaleRecursively(view);
        
        ImageButton backButton = view.findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        settingsRecyclerView = view.findViewById(R.id.settings_recycler);
        settingsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        settingsRecyclerView.setAdapter(new SettingsAdapter(containerView -> {
            settingsItemsContainer = containerView;

            FeatureSettings fs = FeatureSettings.getInstance();
            addSwitchItem(
                    getString(R.string.version_isolation),
                    fs.isVersionIsolationEnabled(),
                    (btn, checked) -> fs.setVersionIsolationEnabled(checked)
            );
            addSwitchItem(
                    getString(R.string.show_logcat_overlay),
                    fs.isLogcatOverlayEnabled(),
                    (btn, checked) -> {
                        fs.setLogcatOverlayEnabled(checked);
                        try {
                            LogcatOverlayManager mgr = LogcatOverlayManager.getInstance();
                            if (mgr != null) mgr.refreshVisibility();
                        } catch (Throwable ignored) {}
                    }
            );
        }));

        settingsRecyclerView.post(() ->
                DynamicAnim.staggerRecyclerChildren(settingsRecyclerView));

        return view;
    }

    private void addSwitchItem(String label,
                               boolean defChecked,
                               Switch.OnCheckedChangeListener listener) {
        View ll = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_settings_switch, settingsItemsContainer, false);
        ((TextView) ll.findViewById(R.id.tv_title)).setText(label);
        Switch sw = ll.findViewById(R.id.switch_value);
        sw.setChecked(defChecked);
        if (listener != null) sw.setOnCheckedChangeListener(listener);
        settingsItemsContainer.addView(ll);
    }

    private Spinner addSpinnerItem(String label,
                                   String[] options,
                                   int defaultIdx) {
        View ll = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_settings_spinner, settingsItemsContainer, false);
        ((TextView) ll.findViewById(R.id.tv_title)).setText(label);
        Spinner spinner = ll.findViewById(R.id.spinner_value);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(), R.layout.spinner_item, options);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPopupBackgroundResource(R.drawable.bg_popup_menu_rounded);
        DynamicAnim.applyPressScale(spinner);
        spinner.setSelection(defaultIdx);
        settingsItemsContainer.addView(ll);
        return spinner;
    }

    private void addActionButton(String label,
                                 String buttonText,
                                 View.OnClickListener listener) {
        View ll = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_settings_button, settingsItemsContainer, false);
        ((TextView) ll.findViewById(R.id.tv_title)).setText(label);
        Button btn = ll.findViewById(R.id.btn_action);
        btn.setText(buttonText);
        btn.setOnClickListener(listener);
        settingsItemsContainer.addView(ll);
    }
    
    @Override
    protected void onApplyTheme() {
        View rootView = getView();
        if (rootView == null) return;
        
        rootView.setBackgroundColor(ThemeManager.getInstance().getColor("background"));
        
        ImageButton backButton = rootView.findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setColorFilter(ThemeManager.getInstance().getColor("onBackground"));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        DiscordRPCHelper.getInstance().updateMenuPresence("Configuration");
    }

    @Override
    public void onPause() {
        super.onPause();
        DiscordRPCHelper.getInstance().updateIdlePresence();
    }
}