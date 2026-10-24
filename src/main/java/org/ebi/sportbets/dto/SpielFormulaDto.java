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
public class SpielFormulaDto {

    private Long id;

    @NotNull
    private Long spielId;

    @NotBlank
    @Size(max = 255)
    private String teamName;

    @Size(max = 255)
    private String teamNameAcronym;

    @NotNull
    private Boolean heimTeam;

    @NotNull
    @PositiveOrZero
    private Integer heimTore;

    @NotNull
    @PositiveOrZero
    private Integer gastTore;

    // Goal difference, may be negative
    @NotNull
    private Integer diffTore;

    @NotNull
    @PositiveOrZero
    private Integer won;

    @NotNull
    @PositiveOrZero
    private Integer remis;

    @NotNull
    @PositiveOrZero
    private Integer lost;

    @NotNull
    private Integer points;

    private LocalDateTime createdOn;
}
