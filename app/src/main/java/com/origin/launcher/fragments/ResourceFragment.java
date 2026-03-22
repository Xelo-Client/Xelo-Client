package com.origin.launcher.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import android.util.Log;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ResourceFragment extends BaseThemedFragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ResourceTabAdapter tabAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resource, container, false);
        
        // Initialize tab components
        initializeTabs(view);
        
        return view;
    }
    
    private void initializeTabs(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);
        
        // Set up the adapter
        tabAdapter = new ResourceTabAdapter(this);
        viewPager.setAdapter(tabAdapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                    case 0:
                        tab.setText("Packs");
                        break;
                    case 1:
                        tab.setText("Addons");
                        break;
                    case 2:
                        tab.setText("Maps");
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
        DiscordRPCHelper.getInstance().updateMenuPresence("Resource installer");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        DiscordRPCHelper.getInstance().updateIdlePresence();
    }
    


    private static class ResourceTabAdapter extends FragmentStateAdapter {
        
        public ResourceTabAdapter(Fragment fragment) {
            super(fragment);
        }
        
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new ResourcePacksFragment();
                case 1:
                    return new ResourceAddonsFragment();
                case 2:
                    return new ResourceMapsFragment();
                default:
                    return new ResourcePacksFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 3;
        }
    }
}