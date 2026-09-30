package com.rikivpn.nitaistudio;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.VideoListener;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

/**
 * Ads (Start.io). No subscription tier anymore - the app is fully free/ad-supported,
 * so ads are always initialized and shown for every user.
 *
 * - Banner: shown on Home and Select Location.
 * - Interstitial: every 3rd disconnect / free-server-switch (see maybeShowInterstitial).
 * - Ad-gated premium unlock: tapping a "premium" server offers ONE opt-in rewarded video
 *   (see showRewardedVideo). The server unlocks only after the video is watched to the end.
 */
public final class AdsManager {

    public static final String START_IO_APP_ID = "208247234";

    private static final String PREFS_NAME = "riki_ads_prefs";
    private static final String KEY_EVENT_COUNTER = "interstitial_event_counter";
    private static final int INTERSTITIAL_EVERY_N = 3;
    private static final long REWARDED_LOAD_TIMEOUT_MS = 15_000;

    private static final String KEY_CONSENT_ANSWERED = "ads_consent_answered";
    private static final String KEY_PERSONALIZED = "ads_personalized";

    private static boolean sdkInitialized = false;

    private AdsManager() {}

    /** Call once (e.g. from MainActivity.onCreate / SelectLocationActivity.onCreate). */
    public static void initIfNeeded(Context context) {
        if (sdkInitialized) return;
        // Tell Start.io whether the user agreed to personalised ads (must be set BEFORE init).
        boolean personalized = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PERSONALIZED, false);
        StartAppSDK.setUserConsent(context.getApplicationContext(), "pas",
                System.currentTimeMillis(), personalized);
        StartAppSDK.initParams(context.getApplicationContext(), START_IO_APP_ID)
                .setReturnAdsEnabled(false)
                .init();
        sdkInitialized = true;
    }

    /**
     * Shows a one-time ads/data consent dialog (Google Play "prominent disclosure & consent"),
     * then runs onReady. If the user already answered, onReady runs immediately.
     * Ads are never initialised before this returns.
     */
    public static void ensureConsent(Activity activity, Runnable onReady) {
        SharedPreferences prefs = activity.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_CONSENT_ANSWERED, false)) {
            onReady.run();
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.consent_title)
                .setMessage(R.string.consent_body)
                .setCancelable(false)
                .setPositiveButton(R.string.consent_accept, (d, w) -> {
                    prefs.edit().putBoolean(KEY_CONSENT_ANSWERED, true)
                            .putBoolean(KEY_PERSONALIZED, true).apply();
                    onReady.run();
                })
                .setNegativeButton(R.string.consent_decline, (d, w) -> {
                    prefs.edit().putBoolean(KEY_CONSENT_ANSWERED, true)
                            .putBoolean(KEY_PERSONALIZED, false).apply();
                    onReady.run();
                })
                .setNeutralButton(R.string.consent_policy, null)
                .create();
        dialog.setOnShowListener(di ->
                // Keep the dialog open when reading the policy (default neutral button would dismiss it).
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v ->
                        activity.startActivity(new Intent(activity, PrivacyPolicyActivity.class))));
        dialog.show();
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

    /** Result callback for {@link #showRewardedVideo}. Exactly one method is called, on the UI thread. */
    public interface RewardListener {
        /** The user watched the video to the end and closed it - grant the reward. */
        void onRewarded();

        /** No reward: the ad failed to load/show, timed out, or the user closed it early. */
        void onNotRewarded(boolean adUnavailable);
    }

    /**
     * Loads and shows one Start.io rewarded video. The reward is granted only after the video
     * completed AND the ad screen was closed (so we never navigate away underneath the ad).
     */
    public static void showRewardedVideo(Activity activity, RewardListener listener) {
        final Handler ui = new Handler(Looper.getMainLooper());
        final boolean[] finished = {false};
        final boolean[] completed = {false};

        final Runnable[] timeoutHolder = new Runnable[1];

        final StartAppAd rewardedAd = new StartAppAd(activity);
        final Runnable onTimeout = () -> {
            if (finished[0]) return;
            finished[0] = true;
            listener.onNotRewarded(true);
        };
        timeoutHolder[0] = onTimeout;
        ui.postDelayed(onTimeout, REWARDED_LOAD_TIMEOUT_MS);

        rewardedAd.setVideoListener(new VideoListener() {
            @Override
            public void onVideoCompleted() {
                completed[0] = true;
            }
        });

        rewardedAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
            @Override
            public void onReceiveAd(@NonNull Ad ad) {
                if (finished[0]) return;
                ui.removeCallbacks(timeoutHolder[0]); // loaded in time; the ad UI takes over
                if (activity.isFinishing() || activity.isDestroyed()) {
                    finished[0] = true; // screen is gone - drop silently, nothing to update
                    return;
                }
                rewardedAd.showAd(new AdDisplayListener() {
                    @Override
                    public void adHidden(Ad ad) {
                        if (finished[0]) return;
                        finished[0] = true;
                        if (completed[0]) listener.onRewarded();
                        else listener.onNotRewarded(false);
                    }

                    @Override
                    public void adDisplayed(Ad ad) {}

                    @Override
                    public void adClicked(Ad ad) {}

                    @Override
                    public void adNotDisplayed(Ad ad) {
                        if (finished[0]) return;
                        finished[0] = true;
                        listener.onNotRewarded(true);
                    }
                });
            }

            @Override
            public void onFailedToReceiveAd(@Nullable Ad ad) {
                if (finished[0]) return;
                finished[0] = true;
                ui.removeCallbacks(timeoutHolder[0]);
                listener.onNotRewarded(true);
            }
        });
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
