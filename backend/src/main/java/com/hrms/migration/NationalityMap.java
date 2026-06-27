package com.hrms.migration;

import java.util.Map;

/**
 * Legacy 3-letter nationality codes -> ISO 3166-1 alpha-2.
 *
 * <p>The legacy FoxPro HR system used its OWN 3-letter country codes (e.g. BAN,
 * NEP, PHI) that are NOT ISO 3166 alpha-3. The new HRMS stores nationality as
 * ISO alpha-2 ({@code employee.nationality_country_code VARCHAR(2)}). This table
 * converts the legacy codes seen in {@code nation.dbf} to ISO alpha-2.
 *
 * <p>Codes not present here are imported as NULL nationality and reported as
 * warnings, so the list can be extended over time without breaking a run.
 * Mirrors {@code migration/nationality_map.py} (the CLI cutover tool).
 */
public final class NationalityMap {

    private static final Map<String, String> LEGACY_TO_ISO2 = Map.ofEntries(
            // South Asia
            Map.entry("BAN", "BD"), Map.entry("IND", "IN"), Map.entry("PAK", "PK"),
            Map.entry("NEP", "NP"), Map.entry("SRI", "LK"), Map.entry("AFG", "AF"),
            Map.entry("BHU", "BT"), Map.entry("MAL", "MV"),
            // South-East / East Asia
            Map.entry("PHI", "PH"), Map.entry("INO", "ID"), Map.entry("THA", "TH"),
            Map.entry("VIE", "VN"), Map.entry("CHN", "CN"), Map.entry("CHI", "CN"),
            Map.entry("MYA", "MM"), Map.entry("MAS", "MY"), Map.entry("KOR", "KR"),
            Map.entry("JPN", "JP"),
            // Middle East / GCC
            Map.entry("UAE", "AE"), Map.entry("KSA", "SA"), Map.entry("SAU", "SA"),
            Map.entry("QAT", "QA"), Map.entry("KUW", "KW"), Map.entry("BAH", "BH"),
            Map.entry("OMA", "OM"), Map.entry("OMN", "OM"), Map.entry("YEM", "YE"),
            Map.entry("JOR", "JO"), Map.entry("SYR", "SY"), Map.entry("LEB", "LB"),
            Map.entry("IRQ", "IQ"), Map.entry("IRN", "IR"), Map.entry("PAL", "PS"),
            // North Africa / Africa
            Map.entry("EGY", "EG"), Map.entry("SUD", "SD"), Map.entry("SDN", "SD"),
            Map.entry("MOR", "MA"), Map.entry("TUN", "TN"), Map.entry("ALG", "DZ"),
            Map.entry("LIB", "LY"), Map.entry("ETH", "ET"), Map.entry("KEN", "KE"),
            Map.entry("UGA", "UG"), Map.entry("NIG", "NG"), Map.entry("GHA", "GH"),
            Map.entry("SOM", "SO"), Map.entry("ERI", "ER"),
            // Europe / Americas
            Map.entry("UK", "GB"), Map.entry("GBR", "GB"), Map.entry("USA", "US"),
            Map.entry("CAN", "CA"), Map.entry("AUS", "AU"), Map.entry("FRA", "FR"),
            Map.entry("GER", "DE"), Map.entry("ITA", "IT"), Map.entry("ESP", "ES"),
            Map.entry("POR", "PT"), Map.entry("TUR", "TR"), Map.entry("RUS", "RU"),
            Map.entry("UKR", "UA"));

    private NationalityMap() {
    }

    /** ISO alpha-2 for a legacy 3-letter code, or {@code null} if unknown. */
    public static String toIso2(String legacyCode) {
        if (legacyCode == null) {
            return null;
        }
        return LEGACY_TO_ISO2.get(legacyCode.trim().toUpperCase());
    }
}
