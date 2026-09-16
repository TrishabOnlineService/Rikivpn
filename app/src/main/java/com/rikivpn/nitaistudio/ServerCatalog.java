package com.rikivpn.nitaistudio;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Feature 2 - Auto Server Discovery.
 *
 * Scans assets/free_vpn/*.ovpn and assets/premium_vpn/*.ovpn at runtime and turns
 * each file into a {@link VpnServer}. Nothing about a specific server is hardcoded:
 * drop a new "brazil.ovpn" into premium_vpn/ and it appears in the Premium tab on
 * the next app start with no code changes.
 *
 * Country name + flag are derived from the filename using a name -> ISO-3166 alpha-2
 * lookup table (common country names/aliases). If a filename isn't recognised, we still
 * show the server with a title-cased name and a generic flag instead of hiding it -
 * so unknown future files never disappear silently.
 */
public final class ServerCatalog {

    public static final String FREE_FOLDER = "free_vpn";
    public static final String PREMIUM_FOLDER = "premium_vpn";

    private ServerCatalog() {}

    public static List<VpnServer> scanFree(Context context) {
        return scanFolder(context, FREE_FOLDER, false);
    }

    public static List<VpnServer> scanPremium(Context context) {
        return scanFolder(context, PREMIUM_FOLDER, true);
    }

    /** Looks up a single server by its persisted key (see {@link VpnServer#key()}). */
    public static VpnServer findByKey(Context context, String key) {
        if (key == null) return null;
        List<VpnServer> all = new ArrayList<>();
        all.addAll(scanFree(context));
        all.addAll(scanPremium(context));
        for (VpnServer s : all) {
            if (s.key().equals(key)) return s;
        }
        return null;
    }

    private static List<VpnServer> scanFolder(Context context, String folder, boolean premium) {
        List<VpnServer> result = new ArrayList<>();
        AssetManager assets = context.getAssets();
        String[] files;
        try {
            files = assets.list(folder);
        } catch (IOException e) {
            files = null;
        }
        if (files == null) return result;

        Arrays.sort(files);
        for (String file : files) {
            if (!file.toLowerCase(Locale.US).endsWith(".ovpn")) continue;
            String fileId = file.substring(0, file.length() - ".ovpn".length());
            CountryInfo info = resolveCountry(fileId);
            String assetPath = folder + "/" + file;
            result.add(new VpnServer(assetPath, fileId, info.name, info.isoCode, info.flag, premium));
        }
        return result;
    }

    // ---------------------------------------------------------------------
    // Country name + flag resolution
    // ---------------------------------------------------------------------

    private static final class CountryInfo {
        final String name;
        final String isoCode;
        final String flag;
        CountryInfo(String name, String isoCode, String flag) {
            this.name = name;
            this.isoCode = isoCode;
            this.flag = flag;
        }
    }

    private static CountryInfo resolveCountry(String rawFileId) {
        String key = normalize(rawFileId);
        String isoCode = NAME_TO_ISO.get(key);

        if (isoCode == null) {
            // Maybe the filename already IS a 2-letter ISO code, e.g. "in.ovpn", "us.ovpn"
            if (rawFileId.length() == 2 && rawFileId.matches("(?i)[a-z]{2}")) {
                isoCode = rawFileId.toUpperCase(Locale.US);
            }
        }

        if (isoCode != null) {
            String displayName = ISO_TO_NAME.getOrDefault(isoCode, titleCase(rawFileId));
            return new CountryInfo(displayName, isoCode, flagEmoji(isoCode));
        }

        // Unknown country: never hide the server, just show a friendly title-cased
        // name and a generic globe flag so future/unmapped .ovpn files still work.
        return new CountryInfo(titleCase(rawFileId), "", "\uD83C\uDF10" /* 🌐 */);
    }

    private static String flagEmoji(String isoCode) {
        if (isoCode == null || isoCode.length() != 2) return "\uD83C\uDF10";
        int base = 0x1F1E6; // Regional Indicator Symbol Letter A
        char c1 = Character.toUpperCase(isoCode.charAt(0));
        char c2 = Character.toUpperCase(isoCode.charAt(1));
        if (c1 < 'A' || c1 > 'Z' || c2 < 'A' || c2 > 'Z') return "\uD83C\uDF10";
        int cp1 = base + (c1 - 'A');
        int cp2 = base + (c2 - 'A');
        return new String(Character.toChars(cp1)) + new String(Character.toChars(cp2));
    }

    private static String normalize(String raw) {
        return raw.toLowerCase(Locale.US).replace("_", " ").replace("-", " ").trim();
    }

    private static String titleCase(String raw) {
        String spaced = raw.replace("_", " ").replace("-", " ").trim();
        String[] words = spaced.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase(Locale.US));
        }
        return sb.length() == 0 ? raw : sb.toString();
    }

    /** Common country names/aliases -> ISO 3166-1 alpha-2. Extend freely; nothing else needs to change. */
    private static final Map<String, String> NAME_TO_ISO = new HashMap<>();
    private static final Map<String, String> ISO_TO_NAME = new HashMap<>();

    private static void map(String iso, String displayName, String... aliases) {
        ISO_TO_NAME.put(iso, displayName);
        NAME_TO_ISO.put(displayName.toLowerCase(Locale.US), iso);
        for (String a : aliases) {
            NAME_TO_ISO.put(a.toLowerCase(Locale.US), iso);
        }
    }

    static {
        map("IN", "India", "india", "bharat");
        map("US", "United States", "usa", "us", "america", "unitedstates", "united states");
        map("GB", "United Kingdom", "uk", "britain", "england", "unitedkingdom");
        map("JP", "Japan", "japan");
        map("DE", "Germany", "germany", "deutschland");
        map("FR", "France", "france");
        map("CA", "Canada", "canada");
        map("AU", "Australia", "australia");
        map("SG", "Singapore", "singapore");
        map("NL", "Netherlands", "netherlands", "holland");
        map("CH", "Switzerland", "switzerland");
        map("SE", "Sweden", "sweden");
        map("IT", "Italy", "italy");
        map("ES", "Spain", "spain");
        map("BR", "Brazil", "brazil");
        map("RU", "Russia", "russia");
        map("CN", "China", "china");
        map("KR", "South Korea", "southkorea", "south korea", "korea");
        map("AE", "United Arab Emirates", "uae", "dubai", "unitedarabemirates");
        map("HK", "Hong Kong", "hongkong");
        map("ID", "Indonesia", "indonesia");
        map("MY", "Malaysia", "malaysia");
        map("TH", "Thailand", "thailand");
        map("VN", "Vietnam", "vietnam");
        map("PH", "Philippines", "philippines");
        map("TR", "Turkey", "turkey");
        map("PL", "Poland", "poland");
        map("NO", "Norway", "norway");
        map("FI", "Finland", "finland");
        map("DK", "Denmark", "denmark");
        map("IE", "Ireland", "ireland");
        map("PT", "Portugal", "portugal");
        map("MX", "Mexico", "mexico");
        map("AR", "Argentina", "argentina");
        map("ZA", "South Africa", "southafrica", "south africa");
        map("EG", "Egypt", "egypt");
        map("SA", "Saudi Arabia", "saudiarabia", "saudi arabia");
        map("IL", "Israel", "israel");
        map("NZ", "New Zealand", "newzealand", "new zealand");
        map("BE", "Belgium", "belgium");
        map("AT", "Austria", "austria");
        map("UA", "Ukraine", "ukraine");
        map("RO", "Romania", "romania");
        map("GR", "Greece", "greece");
        map("PK", "Pakistan", "pakistan");
        map("BD", "Bangladesh", "bangladesh");
        map("NG", "Nigeria", "nigeria");
        map("KE", "Kenya", "kenya");
    }
}
