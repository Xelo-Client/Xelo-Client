package com.origin.launcher.utils;

import android.os.Bundle;

import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.origin.launcher.activity.BaseThemedActivity;
import com.origin.launcher.R;

public class Fallback extends BaseThemedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fallback);
        TextView logOut = findViewById(R.id.logOut);
        String log = getIntent().getStringExtra("LOG_STR");
        logOut.setText(log);
    }
}