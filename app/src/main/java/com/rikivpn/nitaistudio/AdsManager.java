package com.rikivpn.nitaistudio;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

/**
 * Ads (Start.io). No subscription tier anymore - the app is fully free/ad-supported,
 * so ads are always initialized and shown for every user.
 *
 * - Banner: shown on Home and Select Location.
 * - Interstitial: every 3rd disconnect / free-server-switch (see maybeShowInterstitial).
 * - Ad-gated premium unlock: tapping a "premium" server now shows 5 interstitials back to
 *   back (see showAdSequence) instead of requiring a purchase; once the 5th ad is dismissed
 *   the caller proceeds to select + connect that server.
 */
public final class AdsManager {

    public static final String START_IO_APP_ID = "208247234";

    private static final String PREFS_NAME = "riki_ads_prefs";
    private static final String KEY_EVENT_COUNTER = "interstitial_event_counter";
    private static final int INTERSTITIAL_EVERY_N = 3;
    public static final int PREMIUM_UNLOCK_AD_COUNT = 5;

    private static boolean sdkInitialized = false;

    private AdsManager() {}

    /** Call once (e.g. from MainActivity.onCreate / SelectLocationActivity.onCreate). */
    public static void initIfNeeded(Context context) {
        if (sdkInitialized) return;
        StartAppSDK.initParams(context.getApplicationContext(), START_IO_APP_ID)
                .setReturnAdsEnabled(false)
                .init();
        sdkInitialized = true;
    }

    public static boolean isInitialized() {
        return sdkInitialized;
    }

    /** Call from the two allowed trigger points only: VPN disconnect and free-server switch. */
    public static void maybeShowInterstitial(Activity activity) {
        int count = incrementAndGetCounter(activity);
        if (count % INTERSTITIAL_EVERY_N != 0) return;
        showSingleInterstitial(activity, null);
    }

    /**
     * Shows {@link #PREMIUM_UNLOCK_AD_COUNT} interstitials back to back, then calls onAllDone
     * on the UI thread. If an ad fails to load, that slot is skipped (never blocks the user
     * indefinitely waiting on ad fill) but still counts towards the total.
     */
    public static void showAdSequence(Activity activity, Runnable onAllDone) {
        showAdSequenceStep(activity, PREMIUM_UNLOCK_AD_COUNT, onAllDone);
    }

    private static void showAdSequenceStep(Activity activity, int remaining, Runnable onAllDone) {
        if (remaining <= 0) {
            activity.runOnUiThread(onAllDone);
            return;
        }
        showSingleInterstitial(activity, () -> showAdSequenceStep(activity, remaining - 1, onAllDone));
    }

    /** Loads + shows one interstitial. Calls onFinished (success or failure) exactly once, or immediately if no callback is needed. */
    private static void showSingleInterstitial(Activity activity, @Nullable Runnable onFinished) {
        StartAppAd interstitialAd = new StartAppAd(activity);
        interstitialAd.loadAd(new AdEventListener() {
            @Override
            public void onReceiveAd(@NonNull Ad ad) {
                interstitialAd.showAd(new AdDisplayListener() {
                    @Override
                    public void adHidden(Ad ad) {
                        if (onFinished != null) onFinished.run();
                    }

                    @Override
                    public void adDisplayed(Ad ad) {}

                    @Override
                    public void adClicked(Ad ad) {}

                    @Override
                    public void adNotDisplayed(Ad ad) {
                        if (onFinished != null) onFinished.run();
                    }
                });
            }

            @Override
            public void onFailedToReceiveAd(@Nullable Ad ad) {
                // No fill - skip this slot rather than blocking the user.
                if (onFinished != null) onFinished.run();
            }
        });
    }

    private static int incrementAndGetCounter(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int next = prefs.getInt(KEY_EVENT_COUNTER, 0) + 1;
        prefs.edit().putInt(KEY_EVENT_COUNTER, next).apply();
        return next;
    }
}
