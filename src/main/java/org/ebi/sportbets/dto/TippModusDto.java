package org.ebi.sportbets.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Mirrors the tipp mode hierarchy; {@code modus} selects the subtype in JSON
 * ({@code type} is already a regular field of the entity).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "modus")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TippModusPointDto.class, name = "POINT"),
        @JsonSubTypes.Type(value = TippModusResultDto.class, name = "RESULT"),
        @JsonSubTypes.Type(value = TippModusTotoDto.class, name = "TOTO")
})
public abstract class TippModusDto {

    private Long id;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String type;

    @PositiveOrZero
    private Integer deadline;

    @NotNull
    private Long communityId;

    private LocalDateTime createdOn;
}
