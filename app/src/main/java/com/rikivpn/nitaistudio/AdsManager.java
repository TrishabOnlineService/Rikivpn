package com.rikivpn.nitaistudio;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.appnext.ads.fullscreen.RewardedVideo;
import com.appnext.ads.interstitial.Interstitial;
import com.appnext.banners.BannerAdRequest;
import com.appnext.banners.BannerSize;
import com.appnext.banners.BannerView;
import com.appnext.core.callbacks.OnAdClosed;
import com.appnext.core.callbacks.OnAdError;
import com.appnext.core.callbacks.OnVideoEnded;

/**
 * Ads (AppNext SDK): banner, interstitial and rewarded video.
 *
 * - Banner: Home and Select Location.
 * - Interstitial: on VPN disconnect and when switching server (frequency-capped).
 * - Rewarded video: shown before connecting to a premium server. If no ad is available the
 *   connection proceeds anyway (an ad problem never blocks the VPN).
 *
 * NOTE: AppNext normally gives one placement ID per ad unit. All three constants below use the
 * same ID for now - replace INTERSTITIAL_PLACEMENT_ID / REWARDED_PLACEMENT_ID if AppNext gives
 * you separate ones.
 */
public final class AdsManager {

    public static final String APPNEXT_APP_ID = "208247234";

    private static final String PLACEMENT_ID = "95102692-9cf1-46e2-b9f8-5ebc4d51fd3c";
    public static final String BANNER_PLACEMENT_ID = PLACEMENT_ID;
    public static final String INTERSTITIAL_PLACEMENT_ID = PLACEMENT_ID;
    public static final String REWARDED_PLACEMENT_ID = PLACEMENT_ID;

    /** Minimum gap between two interstitials. */
    private static final long INTERSTITIAL_MIN_GAP_MS = 2 * 60 * 1000L;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static boolean sdkInitialized = false;

    private static Interstitial interstitial;
    private static long lastInterstitialShownAt = 0L;

    private static RewardedVideo rewarded;

    private AdsManager() {}

    /** Rewarded result codes. */
    public static final int REWARD_EARNED = 0;       // video watched to the end
    public static final int REWARD_UNAVAILABLE = 1;  // no fill / error / timeout - ad never played
    public static final int REWARD_SKIPPED = 2;      // ad was shown but user closed it early

    /** Result of a rewarded request. Always called exactly once, on the main thread. */
    public interface RewardCallback {
        void onResult(int result);
    }

    // ------------------------------------------------------------------ init

    /** Initialize the AppNext SDK. Call once from any Activity. */
    public static void initIfNeeded(Context context) {
        if (sdkInitialized) return;
        try {
            // Looked up reflectively so the build never depends on which AppNext artifact
            // ships the Appnext class. Ads still work if the call is unavailable.
            Class<?> c = Class.forName("com.appnext.core.Appnext");
            c.getMethod("init", Context.class).invoke(null, context.getApplicationContext());
        } catch (Throwable ignored) {
            // AppNext init not available - continue without it.
        }
        sdkInitialized = true;
    }

    public static boolean isInitialized() {
        return sdkInitialized;
    }

    // ---------------------------------------------------------------- banner

    /** Create a banner ad view and start loading it. */
    public static BannerView createBanner(Context context) {
        BannerView banner = new BannerView(context);
        banner.setPlacementId(BANNER_PLACEMENT_ID);
        banner.setBannerSize(BannerSize.BANNER);
        banner.loadAd(new BannerAdRequest());
        return banner;
    }

    // ------------------------------------------------------------ interstitial

    /** Start loading an interstitial so it is ready when needed. */
    public static void preloadInterstitial(Activity activity) {
        try {
            if (interstitial == null) {
                interstitial = new Interstitial(activity, INTERSTITIAL_PLACEMENT_ID);
                interstitial.setOnAdClosedCallback(new OnAdClosed() {
                    @Override
                    public void onAdClosed() {
                        reloadInterstitial();
                    }
                });
                interstitial.setOnAdErrorCallback(new OnAdError() {
                    @Override
                    public void adError(String error) {
                        // Nothing to do - next show attempt just does nothing / reloads.
                    }
                });
            }
            interstitial.loadAd();
        } catch (Throwable ignored) {
        }
    }

    private static void reloadInterstitial() {
        try {
            if (interstitial != null) interstitial.loadAd();
        } catch (Throwable ignored) {
        }
    }

    /** Show an interstitial if the frequency cap allows. Safe to call any time. */
    public static void maybeShowInterstitial(Activity activity) {
        try {
            long now = SystemClock.elapsedRealtime();
            if (lastInterstitialShownAt != 0L && now - lastInterstitialShownAt < INTERSTITIAL_MIN_GAP_MS) {
                return;
            }
            if (interstitial == null) {
                preloadInterstitial(activity);
                return;
            }
            lastInterstitialShownAt = now;
            // If the ad is not ready AppNext just reports an AD_NOT_READY error (ignored above).
            interstitial.showAd();
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- rewarded

    /** How long we wait for the rewarded video to load before giving up (and connecting anyway). */
    private static final long REWARDED_LOAD_WAIT_MS = 8000L;
    private static final long REWARDED_POLL_MS = 400L;
    /** After showAd(): if the activity still has window focus after this, no ad opened. */
    private static final long REWARDED_OPEN_CHECK_MS = 5000L;

    private static volatile boolean rewardedLoadFailed = false;

    /** Start loading a rewarded video so it is ready when needed. */
    public static void preloadRewarded(Activity activity) {
        try {
            ensureRewarded(activity);
            rewardedLoadFailed = false;
            rewarded.loadAd();
        } catch (Throwable ignored) {
        }
    }

    private static void ensureRewarded(Activity activity) {
        if (rewarded != null) return;
        rewarded = new RewardedVideo(activity, REWARDED_PLACEMENT_ID);
        // Registered at creation so load errors (no fill) are noticed while we wait.
        rewarded.setOnAdErrorCallback(new OnAdError() {
            @Override
            public void adError(String error) {
                rewardedLoadFailed = true;
            }
        });
    }

    /** Reflection keeps the build independent of the exact SDK method set. null = unknown. */
    private static Boolean isRewardedLoaded(Object ad) {
        try {
            return (Boolean) ad.getClass().getMethod("isAdLoaded").invoke(ad);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Show a rewarded video before a premium connect.
     *
     * Flow: wait (max 8s) for the ad to load -> show it -> callback.
     * Result is REWARD_EARNED (watched), REWARD_SKIPPED (closed early) or REWARD_UNAVAILABLE
     * (no ad / error / timeout). Callers should connect on EARNED and UNAVAILABLE so a missing
     * ad never blocks the VPN. The callback is invoked exactly once on the main thread.
     */
    public static void showRewarded(final Activity activity, final RewardCallback callback) {
        final boolean[] done = {false};
        final boolean[] earned = {false};
        final boolean[] shown = {false};
        final long startedAt = SystemClock.elapsedRealtime();

        final java.util.function.IntConsumer finish = result -> {
            if (done[0]) return;
            done[0] = true;
            callback.onResult(result);
        };

        try {
            ensureRewarded(activity);
            final RewardedVideo ad = rewarded;
            if (!shown[0] && !Boolean.TRUE.equals(isRewardedLoaded(ad))) {
                rewardedLoadFailed = false;
                ad.loadAd();
            }

            final Runnable[] poll = new Runnable[1];
            poll[0] = () -> {
                if (done[0] || shown[0]) return;
                if (activity.isFinishing() || activity.isDestroyed()) {
                    finish.accept(REWARD_UNAVAILABLE);
                    return;
                }
                Boolean loaded = isRewardedLoaded(ad);
                boolean ready = loaded == null || loaded; // unknown -> just try to show
                if (ready) {
                    showLoadedRewarded(activity, ad, done, earned, shown, finish);
                } else if (rewardedLoadFailed
                        || SystemClock.elapsedRealtime() - startedAt >= REWARDED_LOAD_WAIT_MS) {
                    finish.accept(REWARD_UNAVAILABLE);
                } else {
                    MAIN.postDelayed(poll[0], REWARDED_POLL_MS);
                }
            };
            MAIN.post(poll[0]);
        } catch (Throwable t) {
            finish.accept(REWARD_UNAVAILABLE);
        }
    }

    private static void showLoadedRewarded(final Activity activity, final RewardedVideo ad,
                                           final boolean[] done, final boolean[] earned,
                                           final boolean[] shown,
                                           final java.util.function.IntConsumer finish) {
        shown[0] = true;
        try {
            ad.setOnVideoEndedCallback(new OnVideoEnded() {
                @Override
                public void videoEnded() {
                    earned[0] = true;
                    // Safety net in case the "closed" callback never arrives.
                    MAIN.postDelayed(() -> finish.accept(REWARD_EARNED), 4000);
                }
            });
            ad.setOnAdClosedCallback(new OnAdClosed() {
                @Override
                public void onAdClosed() {
                    MAIN.post(() -> {
                        finish.accept(earned[0] ? REWARD_EARNED : REWARD_SKIPPED);
                        reloadRewarded();
                    });
                }
            });
            ad.setOnAdErrorCallback(new OnAdError() {
                @Override
                public void adError(String error) {
                    rewardedLoadFailed = true;
                    // Ad failed while showing: do not block the connection.
                    MAIN.post(() -> {
                        finish.accept(REWARD_UNAVAILABLE);
                        reloadRewarded();
                    });
                }
            });
            ad.showAd();

            // If the ad never opened (activity keeps window focus), do not leave the user stuck.
            MAIN.postDelayed(() -> {
                if (!done[0] && !earned[0] && activity.hasWindowFocus()) {
                    finish.accept(REWARD_UNAVAILABLE);
                    reloadRewarded();
                }
            }, REWARDED_OPEN_CHECK_MS);
        } catch (Throwable t) {
            finish.accept(REWARD_UNAVAILABLE);
        }
    }

    private static void reloadRewarded() {
        try {
            rewardedLoadFailed = false;
            if (rewarded != null) rewarded.loadAd();
        } catch (Throwable ignored) {
        }
    }

    /** Drop the interstitial held for the Home Activity (call from its onDestroy). */
    public static void release() {
        interstitial = null;
    }

    /** Drop the rewarded ad held for the Select Location Activity (call from its onDestroy). */
    public static void releaseRewarded() {
        rewarded = null;
    }
}
