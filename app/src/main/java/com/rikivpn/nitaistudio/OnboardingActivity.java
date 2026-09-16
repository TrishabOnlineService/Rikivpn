package com.rikivpn.nitaistudio;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "riki_prefs";
    private static final String KEY_ONBOARDED = "onboarding_complete";

    private ViewPager2 viewPager;
    private LinearLayout layoutDots;
    private MaterialButton btnNext;
    private int pageCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        layoutDots = findViewById(R.id.layoutDots);
        btnNext = findViewById(R.id.btnNext);
        TextView tvSkip = findViewById(R.id.tvSkip);

        List<OnboardingAdapter.Page> pages = new ArrayList<>();
        pages.add(new OnboardingAdapter.Page(R.drawable.ic_privacy,
                getString(R.string.onboard1_title), getString(R.string.onboard1_desc)));
        pages.add(new OnboardingAdapter.Page(R.drawable.ic_speed,
                getString(R.string.onboard2_title), getString(R.string.onboard2_desc)));
        pages.add(new OnboardingAdapter.Page(R.drawable.ic_flag_india,
                getString(R.string.onboard3_title), getString(R.string.onboard3_desc)));

        pageCount = pages.size();
        viewPager.setAdapter(new OnboardingAdapter(pages));
        viewPager.setPageTransformer(new FadeScalePageTransformer());

        setupDots();
        updateDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                animateButton(position == pageCount - 1);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < pageCount - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });

        tvSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void setupDots() {
        layoutDots.removeAllViews();
        for (int i = 0; i < pageCount; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(6, 0, 6, 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.dot_inactive);
            layoutDots.addView(dot);
        }
    }

    private void updateDots(int selected) {
        for (int i = 0; i < layoutDots.getChildCount(); i++) {
            View dot = layoutDots.getChildAt(i);
            boolean isActive = i == selected;
            dot.setBackgroundResource(isActive ? R.drawable.dot_active : R.drawable.dot_inactive);
            ObjectAnimator.ofFloat(dot, View.SCALE_Y, isActive ? 0.6f : 1f, 1f)
                    .setDuration(220)
                    .start();
        }
    }

    private void animateButton(boolean isLastPage) {
        btnNext.animate().alpha(0.4f).setDuration(100).withEndAction(() -> {
            btnNext.setText(isLastPage ? R.string.onboard_get_started : R.string.onboard_next);
            btnNext.animate().alpha(1f).setDuration(180).setInterpolator(new DecelerateInterpolator()).start();
        }).start();
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDED, true).apply();
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
