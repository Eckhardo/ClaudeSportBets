package org.ebi.sportbets.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.ebi.sportbets.enums.TippModusType;

/**
 * Stores {@link TippModusType} by its display name ("TotoTipp", "PointTipp", "ResultTipp"),
 * which is the format of the existing {@code type} column, instead of the enum constant name.
 */
@Converter
public class TippModusTypeConverter implements AttributeConverter<TippModusType, String> {

    @Override
    public String convertToDatabaseColumn(TippModusType type) {
        return type == null ? null : type.getDisplayName();
    }

    @Override
    public TippModusType convertToEntityAttribute(String displayName) {
        if (displayName == null) {
            return null;
        }
        TippModusType type = TippModusType.fromString(displayName);
        // Fail loudly: mapping an unknown value to null would erase it on the next update
        if (type == null) {
            throw new IllegalArgumentException("Unknown TippModusType in database: " + displayName);
        }
        return type;
    }
}
