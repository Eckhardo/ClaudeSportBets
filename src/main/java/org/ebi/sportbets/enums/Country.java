package org.ebi.sportbets.enums;

import lombok.Getter;

@Getter
public enum Country {
    ENGLAND("EN", "England"),
    ITALY("IT", "Italy"),
    GERMANY("DE", "Germany"),
    SPAIN("ES", "Spain");

    private final String isoCode;
    private final String displayName;

    // Enum constructor (always private)
    Country(String isoCode, String displayName) {
        this.isoCode = isoCode;
        this.displayName = displayName;
    }

    // Helper method to find a country by its ISO code
    public static Country fromIsoCode(String code) {
        for (Country country : Country.values()) {
            if (country.isoCode.equalsIgnoreCase(code)) {
                return country;
            }
        }
        return null;
    }

}
