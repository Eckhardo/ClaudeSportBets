package org.ebi.sportbets.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpieltagDto {

    private Long id;

    @NotNull
    @Positive
    private Integer spieltagNumber;

    private LocalDateTime startDate;

    @NotNull
    private Long competitionRoundId;

    private LocalDateTime createdOn;
}
