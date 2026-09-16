package com.rikivpn.nitaistudio;

/**
 * A single VPN server/location, discovered at runtime from an .ovpn file
 * placed under assets/free_vpn/ or assets/premium_vpn/.
 *
 * Nothing here is hardcoded per-country: {@link ServerCatalog} builds these
 * objects by scanning the asset folders, so dropping a new .ovpn file into
 * either folder makes it show up automatically on next app start.
 */
public class VpnServer {

    /** Folder-relative asset path, e.g. "free_vpn/india.ovpn" — passed straight to BongoVpn.attachFromAsset(). */
    public final String assetPath;

    /** Raw file name without extension, e.g. "india". Used as a stable id. */
    public final String fileId;

    /** Display name, e.g. "India". */
    public final String countryName;

    /** ISO 3166-1 alpha-2 code if it could be resolved, e.g. "IN". Empty if unknown. */
    public final String countryCode;

    /** Emoji flag rendered from countryCode, or a generic globe if unknown. */
    public final String flagEmoji;

    public final boolean premium;

    public VpnServer(String assetPath, String fileId, String countryName, String countryCode,
                      String flagEmoji, boolean premium) {
        this.assetPath = assetPath;
        this.fileId = fileId;
        this.countryName = countryName;
        this.countryCode = countryCode;
        this.flagEmoji = flagEmoji;
        this.premium = premium;
    }

    /** Stable key used for persistence (SharedPreferences) and list diffing. */
    public String key() {
        return (premium ? "premium:" : "free:") + fileId;
    }
}
