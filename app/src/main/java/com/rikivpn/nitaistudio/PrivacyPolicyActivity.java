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

        appendSubheading(sb, "1. Information We Collect");
        appendBody(sb,
                "RikiVpn is designed to collect as little personal information as possible. " +
                "We do not require account creation, and we do not log your browsing history, " +
                "DNS queries, or the destination of your traffic while connected.\n\n");

        appendSubheading(sb, "2. Connection & Diagnostic Data");
        appendBody(sb,
                "To keep the App working reliably, basic technical data such as connection status, " +
                "session duration, and aggregate data usage (shown to you on the home screen) may be " +
                "processed on-device. This information is used only to power the App's own speed and " +
                "usage display, and is not sold to third parties.\n\n");

        appendSubheading(sb, "3. Third-Party VPN Engine");
        appendBody(sb,
                "RikiVpn uses a licensed third-party OpenVPN engine to establish the secure tunnel. " +
                "Once connected, your traffic is routed through the configured VPN server according to " +
                "that server's own operating policies. Please review the policies of the VPN server " +
                "provider you connect to for details on how they handle traffic.\n\n");

        appendSubheading(sb, "4. Permissions We Request");
        appendBody(sb,
                "\u2022 VPN permission \u2014 required to create the secure tunnel.\n" +
                "\u2022 Notification permission \u2014 required to show the ongoing connection status.\n" +
                "We do not request access to your contacts, photos, messages, or location.\n\n");

        appendSubheading(sb, "5. Data Retention & Security");
        appendBody(sb,
                "Any diagnostic data processed on-device is retained only for the current session and " +
                "is cleared when you disconnect or close the App. We apply reasonable technical " +
                "safeguards to protect the App and its connection process.\n\n");

        appendSubheading(sb, "6. Your Rights");
        appendBody(sb,
                "You may stop using RikiVpn and uninstall it at any time. If you have questions about " +
                "your data or this policy, you can contact us using the details below.\n\n");

        appendSubheading(sb, "7. Changes to This Policy");
        appendBody(sb,
                "We may update this Privacy Policy from time to time to reflect changes in the App or " +
                "applicable law. Continued use of RikiVpn after an update constitutes acceptance of the " +
                "revised policy.\n\n");

        appendSubheading(sb, "8. Contact Us");
        appendBody(sb,
                "For support, feedback, or privacy questions, reach out to us at:\n" +
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
