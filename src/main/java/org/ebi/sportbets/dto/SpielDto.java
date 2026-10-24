package org.ebi.sportbets.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpielDto {

    private Long id;

    @NotNull
    @Positive
    private Integer spielNumber;

    private LocalDateTime anpfiffDate;

    @NotNull
    @PositiveOrZero
    private Integer heimTore;

    @NotNull
    @PositiveOrZero
    private Integer gastTore;

    @NotNull
    private Boolean stattgefunden;

    private Long heimTeamId;

    private Long gastTeamId;

    private Long spieltagId;

    private Long competitionGroupId;

    private LocalDateTime createdOn;
}
