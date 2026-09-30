package com.rikivpn.nitaistudio;

import android.view.View;

import androidx.viewpager2.widget.ViewPager2;

/** Subtle fade + scale transition as onboarding pages are swiped. */
public class FadeScalePageTransformer implements ViewPager2.PageTransformer {

    private static final float MIN_SCALE = 0.88f;

    @Override
    public void transformPage(@androidx.annotation.NonNull View page, float position) {
        float absPos = Math.abs(position);
        page.setAlpha(1f - Math.min(absPos, 1f) * 0.7f);
        float scale = MIN_SCALE + (1f - MIN_SCALE) * (1f - Math.min(absPos, 1f));
        page.setScaleX(scale);
        page.setScaleY(scale);
        page.setTranslationX(-position * page.getWidth() * 0.15f);
    }
}
