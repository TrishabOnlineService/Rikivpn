package com.rikivpn.nitaistudio;

import android.animation.ObjectAnimator;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Displays the Disclaimer and Privacy Policy for RikiVpn.
 * Content is built as a SpannableStringBuilder so section headers stand out
 * from body text, and the support email is auto-linked (android:autoLink="email").
 */
public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        TextView tvContent = findViewById(R.id.tvLegalContent);
        tvContent.setText(buildLegalText());
        tvContent.setAlpha(0f);
        tvContent.setTranslationY(24f);
        tvContent.animate().alpha(1f).translationY(0f).setDuration(300).start();

        findViewById(R.id.ivBack).setOnClickListener(v -> {
            View root = findViewById(android.R.id.content);
            ObjectAnimator.ofFloat(root, View.ALPHA, 1f, 0f).setDuration(150)
                    .start();
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private CharSequence buildLegalText() {
        SpannableStringBuilder sb = new SpannableStringBuilder();

        appendHeading(sb, "Disclaimer");
        appendBody(sb,
                "RikiVpn (\"the App\") is developed and published by Nitai Studio, an independent " +
                "Indian app development studio. RikiVpn is provided \"as is\" for general privacy " +
                "and network-connectivity purposes. It is not intended to facilitate, and must not " +
                "be used for, any unlawful activity. Nitai Studio does not guarantee uninterrupted, " +
                "error-free, or unrestricted access to any server, website, or online service while " +
                "the VPN is active, and is not responsible for content accessed by users through the App.\n\n");

        appendHeading(sb, "Privacy Policy");
        appendBody(sb, "Effective for all versions of RikiVpn.\n\n");

        appendSubheading(sb, "1. What we do NOT collect");
        appendBody(sb,
                "RikiVpn does not require an account. The app itself does not read, store or log the " +
                "websites you visit, your DNS queries, or the content of your traffic. We do not " +
                "request access to your contacts, photos, messages or precise location.\n\n");

        appendSubheading(sb, "2. Third-party VPN servers");
        appendBody(sb,
                "The VPN servers used by RikiVpn are operated by third parties, not by Nitai Studio. " +
                "Your traffic is encrypted between your device and the server, but the server " +
                "operator can technically see the traffic that leaves the server and may keep its own " +
                "logs. We cannot control or guarantee their practices. Do not use RikiVpn for " +
                "sensitive activity (banking, private accounts) if you are not comfortable with this.\n\n");

        appendSubheading(sb, "3. Ads and advertising data (Start.io)");
        appendBody(sb,
                "RikiVpn is free and shows ads through the Start.io SDK. The SDK may collect and " +
                "process your Advertising ID, IP address, device and OS information, approximate " +
                "location derived from IP, and ad interaction data in order to serve and measure ads. " +
                "If you accept personalised ads, this data may be used to personalise them; if you " +
                "choose non-personalised only, personalised targeting is turned off. Start.io acts " +
                "under its own privacy policy: https://www.start.io/policy/privacy-policy\n" +
                "You can also reset or delete your Advertising ID in your Android settings.\n\n");

        appendSubheading(sb, "4. On-device data");
        appendBody(sb,
                "Connection status, session duration and data usage shown on the home screen are " +
                "processed on your device only. Your selected server and ad-consent choice are " +
                "stored locally and are deleted when you uninstall the app.\n\n");

        appendSubheading(sb, "5. Permissions");
        appendBody(sb,
                "\u2022 VPN permission \u2014 required to create the VPN tunnel (Android VpnService).\n" +
                "\u2022 Notifications \u2014 to show the ongoing connection status.\n" +
                "\u2022 Network state / Internet \u2014 to connect and to load ads.\n" +
                "\u2022 Advertising ID \u2014 used by the ads SDK as described above.\n\n");

        appendSubheading(sb, "6. Children");
        appendBody(sb,
                "RikiVpn is not directed to children under 13 and we do not knowingly collect " +
                "personal information from children.\n\n");

        appendSubheading(sb, "7. Your choices");
        appendBody(sb,
                "You can uninstall the app at any time, reset your Advertising ID in Android settings, " +
                "or contact us to ask questions about your data.\n\n");

        appendSubheading(sb, "8. Changes to this policy");
        appendBody(sb,
                "We may update this policy when the app changes. The latest version is always " +
                "available inside the app.\n\n");

        appendSubheading(sb, "9. Contact");
        appendBody(sb,
                "Nitai Studio, India\n" +
                getString(R.string.support_email) + "\n\n");

        return sb;
    }

    private void appendHeading(SpannableStringBuilder sb, String text) {
        int start = sb.length();
        sb.append(text).append("\n\n");
        sb.setSpan(new StyleSpan(Typeface.BOLD), start, start + text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.riki_accent)),
                start, start + text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        setRelativeSize(sb, start, start + text.length(), 1.25f);
    }

    private void appendSubheading(SpannableStringBuilder sb, String text) {
        int start = sb.length();
        sb.append(text).append("\n");
        sb.setSpan(new StyleSpan(Typeface.BOLD), start, start + text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.riki_text_primary)),
                start, start + text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void appendBody(SpannableStringBuilder sb, String text) {
        sb.append(text);
    }

    private void setRelativeSize(SpannableStringBuilder sb, int start, int end, float size) {
        sb.setSpan(new android.text.style.RelativeSizeSpan(size), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
