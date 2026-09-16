package com.rikivpn.nitaistudio;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DisclaimerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disclaimer);

        findViewById(R.id.ivBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        TextView tvContent = findViewById(R.id.tvDisclaimerContent);
        tvContent.setText(getString(R.string.disclaimer_body, getString(R.string.support_email)));
    }
}
