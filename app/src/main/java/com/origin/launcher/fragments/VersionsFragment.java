package com.origin.launcher.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import android.util.Log;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.origin.launcher.utils.ThemeUtils;
import com.origin.launcher.discord.DiscordRPCHelper;
import com.origin.launcher.R;

public class VersionsFragment extends BaseThemedFragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private VersionsTabAdapter tabAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_versions, container, false);
        
        initializeTabs(view);
        
        return view;
    }
    
    private void initializeTabs(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
        
        tabAdapter = new VersionsTabAdapter(this);
        viewPager.setAdapter(tabAdapter);
        
        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                    case 0:
                        tab.setText("stable");
                        break;
                    case 1:
                        tab.setText("beta");
                        break;
                    default:
                        tab.setText("Tab " + (position + 1));
                }
            }).attach();
        // Apply theme to TabLayout after attaching
        ThemeUtils.applyThemeToTabLayout(tabLayout);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        DiscordRPCHelper.getInstance().updateMenuPresence("version manager");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        DiscordRPCHelper.getInstance().updateIdlePresence();
    }
    
    private static class VersionsTabAdapter extends FragmentStateAdapter {
        
        public VersionsTabAdapter(Fragment fragment) {
            super(fragment);
        }
        
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new VersionsStableFragment();
                case 1:
                    return new VersionsBetaFragment();
                default:
                    return new VersionsStableFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 2;
        }
    }
}