package org.ebi.sportbets.entity;

import org.ebi.sportbets.enums.TippModusType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TippModusTypeConverterTest {

    private final TippModusTypeConverter converter = new TippModusTypeConverter();

    @Test
    void should_writeDisplayName_when_convertingToDatabaseColumn() {
        // Arrange
        TippModusType type = TippModusType.TIPPMODUS_TOTO;

        // Act
        String column = converter.convertToDatabaseColumn(type);

        // Assert
        assertThat(column).isEqualTo("TotoTipp");
    }

    @ParameterizedTest
    @EnumSource(TippModusType.class)
    void should_returnSameType_when_roundTrippingEveryType(TippModusType type) {
        // Arrange
        String column = converter.convertToDatabaseColumn(type);

        // Act
        TippModusType result = converter.convertToEntityAttribute(column);

        // Assert
        assertThat(result).isEqualTo(type);
    }

    @Test
    void should_writeNull_when_typeIsNull() {
        // Arrange
        TippModusType type = null;

        // Act
        String column = converter.convertToDatabaseColumn(type);

        // Assert
        assertThat(column).isNull();
    }

    @Test
    void should_readNull_when_columnIsNull() {
        // Arrange
        String column = null;

        // Act
        TippModusType result = converter.convertToEntityAttribute(column);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void should_throwIllegalArgument_when_columnHoldsUnknownValue() {
        // Arrange
        String column = "TIPPMODUS_TOTO";

        // Act
        Throwable thrown = catchThrowable(() -> converter.convertToEntityAttribute(column));

        // Assert
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown TippModusType in database: TIPPMODUS_TOTO");
    }
}
