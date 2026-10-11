package com.rikivpn.nitaistudio;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Animated splash screen for RikiVpn.
 * Logo scales + fades in with an overshoot bounce, followed by the app name,
 * tagline and "Made in India" badge sliding/fading in in sequence.
 * After a short brand moment, routes to Onboarding (first launch) or MainActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "riki_prefs";
    private static final String KEY_ONBOARDED = "onboarding_complete";
    private static final long NAVIGATE_DELAY_MS = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_RikiVpn_Splash);
        setContentView(R.layout.activity_splash);

        ImageView ivLogo = findViewById(R.id.ivLogo);
        TextView tvAppName = findViewById(R.id.tvAppName);
        TextView tvTagline = findViewById(R.id.tvTagline);
        View layoutBadge = findViewById(R.id.layoutBadge);

        runEntranceAnimation(ivLogo, tvAppName, tvTagline, layoutBadge);

        new android.os.Handler(getMainLooper()).postDelayed(this::navigateNext, NAVIGATE_DELAY_MS);
    }

    private void runEntranceAnimation(ImageView ivLogo, TextView tvAppName, TextView tvTagline, View layoutBadge) {
        // Logo: fade in + scale up with a slight bounce
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(ivLogo, View.ALPHA, 0f, 1f);
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(ivLogo, View.SCALE_X, 0.6f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(ivLogo, View.SCALE_Y, 0.6f, 1f);
        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(logoAlpha, logoScaleX, logoScaleY);
        logoSet.setDuration(650);
        logoSet.setInterpolator(new OvershootInterpolator(1.6f));

        // App name: fade + rise
        ObjectAnimator nameAlpha = ObjectAnimator.ofFloat(tvAppName, View.ALPHA, 0f, 1f);
        ObjectAnimator nameTranslate = ObjectAnimator.ofFloat(tvAppName, View.TRANSLATION_Y, 24f, 0f);
        AnimatorSet nameSet = new AnimatorSet();
        nameSet.playTogether(nameAlpha, nameTranslate);
        nameSet.setDuration(450);
        nameSet.setInterpolator(new DecelerateInterpolator());
        nameSet.setStartDelay(350);

        // Tagline fade in
        ObjectAnimator taglineAlpha = ObjectAnimator.ofFloat(tvTagline, View.ALPHA, 0f, 1f);
        taglineAlpha.setDuration(400);
        taglineAlpha.setStartDelay(550);

        // Badge slides up from bottom while fading in
        ObjectAnimator badgeAlpha = ObjectAnimator.ofFloat(layoutBadge, View.ALPHA, 0f, 1f);
        ObjectAnimator badgeTranslate = ObjectAnimator.ofFloat(layoutBadge, View.TRANSLATION_Y, 40f, 0f);
        AnimatorSet badgeSet = new AnimatorSet();
        badgeSet.playTogether(badgeAlpha, badgeTranslate);
        badgeSet.setDuration(500);
        badgeSet.setInterpolator(new DecelerateInterpolator());
        badgeSet.setStartDelay(750);

        AnimatorSet full = new AnimatorSet();
        full.playTogether(logoSet, nameSet, taglineAlpha, badgeSet);
        full.start();

        // Subtle continuous "breathing" pulse on the logo while it waits
        logoSet.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationEnd(Animator animation) {
                startPulse(ivLogo);
            }
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
        });
    }

    private void startPulse(ImageView ivLogo) {
        ValueAnimator pulse = ValueAnimator.ofFloat(1f, 1.08f, 1f);
        pulse.setDuration(1400);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        pulse.addUpdateListener(animation -> {
            float v = (float) animation.getAnimatedValue();
            ivLogo.setScaleX(v);
            ivLogo.setScaleY(v);
        });
        pulse.start();
    }

    private void navigateNext() {
        if (isFinishing()) return;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean onboarded = prefs.getBoolean(KEY_ONBOARDED, false);

        Intent intent = new Intent(this, onboarded ? MainActivity.class : OnboardingActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
