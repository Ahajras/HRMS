"""
Legacy 3-letter nationality codes -> ISO 3166-1 alpha-2.

The legacy FoxPro HR system used its OWN 3-letter country codes (e.g. BAN, NEP,
PHI) that are NOT ISO 3166 alpha-3. The new HRMS stores nationality as ISO
alpha-2 (employee.nationality_country_code VARCHAR(2)). This table converts the
legacy codes seen in nation.dbf to ISO alpha-2.

Codes not present here are migrated as NULL nationality and reported as warnings,
so the list can be extended over time without breaking a run.
"""

LEGACY_NATION_TO_ISO2 = {
    # --- South Asia (bulk of GCC blue-collar workforce) ---
    "BAN": "BD",  # Bangladesh
    "IND": "IN",  # India
    "PAK": "PK",  # Pakistan
    "NEP": "NP",  # Nepal
    "SRI": "LK",  # Sri Lanka
    "AFG": "AF",  # Afghanistan
    "BHU": "BT",  # Bhutan
    "MAL": "MV",  # Maldives

    # --- South-East / East Asia ---
    "PHI": "PH",  # Philippines
    "INO": "ID",  # Indonesia
    "THA": "TH",  # Thailand
    "VIE": "VN",  # Vietnam
    "CHN": "CN",  # China
    "CHI": "CN",  # China (alt)
    "MYA": "MM",  # Myanmar
    "MAS": "MY",  # Malaysia
    "KOR": "KR",  # South Korea
    "JPN": "JP",  # Japan

    # --- Middle East / GCC ---
    "UAE": "AE",
    "KSA": "SA",  # Saudi Arabia
    "SAU": "SA",
    "QAT": "QA",
    "KUW": "KW",
    "BAH": "BH",
    "OMA": "OM",
    "OMN": "OM",
    "YEM": "YE",
    "JOR": "JO",
    "SYR": "SY",
    "LEB": "LB",
    "IRQ": "IQ",
    "IRN": "IR",
    "PAL": "PS",

    # --- North Africa / Africa ---
    "EGY": "EG",
    "SUD": "SD",  # Sudan
    "SDN": "SD",
    "MOR": "MA",  # Morocco
    "TUN": "TN",
    "ALG": "DZ",  # Algeria
    "LIB": "LY",  # Libya
    "ETH": "ET",  # Ethiopia
    "KEN": "KE",  # Kenya
    "UGA": "UG",  # Uganda
    "NIG": "NG",  # Nigeria
    "GHA": "GH",  # Ghana
    "SOM": "SO",  # Somalia
    "ERI": "ER",  # Eritrea

    # --- Europe / Americas (occasional staff) ---
    "UK":  "GB",
    "GBR": "GB",
    "USA": "US",
    "CAN": "CA",
    "AUS": "AU",
    "FRA": "FR",
    "GER": "DE",
    "ITA": "IT",
    "ESP": "ES",
    "POR": "PT",
    "TUR": "TR",
    "RUS": "RU",
    "UKR": "UA",
}


def to_iso2(legacy_code):
    """Return ISO alpha-2 for a legacy 3-letter code, or None if unknown."""
    if not legacy_code:
        return None
    return LEGACY_NATION_TO_ISO2.get(legacy_code.strip().upper())
