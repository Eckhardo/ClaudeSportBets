package org.ebi.sportbets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionDto {

    private Long id;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String description;

    @NotNull
    @PositiveOrZero
    private Integer winMultiplicator;

    @NotNull
    @PositiveOrZero
    private Integer remisMultiplicator;

    @NotNull
    private Long competitionFamilyId;

    private LocalDateTime createdOn;
}
