package org.ebi.sportbets.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionTeamDto {

    private Long id;

    @NotNull
    private Long competitionId;

    @NotNull
    private Long teamId;

    private LocalDateTime createdOn;
}
