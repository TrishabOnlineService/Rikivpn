package com.rikivpn.nitaistudio;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        findViewById(R.id.ivBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        TextView tvVersion = findViewById(R.id.tvAppVersion);
        // Version read dynamically from BuildConfig, never hardcoded (Feature 3 - About requirement).
        tvVersion.setText(getString(R.string.version_format, BuildConfig.VERSION_NAME));
    }
}
