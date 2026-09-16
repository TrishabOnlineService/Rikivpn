package com.rikivpn.nitaistudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.startapp.sdk.ads.banner.Banner;

import java.util.List;

/**
 * Feature 1 - Select Location screen.
 *
 * Two tabs (Free / Premium) backed by servers discovered at runtime by
 * {@link ServerCatalog} (Feature 2 - no per-country hardcoding here).
 *
 * There is no subscription anymore - every server is free to use:
 *  - Free server -> persist selection, return to Home.
 *  - Premium server -> play 5 interstitial ads back to back, then persist selection,
 *    return to Home, and tell MainActivity to auto-connect.
 */
public class SelectLocationActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_SERVER_KEY = "extra_selected_server_key";
    public static final String EXTRA_AUTO_CONNECT = "extra_auto_connect";

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private ServerAdapter adapter;
    private View adUnlockOverlay;
    private TextView tvAdUnlockStatus;

    private List<VpnServer> freeServers;
    private List<VpnServer> premiumServers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_location);

        findViewById(R.id.ivBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerServers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adUnlockOverlay = findViewById(R.id.adUnlockOverlay);
        tvAdUnlockStatus = findViewById(R.id.tvAdUnlockStatus);

        freeServers = ServerCatalog.scanFree(this);
        premiumServers = ServerCatalog.scanPremium(this);

        String currentSelection = ServerPreferences.getSelectedServerKey(this);

        adapter = new ServerAdapter(this::onServerClicked, currentSelection);
        recyclerView.setAdapter(adapter);

        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_free_servers));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_premium_servers));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Open on whichever tab the current selection belongs to.
        boolean currentIsPremium = currentSelection != null && currentSelection.startsWith("premium:");
        tabLayout.selectTab(tabLayout.getTabAt(currentIsPremium ? 1 : 0));
        showTab(currentIsPremium ? 1 : 0);

        // Feature 5: banner ad - shown for everyone, app is fully free/ad-supported now.
        AdsManager.initIfNeeded(this);
        FrameLayout adContainer = findViewById(R.id.adBannerContainer);
        adContainer.addView(new Banner(this));
    }

    private void showTab(int position) {
        adapter.submitList(position == 0 ? freeServers : premiumServers);
    }

    private void onServerClicked(VpnServer server) {
        if (server.premium) {
            runPremiumAdUnlock(server);
        } else {
            selectAndReturn(server, false);
        }
    }

    /** Premium servers are free - tapping one just plays 5 interstitials back to back first. */
    private void runPremiumAdUnlock(VpnServer server) {
        setUiLocked(true, 0);
        AdsManager.showAdSequence(this, () -> {
            // Runs on UI thread once all 5 ad slots have been shown or skipped.
            selectAndReturn(server, true);
        });
    }

    private void setUiLocked(boolean locked, int adsShownSoFar) {
        recyclerView.setEnabled(!locked);
        tabLayout.setEnabled(!locked);
        adUnlockOverlay.setVisibility(locked ? View.VISIBLE : View.GONE);
        if (locked) {
            tvAdUnlockStatus.setText(getString(R.string.watch_ads_progress, AdsManager.PREMIUM_UNLOCK_AD_COUNT));
        }
    }

    private void selectAndReturn(VpnServer server, boolean autoConnect) {
        ServerPreferences.setSelectedServerKey(this, server.key());
        Intent result = new Intent();
        result.putExtra(EXTRA_SELECTED_SERVER_KEY, server.key());
        result.putExtra(EXTRA_AUTO_CONNECT, autoConnect);
        setResult(RESULT_OK, result);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
