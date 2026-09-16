package com.rikivpn.nitaistudio;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.startapp.sdk.ads.banner.Banner;

import ai.bongotech.bongovpn.BongoVpn;

public class MainActivity extends AppCompatActivity {

    MaterialButton btnConnect;
    TextView tvDownloadSpeed, tvUploadSpeed, tvSessionUsage;
    ImageView ivStatusIcon, ivInfo, ivMenu;
    View ringOuter, ringInner, tvPrivacyLink;

    // Feature 3: side drawer
    DrawerLayout drawerLayout;
    NavigationView navigationView;

    // Feature 1: selected server row
    View rowSelectedServer;
    TextView tvSelectedFlag, tvSelectedCountry;
    private VpnServer currentServer;

    boolean isConnected = false;
    public static String VPN_USERNAME = "vpn";
    public static String VPN_PASSWORD = "vpn";
    private BongoVpn bongoVpn = new BongoVpn(this);
    private ValueAnimator connectingPulse;

    // Feature 1 result: returns here after picking a server on SelectLocationActivity
    private final ActivityResultLauncher<Intent> selectLocationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                boolean serverChanged = updateSelectedServerFromPrefs();
                boolean autoConnect = result.getData() != null
                        && result.getData().getBooleanExtra(SelectLocationActivity.EXTRA_AUTO_CONNECT, false);
                if (autoConnect) {
                    // Returning from the premium 5-ad unlock sequence - connect right away.
                    if (!isConnected) {
                        btnConnect.setEnabled(false);
                        btnConnect.setText(getString(R.string.connecting));
                        startConnectingPulse();
                        connectVpn();
                    }
                } else if (serverChanged) {
                    // Feature 5: interstitial allowed on a free-server switch (frequency-capped).
                    AdsManager.maybeShowInterstitial(MainActivity.this);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnConnect = findViewById(R.id.btnConnect);
        tvDownloadSpeed = findViewById(R.id.tvDownloadSpeed);
        tvUploadSpeed = findViewById(R.id.tvUploadSpeed);
        tvSessionUsage = findViewById(R.id.tvSessionUsage);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        ivInfo = findViewById(R.id.ivInfo);
        ivMenu = findViewById(R.id.ivMenu);
        ringOuter = findViewById(R.id.ringOuter);
        ringInner = findViewById(R.id.ringInner);
        tvPrivacyLink = findViewById(R.id.tvPrivacyLink);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        rowSelectedServer = findViewById(R.id.rowSelectedServer);
        tvSelectedFlag = findViewById(R.id.tvSelectedFlag);
        tvSelectedCountry = findViewById(R.id.tvSelectedCountry);

        // Entrance animation for the status circle
        ivStatusIcon.setScaleX(0.7f);
        ivStatusIcon.setScaleY(0.7f);
        ivStatusIcon.setAlpha(0f);
        ivStatusIcon.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(450)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f)).start();

        View.OnClickListener openLegal = v -> {
            startActivity(new Intent(MainActivity.this, PrivacyPolicyActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        };
        ivInfo.setOnClickListener(openLegal);
        tvPrivacyLink.setOnClickListener(openLegal);

        // Feature 3: side drawer menu
        ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        navigationView.setNavigationItemSelectedListener(this::onDrawerItemSelected);

        // Feature 1: tapping the selected-server row opens Select Location
        rowSelectedServer.setOnClickListener(v ->
                selectLocationLauncher.launch(new Intent(MainActivity.this, SelectLocationActivity.class)));

        // Ads: app is fully free now, ads are always initialized and the banner always shown.
        AdsManager.initIfNeeded(this);
        FrameLayout adContainer = findViewById(R.id.adBannerContainer);
        adContainer.addView(new Banner(this));

        // Request notification permission on startup (NOT Mandatory)
        if (!bongoVpn.hasNotificationPermission()) {
            bongoVpn.requestNotificationPermission();
        }

        // Feature 1/2: attach whichever server the user last selected (defaults to the
        // bundled free sample server). Never hardcoded past first launch.
        updateSelectedServerFromPrefs();

        // Attach Listener
        bongoVpn.setVpnListener(new BongoVpn.VpnListener() {
            @Override
            public void onVpnConnected() {
                btnConnect.setEnabled(true);
                isConnected = true;
                btnConnect.setText(getString(R.string.disconnect_vpn));
                btnConnect.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.riki_danger));
                stopConnectingPulse();
                setConnectedVisualState(true);
            }

            @Override
            public void onVpnStopped() {
                btnConnect.setEnabled(true);
                isConnected = false;
                btnConnect.setText(getString(R.string.connect_vpn));
                btnConnect.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.connect_button_text));
                stopConnectingPulse();
                setConnectedVisualState(false);
                // Feature 5: interstitial allowed on VPN Disconnect (frequency-capped).
                AdsManager.maybeShowInterstitial(MainActivity.this);
            }

            @Override
            public void onStatusUpdate(String status) {
                tvSessionUsage.setText(status);
            }

            @Override
            public void onError(String errorMessage) {
                btnConnect.setEnabled(true);
                tvSessionUsage.setText(errorMessage);
                stopConnectingPulse();
            }

            @Override
            public void onSpeedUpdate(long downloadBytes, long uploadBytes, long downloadSpeed, long uploadSpeed) {
                tvDownloadSpeed.setText(bongoVpn.formatSpeed(downloadSpeed));
                tvUploadSpeed.setText(bongoVpn.formatSpeed(uploadSpeed));
                tvSessionUsage.setText("Down: " + BongoVpn.formatBytes(downloadBytes) + " | Up: " + BongoVpn.formatBytes(uploadBytes));
            }
        });


        // Connect Button Onclick Listener
        btnConnect.setOnClickListener(view -> {
            btnConnect.setEnabled(false);
            bounceButton();
            if (isConnected) {
                btnConnect.setText(getString(R.string.disconnecting));
                stopVpn();
            } else {
                btnConnect.setText(getString(R.string.connecting));
                startConnectingPulse();
                connectVpn();
            }
        });


        // Update Notification if needed
        bongoVpn.showNotification()
                .title(getString(R.string.app_name))
                .connectedText("Connected and Secured")
                .smallIcon(R.drawable.security_icon)
                .showSpeed(true)
                .targetActivity(MainActivity.class);

    } // end of onCreate()


    private void bounceButton() {
        ObjectAnimator down = ObjectAnimator.ofFloat(btnConnect, View.SCALE_X, 1f, 0.96f);
        ObjectAnimator downY = ObjectAnimator.ofFloat(btnConnect, View.SCALE_Y, 1f, 0.96f);
        ObjectAnimator up = ObjectAnimator.ofFloat(btnConnect, View.SCALE_X, 0.96f, 1f);
        ObjectAnimator upY = ObjectAnimator.ofFloat(btnConnect, View.SCALE_Y, 0.96f, 1f);
        AnimatorSet down_set = new AnimatorSet();
        down_set.playTogether(down, downY);
        down_set.setDuration(80);
        AnimatorSet up_set = new AnimatorSet();
        up_set.playTogether(up, upY);
        up_set.setDuration(120);
        AnimatorSet full = new AnimatorSet();
        full.playSequentially(down_set, up_set);
        full.start();
    }

    /** Gentle pulsing ring animation while the VPN handshake is in progress. */
    private void startConnectingPulse() {
        stopConnectingPulse();
        connectingPulse = ValueAnimator.ofFloat(1f, 1.12f, 1f);
        connectingPulse.setDuration(1100);
        connectingPulse.setRepeatCount(ValueAnimator.INFINITE);
        connectingPulse.setInterpolator(new AccelerateDecelerateInterpolator());
        connectingPulse.addUpdateListener(animation -> {
            float v = (float) animation.getAnimatedValue();
            ringOuter.setScaleX(v);
            ringOuter.setScaleY(v);
        });
        connectingPulse.start();
    }

    private void stopConnectingPulse() {
        if (connectingPulse != null) {
            connectingPulse.cancel();
            ringOuter.setScaleX(1f);
            ringOuter.setScaleY(1f);
        }
    }

    /** Swaps ring/icon tint between the "secured" and "idle" states with a soft crossfade. */
    private void setConnectedVisualState(boolean connected) {
        int ringRes = R.drawable.circle_icon_bg; // background shape stays the same, only alpha pulses
        ringInner.animate().alpha(connected ? 1f : 0.6f).setDuration(300).start();
        ivStatusIcon.animate().scaleX(connected ? 1.05f : 1f).scaleY(connected ? 1.05f : 1f).setDuration(300).start();
    }

    private void connectVpn() {
        if (bongoVpn.hasVpnPermission()) {
            bongoVpn.startVpn();
        } else {
            bongoVpn.requestVpnPermission();
        }
    }


    // Connect Method
    private void stopVpn() {
        bongoVpn.stopVpn();
    }

    /** Feature 3: side drawer item taps. */
    private boolean onDrawerItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_select_location) {
            selectLocationLauncher.launch(new Intent(this, SelectLocationActivity.class));
        } else if (id == R.id.nav_support) {
            Intent mailIntent = new Intent(Intent.ACTION_SENDTO);
            mailIntent.setData(android.net.Uri.parse("mailto:" + getString(R.string.support_email)));
            mailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " Support");
            if (mailIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mailIntent);
            }
        } else if (id == R.id.nav_privacy_policy) {
            startActivity(new Intent(this, PrivacyPolicyActivity.class));
        } else if (id == R.id.nav_disclaimer) {
            startActivity(new Intent(this, DisclaimerActivity.class));
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(this, AboutActivity.class));
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Feature 1/2: reloads the persisted selection from {@link ServerPreferences}, resolves it
     * via {@link ServerCatalog} (runtime asset scan - never hardcoded), updates the home-screen
     * flag/name, and re-attaches it to BongoVpn so the next Connect tap uses it.
     *
     * @return true if the resolved server actually changed from what was previously attached.
     */
    private boolean updateSelectedServerFromPrefs() {
        String selectedKey = ServerPreferences.getSelectedServerKey(this);
        VpnServer resolved = ServerCatalog.findByKey(this, selectedKey);
        if (resolved == null) {
            // Fall back to the first available free server if the persisted key ever goes stale
            // (e.g. its .ovpn file was removed from assets).
            java.util.List<VpnServer> free = ServerCatalog.scanFree(this);
            resolved = free.isEmpty() ? null : free.get(0);
        }
        boolean changed = resolved != null && (currentServer == null || !currentServer.key().equals(resolved.key()));
        if (resolved != null) {
            currentServer = resolved;
            tvSelectedFlag.setText(currentServer.flagEmoji);
            tvSelectedCountry.setText(currentServer.countryName);
            bongoVpn.attachFromAsset(currentServer.assetPath, VPN_USERNAME, VPN_PASSWORD);
        }
        return changed;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopConnectingPulse();
        if (bongoVpn != null) bongoVpn.release();
    }

    //-----------------------------------
}
