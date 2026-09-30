# Google Play checklist for RikiVpn

## Done in code
- targetSdk/compileSdk 36 (required for new apps/updates since Aug 31, 2026), AGP 8.9.1, Gradle 8.11.1
- Premium unlock = ONE opt-in Start.io rewarded video (reward only after full watch); no more 5 back-to-back interstitials
- Ads consent dialog before Start.io init (personalised vs non-personalised)
- Privacy policy rewritten (third-party servers + Start.io data collection); hosted copy in docs/privacy-policy.html
- AD_ID permission declared, allowBackup=false
- Removed absolute claims ("no one can track", "blazing fast", "buffer-free")
- Release signing via CI secrets; versionCode = GitHub run number

## You must do
1. **Keystore** (keep it safe, never commit):
   `keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
   Then `base64 -w0 upload.jks` -> GitHub repo secrets: KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD.
2. **Privacy policy URL**: repo Settings -> Pages -> deploy from `/docs`; use `https://<user>.github.io/<repo>/privacy-policy.html` in Play Console.
3. **Play Console -> App content**: Privacy policy, Ads (yes), Data safety (Advertising ID, IP, device info -> shared with Start.io), Content rating, Target audience (18+/13+, NOT kids), **VPN service declaration**, **Foreground service declaration** (systemExempted / VPN, needs a short screen-recording video), Advertising ID declaration (yes, advertising).
4. **Store listing** must state that the app uses VpnService, and must not claim "no logs", "100% anonymous", "military-grade", etc.
5. **Server rights**: confirm you have permission to use every .ovpn server in assets (they look like public SoftEther/VPN Gate style servers with vpn/vpn login). If not owned or licensed, Play can reject for deceptive/unauthorized use.
6. **16 KB page size**: check the CI step output. If BongoVPN native libs are not 16 KB aligned, update to a newer bongovpn version or ask Bongo Tech.
7. Personal developer accounts may need closed testing (12 testers, 14 days) before production.

8. Test the rewarded flow on a real device before release (Start.io may return no-fill on new apps; users then see a 'try again' message). Never ship with `StartAppSDK.setTestAdsEnabled(true)`.
